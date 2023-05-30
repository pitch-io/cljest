# cljest: ClojureScript + Jest [![Clojars Project](https://img.shields.io/clojars/v/com.pitch/cljest.svg)](https://clojars.org/com.pitch/cljest)

`cljest` is a way to run your ClojureScript files in Jest. It consists of two packages, one in Clojure and one in JavaScript, which allow you to take your ClojureScript tests and run them in Jest.

# Installation

You'll need two packages, `cljest` and `jest-preset-cljest`.

Install `jest-preset-cljest` using NPM (or Yarn):

```
npm add --save-dev jest-preset-cljest@1.0.0
```

And then add `cljest` to your `deps.edn` or analagous file:

```
{:deps {com.pitch/cljest {:mvn/version "1.0.0"}}}
```

For more details, as well as details about getting started and configuration, please see [the Installation documentation](./docs/installation.md).

# Documentation

[Installation](./docs/installation.md)<br />
[Writing your first test](./docs/getting-started.md)<br />
[Migrating from `cljs.test` to `cljest` and differences](./docs/migrating.md)<br />
[Unit tests](./docs/unit-tests.md)<br />
[Component tests](./docs/component-tests.md)<br />
[Matchers](./docs/matchers.md)<br />
[Mocking](./docs/mocking.md)<br />
[Async code](./docs/async.md)<br />
[General API docs](./docs/api.md)<br />
[Running in CI/noninteractive environments](./docs/ci.md)<br />
[Important information about library versioning](./docs/versioning.md)<br />
[Troubleshooting](./docs/troubleshooting.md)<br />

# Why Jest?

Succinctly:

- Great performance. Jest runs tests in parallel using workers, so your test runs scale with your machine, and as you add more tests, in the CI you can [shard your tests](https://jestjs.io/docs/cli#--shard) across multiple machines.
- Full async support. [Jest automatically handles promises within tests, including timing them out](https://jestjs.io/docs/asynchronous), and `cljest` has a special [`async` macro](https://github.com/pitch-io/cljest/blob/5d19b87021023daef75971ff005e05a288369c1d/cljest/src/cljest/helpers/core.clj#L63) to make dealing with async code easier. No more hanging promises in your tests!
- Built in support for things like component tests, snapshots, and timer mocks. Jest [can be easily configured to use `jsdom`](https://jestjs.io/docs/configuration#testenvironment-string), an environment that lets you mimic a real DOM environment without spinning up a browser, letting you easily test your Reagent, UIx, Helix, or other components, and [supports creating snapshots of your components](https://jestjs.io/docs/snapshot-testing) so that changes to the implementation detail of your components doesn't change what the user sees. It also supports [mocking timers like `setTimeout` and `process.nextTick`](https://jestjs.io/docs/timer-mocks) so that you can have complete control over how time advances in your code, removing the need to mock or abstract timers away yourself.
- Huge community. Jest has the largest community and backing of any JavaScript test runner, meaning it has great community created resources such as [`testing-library`](https://testing-library.com) for good testing practices, [`jest-axe`](https://github.com/nickcolley/jest-axe) for accessibility, and more. Jest is also part of [the OpenJS Foundation](https://openjsf.org/blog/2022/05/11/openjs-foundation-welcomes-jest/).

# Roadmap

The following items, in no particular order, are on the roadmap as potential additions or improvements to `cljest`:

- Incremental test file compilation, which can give even better performance in a large codebase.
- REPL support with JSDOM, so that you can test things on components with the REPL.

# Contributing

Firstly, thanks!

Secondly, please fork the repository and create a PR with your changes. Just keep a couple of things in mind:

- If your change is something that would impact users in a significant way(and therefore potentially requiring a major version bump), please raise an issue so that we can discuss it first.
- Please add tests for your change. In general, if the change is end user facing, write your test in Jest/`cljest`, and otherwise (or if your code is Clojure, not ClojureScript), write your tests in Kaocha.

# Issues and bugs

If something isn't working right, and it's not covered by the [troubleshooting documentation](./docs/troubleshooting.md), or if something is missing, or if you just generally encounter issues, please [create an issue](https://github.com/pitch-io/cljest/issues/new).

# License

MIT
