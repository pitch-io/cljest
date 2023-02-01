const path = require('path')
const fs = require('fs')
const config = require('./jest.config')

const preloadFile = path.resolve(config.globals.shadowOutputDir, 'cljest.preloads.js')

// The preloads files may not exist if the initial compilation failed
if (fs.existsSync(preloadFile)) {
  require(preloadFile)
}
