const {
  PatternPrompt,
  Prompt,
  printRestoredPatternCaret,
  printPatternCaret,
} = require("jest-watcher");
const chalk = require("chalk");
const fs = require("fs");
const { generateTestRegexes, getPathsFromCljestConfig } = require("./utils");

const roots = getPathsFromCljestConfig();
const testRegexes = generateTestRegexes();

function* traverseDirectory(dir) {
  const dirents = fs.readdirSync(dir, { withFileTypes: true });

  for (const dirent of dirents) {
    if (dirent.isFile()) {
      yield `${dir}/${dirent.name}`;
    } else if (dirent.isDirectory()) {
      yield* traverseDirectory(`${dir}/${dirent.name}`);
    }
  }
}

class NamespacePrompt extends PatternPrompt {
  constructor(pipe, prompt, possibleNamespaces) {
    super(pipe, prompt);

    this._pipe = pipe;
    this._prompt = prompt;
    this._entityName = "namespace";
    this._possibleNamespaces = possibleNamespaces;
  }

  _onChange(pattern, options) {
    super._onChange(pattern, options);

    printPatternCaret(pattern, this._pipe);

    const matchedNamespaces = this._getMatchedNamespaces(pattern);

    this._pipe.write("\n");

    if (matchedNamespaces.length > 0) {
      // Print the first 10...
      matchedNamespaces.slice(0, 10).forEach((ns) => {
        this._pipe.write(`${chalk.gray(ns)}\n`);
      });

      // ...and a message if there are more
      if (matchedNamespaces.length > 10) {
        this._pipe.write(
          `... and ${matchedNamespaces.length - 10} more namespaces.`
        );
      }
    } else {
      this._pipe.write("No namespaces found.");
    }

    printRestoredPatternCaret(pattern, this._currentUsageRows, this._pipe);
  }

  _getMatchedNamespaces(pattern) {
    try {
      return this._possibleNamespaces.filter((ns) => ns.match(pattern));
    } catch (_) {
      return [];
    }
  }
}

class NamespaceWatchPlugin {
  constructor({ stdout }) {
    this._stdout = stdout;
    this._prompt = new Prompt();
  }

  onKey(value) {
    this._prompt.put(value);
  }

  run(globalConfig, updateConfigAndRun) {
    const possibleNamespaces = roots.reduce((acc, root) => {
      // Assume root is absolute path
      for (const filename of traverseDirectory(root)) {
        if (testRegexes.some((regex) => new RegExp(regex).test(filename))) {
          acc.push(
            filename
              .replace(`${root}/`, "")
              .slice(0, -5)
              .replace(/_/g, "-")
              .replace(/\//g, ".")
          );
        }
      }

      return acc;
    }, []);

    const prompt = new NamespacePrompt(
      this._stdout,
      this._prompt,
      possibleNamespaces
    );

    return new Promise((resolve, reject) => {
      prompt.run((partialNs) => {
        const filename = partialNs.replace(/-/g, "_");

        updateConfigAndRun({
          testPathPattern: filename,
        });

        // see https://jestjs.io/docs/watch-plugins#runglobalconfig-updateconfigandrun. We should explicitly not
        // return a value here.
        resolve();
      }, reject);
    });
  }

  getUsageInfo() {
    return {
      key: "n",
      prompt: "filter by namespace regex",
    };
  }
}

module.exports = NamespaceWatchPlugin;
