const hasteImpl = require("./jest.hasteImpl");

jest.mock("./utils", () => ({
  getPathsFromCljestConfig: () => ["/root_a", "/root_b"],
}));

describe("getHasteName", () => {
  it("returns filePath if the path is not in a root", () => {
    const path = "/root_c/my_cool_ns/fun.cljs";

    expect(hasteImpl.getHasteName(path)).toStrictEqual(path);
  });

  it("returns the filePath as a normal namespace if the path is in a root", () => {
    const path = "/root_a/my_cool_ns/fun.cljs";

    expect(hasteImpl.getHasteName(path)).toBe("my-cool-ns.fun");
  });

  it("returns the filePath as a macros namespace if the path is in a root and ends with .clj", () => {
    const path = "/root_a/my_cool_ns/fun.clj";

    expect(hasteImpl.getHasteName(path)).toBe("my-cool-ns.fun$macros");
  });
});
