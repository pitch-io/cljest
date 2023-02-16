const fs = require('fs')
const path = require('path')
const { parseEDNString } = require('edn-data')

const jestProjectDir = process.cwd()

function getProjectConfig() {
  if (!jestProjectDir) {
    throw new Error(
      'Jest should be run from a project directory, which contains a file called jest.config.js'
    )
  }

  const dirents = fs.readdirSync(jestProjectDir, { withFileTypes: true })

  if (!dirents.some((dirent) => dirent.name === 'jest.config.js')) {
    throw new Error(`Could not locate a file named jest.config.js in ${jestProjectDir}`)
  }

  return require(path.join(jestProjectDir, 'jest.config.js'))
}

function getPathsFromDepsEdn() {
  const depsEdnFile = path.resolve(jestProjectDir, 'deps.edn')
  const rawDepsEdn = fs.readFileSync(depsEdnFile).toString()
  const { paths } = parseEDNString(rawDepsEdn, { mapAs: 'object', keywordAs: 'string' })

  return paths.map((p) => path.resolve(jestProjectDir, p))
}

module.exports = { getProjectConfig, getPathsFromDepsEdn }
