// Test for Issue 258
// https://code.google.com/p/checker-framework/issues/detail?id=258
public class GenericsBounds2 {
  <I extends Object, C extends I> void method(C arg) {
    arg.toString();
  }
}
