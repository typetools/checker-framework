// Test case for EISOP issue #609:
// https://github.com/eisop/checker-framework/issues/609
enum EnumSwitch {
  FOO;

  EnumSwitch getIt() {
    return FOO;
  }

  String go() {
    EnumSwitch e = getIt();
    switch (e) {
      case FOO:
        return "foo";
    }
    // This location is reachable in general: the enum could evolve and add a new constant.
    // This cannot be the case here, because this code is in the enum declaration.
    // javac does not special case this and I do not see a reason to do so here.
    throw new AssertionError(e);
  }
}
