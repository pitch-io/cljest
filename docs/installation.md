# Installation

First, you'll need to install both the JavaScript and Clojure dependencies, available on NPM and Clojars respectively.

This document assumes you already have a ClojureScript project created and working, with `deps.edn` or similar, probably a `package.json` file, etc., and some basic JavaScript and Clojure project setup knowledge.

## JavaScript

Install `jest-preset-cljest` as a dependency:

```
npm i --save-dev jest-preset-cljest
```

## Clojure

Add `com.pitch/cljest` to your `deps.edn`:

```edn
{:deps {com.pitch/cljest {:mvn/version "<VERSION>"}}}
```

# Configuration

## `cljest`

`cljest` works on the same principle as Jest and believes that things should work great by default. With this in mind, `cljest` will by default either infer from your environment or have something sensible for its configuration values.

More concretely, `cljest` by default will look for ClojureScript files that have a namespace that ends with `-test` (files that end in `_test.cljs`) in the directories that appear in your classpath (for example, `src`, `test`). If this is how your project is laid out too, you probably don't need to configure anything yourself!

However, if, for example, you have paths in your classpath that cause unexpected tests to show up in Jest, you use a different namespace for tests, or you generally have a need for a more complex setup, please see our [configuration docs](./configuration.md) for details on how to setup a custom `cljest.edn` config file.

## Jest

### Config file

Unlike the Clojure side of things, you do need to configure Jest, as we're doing something that's a bit different than the default.

To that end Jest with `cljest` requires a `jest.config.js` config file. In this configuration file you'll need to at minimum add `jest-preset-cljest` as your preset and make sure that a `setupFilesAfterEnv` entry is present.

Create a `jest.config.js` file and add the following values:

```js
module.exports = {
    // The preset you installed earlier, which allows Jest to work with `cljest` and ClojureScript files
    preset: 'jest-preset-cljest',

    // A setup file for loading CLJS specific things like `is`
    setupFilesAfterEnv: ['jest.setup.js'],
}

```

### Setup file

Jest also needs a setup file. This setup file runs before each of your tests.

Create a `jest.setup.js` file, and add the following code so that you load your configured `setup-ns` namespace:

```js
const { loadSetupFile } = require("jest-preset-cljest/utils");

loadSetupFile();
```

You don't need to do anything else here. If you need some custom setup, you can configure it in your `cljest.edn` `setup-ns` namespace instead (see the [configuration docs](./configuration.md) for more info).

# Is everything working? Cross your fingers 🤞

Once you've configured everything, you're almost good to go!

First, you'll need to add a test file. Jest is smart enough to not try to do anything until it finds a test, so make a file with a namespace suffix that matches your config. It's okay that it's empty, since it's only meant to make Jest attempt to run.

Next, start the `cljest` watch process by calling `clj -X cljest.compilation/watch` from your project root directory. When started it will start compiling the tests it finds.

While that's starting, you can start Jest by running `./node_modules/.bin/jest`. Jest will automatically wait until the server starts and initial compilation is complete before continuing.

If you get an error related to no tests existing in your file, and you got a successful compilation in the `cljest` watch process, that means your setup is working! Now you're ready to [write your first test in Jest](./getting-started.md) 😎

# I'm having some issues!

Please see our [troubleshooting docs](./troubleshooting.md) for some possible help, and if you still have issues, please open an issue.
