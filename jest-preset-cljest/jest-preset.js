const path = require("path");

const {
  getBuildDir,
  getPathsFromCljestConfig,
  generateTestRegexes,
} = require("./utils");

module.exports = {
  testRegex: generateTestRegexes(),
  moduleFileExtensions: ["clj", "cljs", "js"],
  globalSetup: path.resolve(__dirname, "jest.globalSetup.js"),
  resolver: path.resolve(__dirname, "jest.resolver.js"),
  transform: {
    "\\.(clj|cljs)$": path.resolve(__dirname, "jest.cljs-transformer.js"),
  },
  watchPlugins: [path.resolve(__dirname, "jest.ns-watch-plugin.js")],
  dependencyExtractor: path.resolve(__dirname, "jest.dependencyExtractor.js"),
  haste: {
    hasteImplModulePath: path.resolve(__dirname, "jest.hasteImpl.js"),
  },
  modulePaths: [getBuildDir()],
  roots: getPathsFromCljestConfig(),
};
