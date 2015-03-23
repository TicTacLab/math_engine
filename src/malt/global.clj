(ns malt.global
  (:use
   [clojure.tools.trace :only (trace)]
   [clojure.tools.cli :only [cli]]
   [malt.utils :only [read-config]]))

(def def-config {:malt-mode :dev
                 :malt-port-number 3000
                 :malt-math-config "config/math.config"
                 :malt-config "config/malt.config"})
(def config (ref def-config))
(def math-config (ref nil))
(def malt-config (ref nil))

(defn set-config
  ([] (set-config []))
  ([args]
  (let [[cfg _ banner] (cli args
                            ["-h" "--help" "Show help" :defalt false :flag true]
                            ["-m" "--malt-mode" "mode dev/prod" :parse-fn keyword :default :dev]
                            ["-p" "--malt-port-number" "http port number " :parse-fn #(Integer. %) :default 3000]
                            ["-c" "--malt-math-config" "path to math.config" :default "/etc/malt/math.config"]
                            ["-e" "--malt-math-models" "path to xls/xlsx files" :default "/etc/malt/math_models/"]
                            ["-s" "--malt-config" "path to malt config" :default "/etc/malt/malt.config"]
                            )]
    (when (:help cfg)
      (println banner)
      (System/exit 0))
    (println "===================================================")
    (println "HTTP PORT: " (:malt-port-number cfg))
    (println "MODE: " (:malt-mode cfg))
    (println "PATH to math.config: " (:malt-math-config cfg))
    (println "PATH to xls/xlsx files with math models: " (:malt-math-models cfg))
    (println "PATH to malt.config: " (:malt-config cfg))
    (println "===================================================")

    ;; try to set malt config
    (dosync  (ref-set malt-config
                      (let [malt-mode (cfg :malt-mode)]
                        (->> cfg
                             :malt-config
                             read-config
                             malt-mode
                             ))))

    (println "Malt params: " @malt-config)
    
    (when (->> @malt-config nil?)
      (println "Exit. No malt.config. run --help for describe")
      (System/exit 0))
    
    (println "===================================================")
    (dosync (ref-set config cfg)
            (ref-set math-config (read-config (cfg :malt-math-config))))
    )))


