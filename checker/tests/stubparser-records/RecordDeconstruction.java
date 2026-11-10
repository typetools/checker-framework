// @below-java21-jdk-skip-test

// Test that a deconstruction pattern uses the annotations that a stub file writes on a record
// component whose accessor is not in the stub file.

class RecordDeconstruction {
  void deconstruct(Object o) {
    if (o instanceof PairRecord(String k, Object v)) {
      k.length();
      // :: error: [dereference.of.nullable]
      v.toString();
    }
  }
}
