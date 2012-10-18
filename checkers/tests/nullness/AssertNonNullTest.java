import checkers.nullness.quals.*;

public class AssertNonNullTest {
  public @Nullable String s;
   
  //:: error: (assert.postcondition.not.satisfied)
  public @AssertNonNullAfter("s") void makeNN() {
    s = null;
  }
   
  public static void main(String[] args) {
    AssertNonNullTest a = new AssertNonNullTest();
    //:: error: (dereference.of.nullable)
    a.s.equals("we");
    AssertNonNullTest b = new AssertNonNullTest();
    b.makeNN();
    b.s.equals("we");
  }
}
