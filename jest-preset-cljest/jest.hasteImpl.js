const { getProjectConfig } = require('./utils')

const { roots } = getProjectConfig()

module.exports = {
  getHasteName(filePath) {
    const root = roots.find((root) => filePath.startsWith(root))

    if (root) {
      const baseHasteName = filePath
        .split(`${root}/`)[1]
        .replace(/\.clj(s|c)?/, '')
        .replace(/\_/g, '-')
        .replace(/\//g, '.')

      let result

      if (filePath.endsWith('.clj')) {
        result = `${baseHasteName}$macros`
      } else {
        result = baseHasteName
      }

      return result
    }

    return filePath
  },
}
