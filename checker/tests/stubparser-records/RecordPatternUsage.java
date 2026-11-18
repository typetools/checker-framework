// @below-java21-jdk-skip-test

// A pattern variable for a record component that is annotated only in a stub file has the
// component's type from the stub file.

class RecordPatternUsage {
  void varPattern(PatternPairRecord p) {
    if (p instanceof PatternPairRecord(var k, var v)) {
      k.toString();
      // :: error: [dereference.of.nullable]
      v.toString();
    }
  }

  void typePattern(PatternPairRecord p) {
    if (p instanceof PatternPairRecord(String k, Object v)) {
      k.toString();
      // :: error: [dereference.of.nullable]
      v.toString();
    }
  }
}
