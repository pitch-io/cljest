# CI/noninteractive environments

In the CI or other noninteractive environments, things are a little different -- you won't want to run `watch` mode since you don't need to watch for any changes.

In these cases `cljest` exposes a `compile` mode. To run it, call `clj -X cljest.compilation/compile`.

You can then run Jest in CI mode by adding the environment variable `CI`: `CI=true npx jest`. **Note** that the `ci` flag for Jest is not the same, you'll need to add the `CI` environment variable as well.

# Example

Below is an example which closely resembles [`cljest`'s Github Actions workflow](https://github.com/pitch-io/cljest/blob/5d19b87021023daef75971ff005e05a288369c1d/.github/workflows/tests.yml#L15-L39). Setting things up in Circle or other CI environments should be very similar.

```yaml
jobs:
  cljest-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup java
        uses: actions/setup-java@v3
        with:
          distribution: 'corretto'
          java-version: '17'
      - name: Setup Clojure
        uses: DeLaGuardo/setup-clojure@9.5
        with:
          cli: 1.11.1.1224
      - name: Install dependencies
        run: npm install
      - name: Compile Jest tests
        run: clojure -X cljest.compilation/compile
      - name: Run Jest tests
        run: ./node_modules/.bin/jest --ci
        environment:
          CI: 'true'
```
