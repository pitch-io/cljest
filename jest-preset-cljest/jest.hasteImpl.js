const { getPathsFromCljestConfig } = require('./utils')

const paths = getPathsFromCljestConfig()

module.exports = {
  getHasteName(filePath) {
    const foundPath = paths.find((root) => filePath.startsWith(root))

    if (foundPath) {
      const baseHasteName = filePath
        .split(`${foundPath}/`)[1]
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
