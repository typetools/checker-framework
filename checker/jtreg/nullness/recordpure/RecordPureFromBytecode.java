/*
 * @test
 * @summary Test that compiler-generated record accessors are pure when read from bytecode.
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker ../recordpurelib/RecordPureLib.java
 * @compile/fail/ref=RecordPureFromBytecode.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext RecordPureFromBytecode.java
 */

class RecordPureFromBytecode {
  int generatedAccessorIsPure(RecordPureLib r) {
    if (r.generated() == null) {
      return 0;
    }
    return r.generated().length();
  }

  int explicitAccessorIsNotPure(RecordPureLib r) {
    if (r.explicit() == null) {
      return 0;
    }
    return r.explicit().length();
  }
}
