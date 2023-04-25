(ns cljest.internal-test-setup-ns
  "The `setup-ns` used by `cljest`. External users should require `cljest.setup`."
  (:require ["@testing-library/jest-dom"]
            cljest.setup))
