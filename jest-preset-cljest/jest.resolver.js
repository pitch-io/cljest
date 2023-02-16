const path = require('path')
const { getProjectConfig } = require('./utils')

const {
  globals: { shadowOutputDir },
} = getProjectConfig()

module.exports = function (filePath, options) {
  try {
    return options.defaultResolver(filePath, options)
  } catch (e) {
    const absPath = path.resolve(shadowOutputDir, filePath)

    return options.defaultResolver(absPath, options)
  }
}
