const path = require('path')
const { getPathsFromDepsEdn } = require('jest-preset-shadow/utils')

const shadowOutputDir = path.resolve(__dirname, '.jest')

module.exports = {
  setupFilesAfterEnv: [path.resolve(__dirname, 'jest.setup.js')],
  testRegex: ['(.*)_spec.cljs'],
  modulePaths: [shadowOutputDir],
  rootDir: shadowOutputDir,
  roots: getPathsFromDepsEdn(),
  reporters: ['default', ['jest-junit', { outputDirectory: 'reports/jest' }]],
  clearMocks: true,

  globals: {
    shadowOutputDir,
    serverUrl: 'http://localhost:9111',
  },

  preset: 'jest-preset-shadow',
}
