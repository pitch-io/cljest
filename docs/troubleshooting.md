# Troubleshooting

## When I start Jest, it just hangs, stuck on `Determining test suites to run...`

Jest, setup with `jest-preset-cljest` and running without the `CI` environment variable, expects to find a running compilation watch process, which starts a server running on the port optionally configured in your `cljest.edn` file. Ensure that you've started the watch process, e.g. `clj -X cljest.compilation/watch`.

## I get errors related to a namespace not existing

Make sure that your `test-src-dirs` corresponds to a path in your classpath, as internally `cljest` makes some assumptions based on these paths. If you're certain that your `test-src-dirs` is correct and you're still having issues, please raise an issue and include your config and current classpath.
