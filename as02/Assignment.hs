module Assignment (cartesian, lpl2ll, genCartesian, allAssignments, Formula(..), vars, unique, check, solve, formula1, formula2) where
todo = error "not implemented"

-- ASSIGNMENT STARTS HERE
-- Feel free to define helper functions and data types as needed, but do not
-- change the name and type signatures of the existing functions.

-- Task 1
cartesian :: [a] -> [b] -> [(a, b)]
cartesian a b = [(x, y) | x <- a, y <- b]

-- Task 2
lpl2ll :: [(a, [a])] -> [[a]]
lpl2ll = map (\(x, xs) -> x : xs)

-- Task 3
genCartesian :: [[a]] -> [[a]]
genCartesian [] = [[]]
genCartesian (xs:xss) = lpl2ll (cartesian xs (genCartesian xss))

-- Task 4
allAssignments :: [String] -> [[(String,Bool)]]
allAssignments vars = genCartesian [[(v, False), (v, True)] | v <- vars]

-- Task 5
data Formula = Var String
             | Not Formula
             | And Formula Formula
             | Or Formula Formula
             | Implies Formula Formula
             deriving (Eq, Show)

formula :: Formula -- (p ∧ (q ∨ p)) → ¬s
formula = Implies (And (Var "p") (Or (Var "q") (Var "p"))) (Not (Var "s"))

-- Task 6
vars :: Formula -> [String]
vars (Var s)      = [s]
vars (Not f)      = vars f
vars (And f g)    = vars f ++ vars g
vars (Or f g)     = vars f ++ vars g
vars (Implies f g) = vars f ++ vars g

-- Task 7
contains :: Eq a => [a] -> a -> Bool
contains [] _ = False
contains (x:xs) y | x == y    = True
            | otherwise = contains xs y

unique :: Eq a => [a] -> [a]
unique = foldr (\x acc -> if contains acc x then acc else x : acc) []

-- Task 8
check :: Formula -> [(String,Bool)] -> Bool
check f assignment = case f of
    Var s -> case lookup s assignment of
                Just val -> val
                Nothing  -> False
    Not f' -> not (check f' assignment)
    And f1 f2 -> check f1 assignment && check f2 assignment
    Or f1 f2 -> check f1 assignment || check f2 assignment
    Implies f1 f2 -> not (check f1 assignment) || check f2 assignment

-- Task 9
solve :: Formula -> Maybe [(String,Bool)]
solve f = let uniqueVars = unique (vars f)
              assignments = allAssignments uniqueVars
          in case filter (check f) assignments of
               (a:_) -> Just a
               []    -> Nothing

-- Task 10
formula1 :: Formula -- a ∨ b ∨ c ∨ d ∨ e ∨ f
formula1 = Or (Var "a") (Or (Var "b") (Or (Var "c") (Or (Var "d") (Or (Var "e") (Var "f")))))

formula2 :: Formula -- ¬a ∧ a ∧ b ∧ c ∧ d ∧ e ∧ f
formula2 = And (Not (Var "a")) (And (Var "a") (And (Var "b") (And (Var "c") (And (Var "d") (And (Var "e") (Var "f"))))))