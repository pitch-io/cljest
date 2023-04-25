# Configuration

# `cljest` config

By default, `cljest` is either able to infer the information it needs or has sensible defaults. If you're just starting out and your tests are in a path in the classpath that you execute `cljest` with, you probably don't need to configure anything yet.

However, if you're doing something more advanced, like you need a custom setup file or if you need to define something for the compiler, below describes how you can set up a custom config file.

## What is the default config?

Assuming that your classpath has a relative `src` directory exposed, the following config will be the default:

```edn
{:test-src-dirs ["src"]
 :ns-suffixes [-test]
 :setup-ns cljest.setup
 :port 9003
 :compiler-options {:closure-defines {}}}
```

# Custom configuration

To create a custom configuration, first create a file called `cljest.edn` that lives at the root of your project. This is typically where your `deps.edn` file exists, and will need to live next to your `jest.config.js` file.

## Config keys in detail

In more detail, the above keys are:

- `:test-src-dirs`: a vector of strings that point to directories that contain test files, relative to `cljest.edn`. Uses relative paths in your classpath as the default.

  These directories should correspond to a directory in the classpath, so if your `deps.edn` file has `src/dev` as an entry and you have tests there, make sure that `test-src-dirs` uses `src/dev` rather than `src`. If you set the wrong path here, you will get cryptic error message with weird looking namespaces.

- `:ns-suffixes`: a vector of symbols that are the endings of a test namespace. Defaults to `[-test]`.

- `setup-ns`: a symbol which is a resolvable namespace that should be loaded before each test. Defaults to `cljest.setup`.

  **Note**: if you specify an alternative `setup-ns`, you **must** require `cljest.setup` for matchers to correctly work.

- `port`: an integer that corresponds to the port that will be used when starting `cljest` in watch mode. Defaults to `9003`.

- `compiler-options`: a map of [ClojureScript compiler options](https://clojurescript.org/reference/compiler-options). At the moment only `closure-defines` is allowed. [Please raise a ticket](https://github.com/pitch-io/cljest/issues/new) if there's a compiler option you would like to be allowed.

**Example**:

```edn
{:test-src-dirs ["src/app"]
 :ns-suffixes [-spec -test]
 :setup-ns my.cool.preloads.ns}
```

# Jest config

In general, you don't need much `cljest` specific config for Jest, namely a couple keys in your `jest.config.js` file and a couple lines in your `jest.setup.js` file.

```js
// jest.config.js
module.exports = {
    preset: 'jest-preset-cljest',
    setupFilesAfterEnv: ['jest.setup.js'],
}
```

```js
// jest.setup.js
const { loadSetupFile } = require("jest-preset-cljest/utils");

loadSetupFile();
```

If you're not using another preset, this is all you need to get started, and details on further Jest configuration please see [the Jest configuration documentation](https://jestjs.io/docs/configuration).

## I need to use `jest-preset-cljest` with another preset

In the event you need to use `jest-preset-cljest` with another preset, such as if you're using `jest-expo` for your React Native components, you can use require `jest-preset-cljest` as a normal object and extend from it. Just keep in mind the following:

- As mentioned, you can import the preset as an object which you can extend by requiring or importing `jest-preset-cljest`, e.g. `const jestShadowCljest = require('jest-preset-cljest')`
- Make sure you deep merge the preset in with your config and other potential presets using something like `lodash.merge`. Without deeply merging you will likely run into issues related to things like `roots` or Jest's Haste mechanism.
