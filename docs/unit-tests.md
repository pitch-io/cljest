# Unit tests

Short and sweet version: if you've written tests using `cljs.test` before, you don't need to change too much.

Longer version: As with `cljs.test`, Jest has full support for standard unit testing through the use of `cljest.core/is`, meaning that the assertions you write in `cljs.test` should be able to be translated to `cljest` with little change. As mentioned in the [Getting Started docs](./getting-started.md), you'll need to use `describe` and `it` instead of `deftest` and `testing`.

There are however a few differences:

- `cljest.core/is` (simply `is`) is a macro that compiles to a [Jest `expect` assertion](https://jestjs.io/docs/expect) and functions a little differently than `cljs.test/is`. The main difference is that, upon receiving a failing assertion, execution of the test will stop, rather than continuing to the next assertion. In other words, `is` throws, rather than returning a map of information about the failure.
- `is` is a bit simpler than `cljs.test/is` and does not support `thrown?` or `thrown-with-msg?` forms.
