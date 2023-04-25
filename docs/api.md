# General API docs

This document describes at a high level the public API of `cljest` from the perspective of a test writer. This document is not meant to be an exhaustive list of exposed functions and macros with examples. For that, please refer to the docstring of the specific function or macro.

`cljest` follows the general API of vanilla Jest, tweaked slightly for ClojureScript, either for dealing with `js/undefined` or for making the functions or macros more Clojure-like.

# Fundamental functions and macros

[`cljest.core`](../cljest/src/cljest/core.clj) exposes the following functions and macros.

**`cljest.core/describe`**: a macro that groups tests together. It is analogous to `deftest` except that it isn't a function declarator and takes a string as the argument. Fixture functions, such as `before-each`, are scoped to the current describe block.

**`cljest.core/it`**: a macro for an individual test. It is analogous to `testing`.

**`cljest.core/before-all`**: a macro that runs the given forms before all tests within the current scope. Note the Jest documentation is a little misleading; `before-all` does respect `describe` scoping.

**`cljest.core/after-all`**: the same as `before-all` except after all tests within the current scope.

**`cljest.core/before-each`**: a macro that runs the given forms before each test within the current scope.

**`cljest.core/after-each`**: the same as `before-each ` except after each test within the current scope.

**`cljest.core/is`**: a generic assertion macro. Analogous to `cljs.test/is` but stops execution on the first false result, and does not support `thrown?` or `thrown-with-msg?` forms.

**`cljest.core/spy`**: a wrapper for `jest.fn()`. A function that captures calls, which can be asserted against using matchers such as `called?` and `called-with?`.

## Less common macros

**`cljest.core/each`**: essentially a mix between `it` and `doseq` (and has the same API as `doseq`). Allows for writing the same test case with multiple inputs.

**`cljest.core/only`**: like `it` but forces the other tests in this file to be skipped (except those that are also `only`). Useful for debugging.

**`cljest.core/only-each`**: `each` but `only`.

**`cljest.core/skip`**: like `it` but skips the test.

**`cljest.core/todo`**: lets you write test case descriptions in your test file before writing the actual test, kind of like a todo list. Does _not_ accept a body, and TODOs will be highlighted by Jest when you run the test file.

# Core helpers

[`cljest.helpers.core`](../cljest/src/cljest/helpers/core.clj) exposes the following functions and macros.

**`cljest.helpers.core/with-mocks`**: analogous to `with-refers` but handles async bodies.

**`cljest.helpers.core/setup-mocks`**: a way to create mocks at the top or `describe` scoped level such that they will be redefined during each test. There is no analogous construct in vanilla `cljs.test` but you can conceptually think about it as `with-refers` but for all tests in the current block that also respects async tests.

**`cljest.helpers.core/async`**: a macro for simplifying writing asynchronous code. See the docstring and [the async documentation](./async.md) for more details, but basically allows you to use `await` like in JS in a few specific cases.
