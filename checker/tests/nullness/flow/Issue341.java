// Test case for issue #341:
// https://github.com/typetools/checker-framework/issues/341

// @skip-test

class Test {

  static class Provider {
    public final Object get = new Object();
  }

  // Because p is non-null, the try block will complete normally and result
  // will get set and thus result will be non-null.  However, CFG
  // construction is not aware that the potential null-pointer exception
  // will never happen.
  Object execute(Provider p) {
    final Object result;
    try {
      result = p.get;
    } finally {
    }
    return result;
  }

}
