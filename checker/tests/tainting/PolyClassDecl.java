import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.tainting.qual.PolyTainted;

public class PolyClassDecl {
  // :: error: (invalid.polymorphic.qualifier)
  @PolyTainted static class Class1 {}
  // :: error: (invalid.polymorphic.qualifier)
  static class Class2<@PolyTainted T> {}
  // :: error: (invalid.polymorphic.qualifier)
  abstract static class Class3<T extends List<@PolyTainted String>> {}
  // :: error: (invalid.polymorphic.qualifier)
  interface Class4 extends List<@PolyTainted String> {}
  // :: error: (invalid.polymorphic.qualifier)
  // :: error: (declaration.inconsistent.with.implements.clause)
  interface Class5 extends @PolyTainted List<String> {}
  // :: error: (invalid.polymorphic.qualifier)
  abstract static class Class6 implements List<@PolyTainted String> {}

  void method() {
    ArrayList<@PolyTainted String> s = new ArrayList<@PolyTainted String>() {};
  }

  // :: error: (invalid.polymorphic.qualifier)
  <@PolyTainted T> T identity(T arg) {
    return arg;
  }
}
