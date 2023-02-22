const fs = require("fs");
const crypto = require("crypto");
const { getPathsFromCljestConfig } = require("./utils");

function getDependenciesFromTokens(tokens) {
  const result = [];
  let currDep = "";
  let currDepth = -1;
  let ignoreRestInBracket = false;

  for (const token of tokens) {
    // Start the depth at -1 to denote that we're not even considering capturing actual dependencies
    // yet (it's still in `(:require`). Once we encounter something that isn't `(:require`/`(require-macros`
    // we can increase the depth and start capturing dependencies.
    if (currDepth === -1 && token.match(/\(|:|[a-z\-]/)) {
      continue;
    } else if (currDepth === -1) {
      currDepth++;
    }

    if (token === "[") {
      currDepth++;
      continue;
    }

    if (token === "]") {
      currDepth--;

      if (currDepth === 0) {
        ignoreRestInBracket = false;
      }

      continue;
    }

    // We can just ignore quotes
    if (token === '"') {
      continue;
    }

    if (ignoreRestInBracket) {
      continue;
    }

    // We know the token isn't a quote or a bracket. If we're at depth 0 (no brackets)
    // or depth 1 (just the first level), and the token isn't a space, capture it.
    if (currDepth <= 1 && !token.match(/\s/)) {
      currDep += token;
      continue;
    }

    // If we're at depth 0/1 and have a current dependency, we can add it to the result
    if (currDepth <= 1 && currDep !== "" && token.match(/\s/)) {
      // If we are at depth 1, we know we're in a bracket and want to ignore all remaining tokens
      // inside of this bracket.
      if (currDepth === 1) {
        ignoreRestInBracket = true;
      }

      result.push(currDep);
      currDep = "";
      continue;
    }
  }

  // If we have a token that is right before the closing `)`, add it to the result.
  if (currDep !== "") {
    result.push(currDep);
  }

  return result;
}

function getDepsForOpening(code, opening) {
  const pos = code.indexOf(opening);

  if (pos === -1) {
    return [];
  }

  const tokens = [];
  let curr = pos;

  while (true) {
    const currToken = code[curr];

    if (currToken === ")") {
      break;
    }

    tokens.push(currToken);
    curr++;
  }

  return getDependenciesFromTokens(tokens);
}

module.exports = {
  extract(code, filePath) {
    const testSrcPaths = getPathsFromCljestConfig();

    if (!testSrcPaths.some((p) => filePath.startsWith(p))) {
      return [];
    }

    const REQUIRE_STR = "(:require";
    const REQUIRE_MACROS_STR = "(:require-macros";

    const requireDeps = getDepsForOpening(code, REQUIRE_STR);
    const requireMacrosDeps = getDepsForOpening(code, REQUIRE_MACROS_STR).map(
      (dep) => `${dep}$macros`
    );

    return requireDeps.concat(requireMacrosDeps);
  },
  getCacheKey() {
    return crypto
      .createHash("md5")
      .update(fs.readFileSync(__filename))
      .digest("hex");
  },
};
