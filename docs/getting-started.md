# Getting started: writing your first test

Now that you've installed and configured Jest, you're ready to write your first test.

This document describes how to get started writing your first test and is meant to be a general guide on creating a function and getting `cljest` to see and run your tests. Make sure you've set up your environment first as this document assumes you have created config and setup files. If you haven't, please refer to the [installation docs](./installation.md).

# The `sum-seq` function

This example is a little contrived, but bear with me!

Let's start with a function that takes a sequence and adds all the numbers in it together. In your `src` directory (or similar), add a directory called `app` and create a file called `utils.cljs`.

```clj
;; app/utils.cljs
(ns app.utils)

(defn sum-seq
  [xs]
  (apply + xs))
```

To start writing some tests, make a file next to this one called `utils_test.cljs`, and add the `cljest.core` imports. Note that it's considered good practice to co-locate your source and test code, rather than separating them into directories like `src` and `test`.

```clj
;; app/utils_test.cljs
(ns app.utils-test
  (:require [app.utils :as utils]
            [cljest.core :refer [describe is it]]))

(describe "sum-seq"
  (it "sums numbers together"
    (is (= 7 (utils/sum-seq [2 3 2]))))

  (it "handles empty lists"
    (is (= 0 (utils/sum-seq [])))))
```

Finally, to run your tests, in your root project directory run `clj -X cljest.compilation/watch` — the one where ` cljest.edn` is. This will start the server that is called by Jest and which will compile your tests on-demand. You'll see some info logs and then you should see some compilation logs like this:

```
[:cljest.compilation.shadow/jest] Compiling ...
[:cljest.compilation.shadow/jest] Build completed. (78 files, 2 compiled, 0 warnings, 1,23s)
```

Now you can run Jest by running `npx jest --watch`. It will take a moment to start, and then you should see the successfully run tests:

```
 PASS  src/app/utils_test.cljs
  sum-seq
    ✓ sums numbers together (1 ms)
    ✓ handles empty lists

Test Suites: 1 passed, 1 total
Tests:       2 passed, 2 total
Snapshots:   0 total
Time:        0.378 s
Ran all test suites related to changed files.
```

Congratulations! You've written your first tests with `cljest` and Jest! Now go forth and conquer the world!
