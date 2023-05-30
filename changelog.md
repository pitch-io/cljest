# Changelog

# Next

## Features
- [Allow users to define their own matchers.](https://github.com/pitch-io/cljest/pull/27)

## Improvements
- [Allow aliases to be defined in `cljest.edn` so that Jest can use it when getting the classpath.](https://github.com/pitch-io/cljest/pull/27)

## Bugfixes
- [Always reinstantiate mocks for each test case when using `setup-mocks`.](https://github.com/pitch-io/cljest/pull/28)
- [Fix matcher negation.](https://github.com/pitch-io/cljest/pull/27)


# 1.0.0

## Features
- [Add documentation and improvements for full public release.](https://github.com/pitch-io/cljest/pull/21) (the below are all part of this PR)
- Separate Reagent and vanilla React rendering so users that don't use Reagent don't need to add it as a dependency.
- Support primitive values in `is`.
- Support custom `formatters-ns`.
- Infer default `test-src-dirs` based on classpath.
- Support wrapping matchers in `is` and enforce that matchers must be inside of `is`. Support negated matchers based on `(is (not ...))` usage.
- Add fake timers support, and expose functions via `cljest.timers` namespace. Automatically handle fake timer support for `user-event` helpers.
- Expose consistent promise API for `user-event` helpers.

## Improvements
- [Simplify matcher support by removing macros.](https://github.com/pitch-io/cljest/pull/21)
- [Add `cache-dir` option to avoid running shadow server conflicts.](https://github.com/pitch-io/cljest/pull/23)
- [Silence a few extraneous logs.](https://github.com/pitch-io/cljest/pull/24)

## Bugfixes
- [Fix issue related to namespace generation.](https://github.com/pitch-io/cljest/pull/20)
- [Fix bug related to `nil` values in `is`.](https://github.com/pitch-io/cljest/pull/21)

## Internal
- [Consistently use `^:private` internally.](https://github.com/pitch-io/cljest/pull/21)
- Separate dependencies that are core to the public package and secondary/internal, such as formatting and release packages.
- Use minimal `cljest.edn` file internally.
- Add example test based on component test docs.

# 1.0.0-alpha2

## Features

- [Support compiler options in the `cljest.edn`, with initial support for the `closure-defines` option.](https://github.com/pitch-io/cljest/pull/14)
- (Update `jest-preset-cljest` to get values from `cljest.edn` and add more things to the preset, reducing the amount of custom config needed, simplifying setup.)](https://github.com/pitch-io/cljest/pull/15)
- [Add support for `setup-ns` config key.](https://github.com/pitch-io/cljest/pull/16)

## Vulnerabilities
- [Upgrade `node-fetch` to latest due to CVE.](https://github.com/pitch-io/cljest/pull/18)

## Internal
- Change version number from `0.9.0` to `1.0.0`. `0.9.0` was based on an incorrect understanding of how the versions should work for alpha releases.
- [Add Makefiles and format all files with tools like `prettier` and `cljfmt`.](https://github.com/pitch-io/cljest/pull/17)

# 0.9.0-alpha1

- Initial released alpha version based on prior work. Publicly available but not announced.
