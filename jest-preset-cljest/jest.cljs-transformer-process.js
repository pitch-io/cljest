const { runAsWorker } = require("synckit");
const fetch = require("node-fetch");
const path = require("path");
const fs = require("fs");
const { getServerUrl, getBuildDir } = require("./utils");

const serverUrl = getServerUrl();
const buildDir = getBuildDir();

function getPrecompiledFile(sourceText) {
  const namespace = sourceText.match(/ns\s([\w\.\-]+)/)[1];
  const filename = `${namespace.replace(/-/g, "_")}.js`;

  return {
    status: "success",
    code: fs.readFileSync(path.resolve(buildDir, filename)).toString(),
    map: fs.readFileSync(path.resolve(buildDir, `${filename}.map`)).toString(),
  };
}

async function getServerCompiledFile(sourceText, sourcePath) {
  const { status, error } = await (
    await fetch(`${serverUrl}/build-status`)
  ).json();

  if (status === "initial-failure") {
    // If the initial compilation failed, nothing actually compiled yet, so we should fail all files.
    return {
      status,
      error,
    };
  } else if (status !== "success" && error.indexOf(sourcePath) !== -1) {
    // It's fine to return the error here, and let the other files run. The compilation failed and so those files couldn't
    // have changed.
    return {
      status,
      error,
    };
  }

  const namespace = sourceText.match(/ns\s([\w\.\-]+)/)[1];
  const filename = `${namespace.replace(/-/g, "_")}.js`;

  return {
    status: "success",
    code: fs.readFileSync(path.resolve(buildDir, filename)).toString(),
    map: fs.readFileSync(path.resolve(buildDir, `${filename}.map`)).toString(),
  };
}

runAsWorker(async function runAsWorkerFn(sourceText, sourcePath) {
  try {
    if (process.env.CI) {
      return await getPrecompiledFile(sourceText);
    } else {
      return await getServerCompiledFile(sourceText, sourcePath);
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
