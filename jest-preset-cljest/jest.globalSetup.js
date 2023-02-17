const fetch = require('node-fetch')
const { getServerUrl } = require('./utils')

async function fetchUntilAvailable() {
  if (process.env.CI) {
    return
  }

  try {
    await fetch(`${getServerUrl()}/compile`)
  } catch (_) {
    await new Promise((resolve) => setTimeout(resolve, 50))

    return fetchUntilAvailable()
  }
}

module.exports = async function globalSetup() {
  await fetchUntilAvailable()
}
