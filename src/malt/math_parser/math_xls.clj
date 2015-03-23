(ns malt.math_parser.math_xls
  (:use    [clojure.tools.trace]
           [malt.utils :only (mfn)])
  (:import (org.apache.commons.math3.distribution
            NormalDistribution
            PascalDistribution
            PoissonDistribution
            BinomialDistribution)))

(def SUM-ARGS-MAX 29)

(defprotocol FormulaCalc
  (udf-calc [this] "calc function"))

(defrecord MathNormalDistributionCDF
    [x mean standard_dev]
  FormulaCalc
  (udf-calc [this] (-> (NormalDistribution. mean standard_dev) (.cumulativeProbability x))))

(defrecord MathNormalDistributionPDF
    [x mean standard_dev]
  FormulaCalc
  (udf-calc [this] (-> (NormalDistribution. mean standard_dev) (.density x))))

(defrecord MathPascalDistributionCDF
    [number_f number_s probability_s]
  FormulaCalc
  (udf-calc [this] (-> (PascalDistribution. number_s probability_s) (.cumulativeProbability number_f))))

(defrecord MathPascalDistributionPDF
    [number_f number_s probability_s]
  FormulaCalc
  (udf-calc [this] (-> (PascalDistribution. number_s probability_s) (.probability number_f))))

(defrecord PoissonDistributionPDF
    [x mean]
  FormulaCalc
  (udf-calc [this] (-> (PoissonDistribution. mean) (.probability x))))

(defrecord PoissonDistributionCDF
    [x mean]
  FormulaCalc
  (udf-calc [this] (-> (PoissonDistribution. mean) (.cumulativeProbability x))))

(defrecord BinomialDistributionPDF
    [number_s trials probability_s]
  FormulaCalc
  (udf-calc [this] (-> (BinomialDistribution. trials probability_s) (.probability number_s))))

(defrecord BinomialDistributionCDF
    [number_s trials probability_s]
  FormulaCalc
  (udf-calc [this] (-> (BinomialDistribution. trials probability_s) (.cumulativeProbability number_s))))

(defmulti normal-distribution
  "function for calc normal distribution with PDF or CDF versions"
  (fn [x mean standard_dev cumulative]
    (true? cumulative)))
(defmethod normal-distribution true  [x mean standard_dev cumulative]
  (-> (->MathNormalDistributionCDF x mean standard_dev) .udf-calc))
(defmethod normal-distribution false [x mean standard_dev cumulative]
  (-> (->MathNormalDistributionPDF x mean standard_dev) .udf-calc))

(defmulti pascal-distribution
  "function for calc normal distribution with PDF or CDF versions"
  (fn [number_f number_s probability_s cumulative] 
    (true? cumulative)))
(defmethod pascal-distribution true  [number_f number_s probability_s cumulative]
  (-> (->MathPascalDistributionCDF number_f number_s probability_s) .udf-calc))
(defmethod pascal-distribution false [number_f number_s probability_s cumulative]
  (-> (->MathPascalDistributionPDF number_f number_s probability_s) .udf-calc))

(defmulti poisson-distribution
  "function for calc Binomial distribution with PDF or CDF versions"
  (fn [_ mean  cumulative]
    (true? cumulative)))
(defmethod poisson-distribution true  [x mean cumulative]
  (-> (->PoissonDistributionCDF x mean) .udf-calc))
(defmethod poisson-distribution false [x mean cumulative]
  (-> (->PoissonDistributionPDF x mean) .udf-calc))

(defmulti binomial-distribution
  "function for calc Binominal distribution with PDF or CDF versions"
  (fn [number_s trials probability_s cumulative]
    (true? cumulative)))
(defmethod binomial-distribution true  [number_s trials probability_s cumulative]
  (-> (->BinomialDistributionCDF number_s trials probability_s) .udf-calc))
(defmethod binomial-distribution false [number_s trials probability_s cumulative]
  (-> (->BinomialDistributionPDF number_s trials probability_s) .udf-calc))


;; CRITBINOM/BINOM.INV funcs
(defn ^Boolean probability?
  [^Float probability_s]
  (and (> probability_s 0.0) (< probability_s 1.0 )))

(defn find-inv [^Integer trials, ^Float probability_s, ^Float alpha]
  (let [[_ n]  (->> (for [x (range trials)
                         :let [p (binomial-distribution x trials probability_s true)]
                         :when (>= p alpha)]
                     [p x])
                   sort
                   first
                   )]
    {:trials trials :probability_s probability_s :alpha alpha :value n}))


(defn ^Integer binom-inv
  [^Integer trials, ^Float probability_s, ^Float alpha]
  (if (and (> trials 0) (probability? probability_s) (probability? alpha))
    (:value (find-inv trials probability_s alpha))
    'error))

(def binom-inv (mfn binom-inv))
(def poisson-distribution (mfn poisson-distribution))

(defn to-bool [arg]
  (condp = (first arg)
    \1 "TRUE)"
    \0 "FALSE)"
    arg))

(defmacro function-name-test [name formula body]
  (let [len (count name)]
    `(if (< ~len (count ~formula))
       (if (= ~name (subs ~formula 0 ~len))
         ~body
         ~formula)
       ~formula)))

(defn poissson-replace-bool-int [formula]
  (function-name-test "POISSON" formula
                      (let [[arg1, arg2, arg3]  (clojure.string/split formula #",")
                            bool-arg3 (to-bool arg3)]
                        (clojure.string/join "," [arg1, arg2, bool-arg3]))))

(defn norm-dist-replace
  [formula]
  (function-name-test "_xlfn.NORM.DIST" formula (let [fn-name-len (count "_xlfn.NORM.DIST")
                                                      fn-name (subs formula 0 fn-name-len)
                                                      fn-body (subs formula fn-name-len)]
                                                      (str "NORMDIST" fn-body))))

(defn sum-replace-long [formula]
   (function-name-test "SUM" formula
                            (let [args (clojure.string/split formula #",")]
                              (trace (count args))
                              (if (< SUM-ARGS-MAX (count args))
                                (clojure.string/join "+" args)
                                formula
                                ))))

