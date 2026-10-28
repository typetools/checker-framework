// Test case for https://github.com/typetools/checker-framework/issues/8017
// and https://github.com/typetools/checker-framework/issues/8018

public class Issue8017 {
  class Inner {
    Inner(Issue8017 o) {}

    Inner(Issue8017 o, String s) {}
  }

  class VarargsInner {
    VarargsInner(String... s) {}

    VarargsInner(Issue8017 o, String... s) {}
  }

  void test(Issue8017 outer) {
    outer.new Inner(outer) {};
    this.new Inner(this) {};
    outer.new Inner(outer, "s") {};
    new Inner(this) {};
    outer.new VarargsInner() {};
    outer.new VarargsInner("a", "b") {};
    outer.new VarargsInner(outer) {};
    outer.new VarargsInner(outer, "a") {};
  }
}
