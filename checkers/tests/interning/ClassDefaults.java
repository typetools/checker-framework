import checkers.interning.quals.Interned;
import java.util.List;

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
