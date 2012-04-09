(ns clj-dcpu16.core-test
  (:use clojure.test
        clj-dcpu16.core))

(defn- clear-memory []
  (dosync (alter memory (fn [old] {:pc 0x0000 :sp 0xFFFF}))))

(defn- force-memory [address value]
  (dosync (alter memory #(assoc % address value))))

(defn- place-instruction
  [address & inst]
  (if (empty? inst)
    nil
    (do (force-memory address (first inst))
        (place-instruction (inc address) (rest inst)))))

(deftest memory-access
  (testing "Get memory: "
    (testing "Get a 0 for non initialized memory"
      (clear-memory)
      (is (= 0 (get-memory 0xABCD)))
      (is (= 0 (get-memory :b))))
    (testing "Get value of initialized memory"
      (clear-memory)
      (force-memory 0x1234 0xEEEE)
      (is (= 0xEEEE (get-memory 0x1234))))
    (testing "Peek"
      (clear-memory)
      (force-memory :sp 0x1234)
      (force-memory 0x1234 0xFFFF)
      (is (= 0xFFFF (get-memory :peek))))
    (testing "Pop"
      (clear-memory)
      (force-memory :sp 0xFFFE)
      (force-memory 0xFFFF 0x2)
      (force-memory 0xFFFE 0x1)
      (is (= 0x1 (get-memory :peek)))
      (is (= 0x1 (get-memory :pop)))
      (is (= 0x2 (get-memory :peek)))
      (is (= 0xFFFF (get-memory :sp)))))
  (testing "Change memory: "
    (testing "Change uninitialized memory location"
      (clear-memory)
      (change-memory 0xABCD 0xFFFF)
      (is (= 0xFFFF (get-memory 0xABCD)))
      (change-memory :x 0x1234)
      (is (= 0x1234 (get-memory :x))))
    (testing "Change initialized memory"
      (clear-memory)
      (force-memory 0xEAEA 0x1234)
      (change-memory 0xEAEA 0x4321)
      (is (= 0x4321 (get-memory 0xEAEA))))
    (testing "Push"
      (clear-memory)
      (change-memory :push 0x1234)
      (is (= 0xFFFE (get-memory :sp)))
      (is (= 0x1234 (get-memory :peek)))
      (change-memory :push 0x4321)
      (is (= 0xFFFD (get-memory :sp)))
      (is (= 0x4321 (get-memory :peek)))))
  (testing "Inc memory"
    (clear-memory)
    (inc-memory 0xAB13)
    (is (= 1 (@memory 0xAB13)))
    (inc-memory :pc)
    (is (= 1 (@memory :pc)))
    (inc-memory :a)
    (inc-memory :a)
    (is (= 2 (@memory :a))))
  (testing "Dec memory"))

(deftest word-parsing
  (testing "Word Parsing"
    (testing "Word 0x7C01"
      (let [word 0x7C01]
        (is (= 1 (get-o word)))
        (is (= 0 (get-a word)))
        (is (= 0x1F (get-b word)))))
    (testing "Word 0x61C1"
      (let [word 0x61C1]
        (is (= 1 (get-o word)))
        (is (= 0x1C (get-a word)))
        (is (= 0x18 (get-b word)))))))

(deftest instruction-length
  (testing "Instruction Length"
    (testing "Word 0x7DE1"
      (is (= 3 (op-size 0x7DE1))))
    (testing "Word 0x7C10"
      (is (= 2 (op-size 0x7C10))))
    (testing "Word 0x9037"
      (is (= 1 (op-size 0x9037))))))