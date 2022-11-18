const crypto = require('crypto')
const path = require('path')
const { createSyncFn } = require('synckit')
const chalk = require('chalk')

module.exports = {
  process: (sourceText, sourcePath, options) => {
    // When we don't run in the CI we make an API call to `http://server/build-status` to get the latest
    // status of the build. Our compilation generates CJS modules, and Jest's support for ESM is both
    // experimental and the shadow-cljs build isn't fully compatible with Jest. `processAsync` only seems
    // to work when running Jest in ESM mode, so we need to call it using `synckit` instead.
    const callProcess = createSyncFn(path.resolve(__dirname, 'jest.cljs-transformer-process.js'))

    const result = callProcess(sourceText, sourcePath, options)

    const { status, error, code, map } = result

    if (status !== 'success') {
      const message =
        status === 'initial-failure'
          ? chalk.bold.red(
              'Initial compilation failed. You need to fix this before you can run any tests.'
            )
          : chalk.yellow('Incremental compilation failed.')
      const err = new Error(`${message}\n\n${error}`)

      throw err
    }

    return {
      code,
      map,
    }
  },
  getCacheKey() {
    // We don't need to cache the result because we're not compiling here, it happens in `globalSetup`, so
    // the benefits are minimal.
    return crypto.randomBytes(32).toString('hex')
  },
}
