/*
 * @test
 * @summary Do not issue unique.leaked for a use of a variable whose type is not written in source
 * code.  JDK 22+ javac attaches the annotations in InferredTypesAliasingBase.class to types read
 * from that class file, including types that javac infers from them.  On JDK 21 and earlier,
 * javac does not attach them, so this test passes even without the fix.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.common.aliasing.AliasingChecker InferredTypesAliasingBase.java
 * @compile/fail/ref=InferredTypesAliasing.goal -XDrawDiagnostics -processor org.checkerframework.common.aliasing.AliasingChecker InferredTypesAliasing.java
 */

import org.checkerframework.common.aliasing.qual.Unique;

public class InferredTypesAliasing {
  static Object sink;

  void varLocal() {
    var sb = InferredTypesAliasingBase.create();
    sink = sb;
  }

  void varLocalParenthesized() {
    var sb = InferredTypesAliasingBase.create();
    sink = (sb);
  }

  void varLocalInArray() {
    var sb = InferredTypesAliasingBase.create();
    Object[] array = {sb};
  }

  // The annotations written in source code are still used.

  void explicitLocal() {
    @Unique StringBuilder sb = InferredTypesAliasingBase.create();
    sink = sb;
  }
}
