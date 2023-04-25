const path = require("path");

module.exports = {
  setupFilesAfterEnv: [path.resolve(__dirname, "jest.setup.js")],
  reporters: ["default", ["jest-junit", { outputDirectory: "reports/jest" }]],
  clearMocks: true,

  preset: "jest-preset-cljest",
  testEnvironment: "jest-environment-jsdom",
};
