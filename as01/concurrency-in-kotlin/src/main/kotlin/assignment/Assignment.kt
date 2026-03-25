package assignment

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import labutils.*
import labutils.math.Point
import labutils.robot.RobotBehavior
import labutils.simulator.Simulator.Companion.actor
import exercises.e3.AsyncAwait.async
import exercises.e3.AsyncAwait.setInterval
import exercises.e3.AsyncAwait.setTimeout
import java.awt.Color

fun main() {
    Scenarios.Assignment.runScenario(
        redBehavior = Assignment.redBehavior,
        greenBehavior = Assignment.greenBehavior,
        blueBehavior = Assignment.blueBehavior,
        width = 600.0,
        height = 600.0,
    )
}

@OptIn(ObsoleteCoroutinesApi::class)
object Assignment {
    // SCENARIO
    // Three colored robots wants to reach the light of their own color:
    // - The red robot wants to reach the red light.
    // - The blue robot wants to reach the blue light.
    // - The green robot wants to reach the green light.
    // Their light sensors can only detect the closest light, which may
    // have a different color than their own. In fact, each robot starts
    // closest to a light with a different color:
    // - The red robot starts near the blue light
    // - The green robot starts near the red light
    // - the blue robot start near the green light
    // The robots must exchange messages to share local information about
    // the environment and achieved their own goals.
    // The robots should also exchange messages about their goals. In
    // particular, only after all three robots have reached their goal,
    // they all start blinking.

    // CHANNELS
    private val robotIds: List<Color> = listOf(Color.RED, Color.GREEN, Color.BLUE)
    private val channels: MutableMap<Color, SendChannel<Message>> = mutableMapOf()
    private val allRegistered = CompletableDeferred<Unit>()

    // MESSAGES
    private sealed class Message
    private data class LightInfo(val lightColor: Color, val position: Point) : Message()
    private data class ReachedGoal(val robotColor: Color) : Message()

    private fun createRobotBehavior(robotColor: Color): RobotBehavior = { robot ->
        var targetPosition: Point? = null
        val completedRobots = mutableSetOf<Color>()
        var goalReached = false

        // Sense the closest light and share its position with the other robots
        val sensed = async {
            allRegistered.await()
            val light = robot.lightSensor.closestLight()
            if (light != null) {
                val absolutePos = robot.body.absolutePosition(light.direction)
                val lightColor = light.color

                // Filter sending robot from ids and send light info to all other robots
                robotIds
                    .filter {
                        id -> id != robotColor
                    }
                    .forEach { id ->
                        channels[id]?.send(LightInfo(lightColor, absolutePos))
                    }
            }
        }

        // Create actor for this robot and signal when all are registered
        channels[robotColor] = actor {
            for (msg in channel) {
                when (msg) {
                    is LightInfo -> {
                        // If this light matches the robot's color, that's the target
                        if (msg.lightColor == robotColor) {
                            targetPosition = msg.position
                        }
                    }
                    is ReachedGoal -> {
                        completedRobots.add(msg.robotColor)
                        // If all robots have reached their goals, start blinking
                        if (completedRobots.size == 3) {
                            setInterval(200) {
                                robot.led.switch(on = true, color = robotColor)
                                setTimeout(100) {
                                    robot.led.switch(on = false, color = robotColor)
                                }.await()
                            }
                        }
                    }
                }
            }
        }

        // Once all robots are registered, we can start
        if (channels.size == robotIds.size) {
            allRegistered.complete(Unit)
        }

        // Navigate towards the target light once we know where it is
        setInterval(10) {
            val target = targetPosition
            if (target != null) {
                val direction = robot.body.relativeDirection(target)
                if (direction.length < 0.1) {
                    if (!goalReached) {
                        goalReached = true
                        // Notify all robots (including self) that we reached our goal
                        for (id in robotIds) {
                            channels[id]?.send(ReachedGoal(robotColor))
                        }
                    }
                } else {
                    val rotating = async { robot.spinMotor.rotate(0.2 * direction.angle) }
                    val moving = async { robot.motor.forward(0.1 * direction.length) }
                }
            }
        }
    }

    // BEHAVIORS
    val redBehavior: RobotBehavior = createRobotBehavior(Color.RED)
    val greenBehavior: RobotBehavior = createRobotBehavior(Color.GREEN)
    val blueBehavior: RobotBehavior = createRobotBehavior(Color.BLUE)
}