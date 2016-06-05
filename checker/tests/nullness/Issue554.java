// @skip-test

// Test case for issue #554: https://github.com/typetools/checker-framework/issues/554

import org.checkerframework.checker.nullness.qual.*;

class MonotonicNonNullConstructorTest1 {
  static class Data {
    @MonotonicNonNull Object field;
  }

  Data data;
  Object object;

  @RequiresNonNull("#1.field")
  MonotonicNonNullConstructorTest1(final Data data) {
    this.data = data;
    this.object = data.field;
  }
}

class MonotonicNonNullConstructorTest2 {
  static class Data {
    @MonotonicNonNull Object field;
  }

  Data data;
  Object object;

  @RequiresNonNull("#1.field")
  MonotonicNonNullConstructorTest2(final Data data) {
    // reverse the assignments
    this.object = data.field;
    this.data = data;
  }
}

class MonotonicNonNullConstructorTest3 {
  static class Data {
    @MonotonicNonNull Object field;
  }

  Data data;
  Object object;

  @RequiresNonNull("#1.field")
  MonotonicNonNullConstructorTest3(final Data dataParam) {
    // use a parameter name that does not shadow the field
    this.data = dataParam;
    this.object = dataParam.field;
  }
}

class MonotonicNonNullConstructorTest4 {
  static class Data {
    @MonotonicNonNull Object field;
  }

  Data data;
  Object object;

  @RequiresNonNull("#1.field")
  MonotonicNonNullConstructorTest4(final Data dataParam) {
    // use a parameter name that does not shadow the field
    // and reverse the assignments
    this.object = dataParam.field;
    this.data = dataParam;
  }
}
