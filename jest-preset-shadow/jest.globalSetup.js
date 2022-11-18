const fetch = require('node-fetch')
const { getProjectConfig } = require('./utils')

async function fetchUntilAvailable(serverUrl) {
  if (process.env.CI) {
    return
  }

  try {
    await fetch(`${serverUrl}/compile`)
  } catch (_) {
    await new Promise((resolve) => setTimeout(resolve, 50))

    return fetchUntilAvailable(serverUrl)
  }
}

module.exports = async function globalSetup() {
  const { globals } = await getProjectConfig()

  if (!globals.serverUrl) {
    throw new Error(
      'config.globals.serverUrl must be set to the server URL of the shadow compilation server. E.g. http://localhost:9001'
    )
  }

  await fetchUntilAvailable(globals.serverUrl)
}
