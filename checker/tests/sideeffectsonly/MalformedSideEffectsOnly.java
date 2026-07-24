import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class MalformedSideEffectsOnly {

  // An unparseable @SideEffectsOnly expression is reported as an error, not a crash.
  @SideEffectsOnly("#1.noSuchMethod()")
  // :: error: (flowexpr.parse.error)
  void method(Object o) {}

  // A parse error whose message key is not `flowexpr.parse.error` is also reported as an error,
  // not a crash.
  @SideEffectsOnly("#2")
  // :: error: (flowexpr.parse.index.too.big)
  void tooFewParameters(Object o) {}
}
