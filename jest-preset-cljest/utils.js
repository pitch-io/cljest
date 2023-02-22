const fs = require("fs");
const path = require("path");
const { parseEDNString } = require("edn-data");

const jestProjectDir = process.cwd();

function ensureProjectConfig() {
  const dirents = fs.readdirSync(jestProjectDir, { withFileTypes: true });

  if (!dirents.some((dirent) => dirent.name === "jest.config.js")) {
    throw new Error(
      `Could not locate a file named jest.config.js in ${jestProjectDir}.`
    );
  }

  if (!dirents.some((dirent) => dirent.name === "cljest.edn")) {
    throw new Error(
      `Could not locate a file named cljest.edn in ${jestProjectDir}.`
    );
  }
}

function withEnsuredProjectConfig(fn) {
  return function (...args) {
    ensureProjectConfig();

    return fn(...args);
  };
}

const getRootDir = withEnsuredProjectConfig(() => jestProjectDir);
const getBuildDir = withEnsuredProjectConfig(() =>
  path.resolve(jestProjectDir, ".jest")
);
const getJestConfig = withEnsuredProjectConfig(() =>
  require(path.join(jestProjectDir, "jest.config.js"))
);
const getCljestConfig = withEnsuredProjectConfig(() => {
  const configPath = path.resolve(jestProjectDir, "cljest.edn");
  const rawCljestConfig = fs.readFileSync(configPath).toString();

  return parseEDNString(rawCljestConfig, {
    mapAs: "object",
    keywordAs: "string",
  });
});

const getPathsFromCljestConfig = withEnsuredProjectConfig(() => {
  const { "test-src-dirs": testSrcDirs } = getCljestConfig();

  return testSrcDirs.map((p) => path.resolve(jestProjectDir, p));
});

const getServerUrl = withEnsuredProjectConfig(() => {
  const { port } = getCljestConfig();

  if (!port) {
    return `http://localhost:9003`;
  }

  return `http://localhost:${parseInt(port, 10)}`;
});

function generateTestRegexes() {
  const { "ns-suffixes": raw } = getCljestConfig();
  let nsSuffixes;

  if (!raw) {
    nsSuffixes = ["_test"];
  } else {
    nsSuffixes = raw.map(({ sym: suffix }) => suffix.replace(/-/g, "_"));
  }

  return nsSuffixes.map((suffix) => `(.*)${suffix}.cljs`);
}

module.exports = {
  getRootDir,
  getBuildDir,
  getServerUrl,
  getJestConfig,
  getCljestConfig,
  getPathsFromCljestConfig,
  generateTestRegexes,
};
