const path = require('path')
const { getProjectConfig } = require('./utils')

const { globals } = getProjectConfig()

if (!globals || !globals.shadowOutputDir || !globals.serverUrl) {
  throw new Error('You must set the global config variables shadowOutputDir and serverUrl')
}

module.exports = {
  moduleFileExtensions: ['clj', 'cljs', 'js'],
  globalSetup: path.resolve(__dirname, 'jest.globalSetup.js'),
  resolver: path.resolve(__dirname, 'jest.resolver.js'),
  transform: {
    '\\.(clj|cljs)$': path.resolve(__dirname, 'jest.cljs-transformer.js'),
  },
  watchPlugins: [path.resolve(__dirname, 'jest.ns-watch-plugin.js')],
}
