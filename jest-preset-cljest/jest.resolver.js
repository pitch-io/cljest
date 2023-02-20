const path = require('path')
const { getBuildDir } = require('./utils')

const buildDir = getBuildDir()

module.exports = function (filePath, options) {
  try {
    return options.defaultResolver(filePath, options)
  } catch (e) {
    const absPath = path.resolve(buildDir, filePath)

    return options.defaultResolver(absPath, options)
  }
}
