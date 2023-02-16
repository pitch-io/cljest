const dependencyExtractor = require('./jest.dependencyExtractor')

jest.mock('./utils', () => ({
  getProjectConfig: () => ({
    roots: ['/root_a', '/root_b'],
  }),
}))

describe('extract', () => {
  const code = `
    (ns my-cool-ns.fun
        (:require "some-node-dep"
                  some.cljs.dep
                  [some.cljs.other-dep :refer [a b]]
                  [some.cljs.another-dep :as another-dep]
                  yet.another-dep
                  "more-node"
                  the.last.one)
        (:require-macros my-cool-ns.fun
                         some.cljs.macros-dep))`

  it('returns empty array if the filePath is not in roots', () => {
    const result = dependencyExtractor.extract(code, '/root_c/my_cool_ns/fun.cljs')

    expect(Array.isArray(result)).toBeTruthy()
    expect(result).toHaveLength(0)
  })

  it('returns array of all deps from the given code', () => {
    const result = dependencyExtractor.extract(code, '/root_a/my_cool_ns/fun.cljs')

    expect(Array.isArray(result)).toBe(true)
    expect(result).toStrictEqual([
      'some-node-dep',
      'some.cljs.dep',
      'some.cljs.other-dep',
      'some.cljs.another-dep',
      'yet.another-dep',
      'more-node',
      'the.last.one',
      'my-cool-ns.fun$macros',
      'some.cljs.macros-dep$macros',
    ])
  })
})
