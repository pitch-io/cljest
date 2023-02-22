const { runAsWorker } = require("synckit");
const fetch = require("node-fetch");
const nodePath = require("path");

/**
 * Replaces hyphens with underscores for the string.
 *
 * This won't work for a more complicated namespace, but let's cross that bridge
 * when we get there.
 *
 * @param      {String}  str
 */
function kindOfMunge(str) {
  return str.replace(/\-/g, "_");
}

function ciGetSetupFile(config, buildDir) {
  const { "setup-ns": raw } = config;

  let setupFilePath;
  if (raw) {
    const { sym: ns } = raw;
    setupFilePath = `${kindOfMunge(ns)}.js`;
  } else {
    setupFilePath = `cljest.setup.js`;
  }

  return {
    status: "success",
    path: nodePath.resolve(buildDir, setupFilePath),
  };
}

async function serverGetSetupFile(serverUrl) {
  const resp = await fetch(`${serverUrl}/setup-file`);

  const { error, path } = await resp.json();

  if (resp.ok) {
    return {
      status: "success",
      path,
    };
  }

  return {
    status: "failure",
    error,
  };
}

runAsWorker(async function loadSetupFileFn(buildDir, serverUrl, cljestConfig) {
  try {
    if (process.env.CI) {
      return ciGetSetupFile(cljestConfig, buildDir);
    } else {
      return await serverGetSetupFile(serverUrl);
    }
  } catch (e) {
    // Always capture any error that happens with the code above and return it in the expected object shape
    // Otherwise, there be dragons.
    return {
      status: "failure",
      error: e.message,
    };
  }
});
