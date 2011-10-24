import checkers.interning.quals.Interned;
import java.util.List;

/*
 * This test case excercises the interaction between class annotations
 * and method type argument inference.
 * A previously existing Unqualified annotation wasn't correctly removed.
 */
class ClassDefaults {
  @Interned
  class Test {}
  public static interface Visitor<T> {}

  class GuardingVisitor implements Visitor<List<Test>> {
      void call() {
          test(this);
      }
  }
  
  <T> T test(Visitor<T> p) {
      return null;
  }
  
  void call(GuardingVisitor p) {
      test(p);
  }
}
