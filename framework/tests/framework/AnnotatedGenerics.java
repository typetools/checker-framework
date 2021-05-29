import org.checkerframework.framework.testchecker.util.*;

public class AnnotatedGenerics {

  public static void testNullableTypeVariable() {
    class Test<T> {
      @Odd T get() {
        return null;
      }
    }
    Test<String> l = null;
    String l1 = l.get();
    @Odd String l2 = l.get();

    Test<@Odd String> n = null;
    String n1 = n.get();
    @Odd String n2 = n.get();
  }

  // Tests the type of the constructed class is correctly inferred for generics.
  public void testConstructors() {
    // Variant without annotated type parameters
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd MyClass<@Odd String> innerClass1 = new @Odd MyClass<@Odd String>();
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd NormalClass<@Odd String> normal1 = new @Odd NormalClass<@Odd String>();

    // Should error because the RHS isn't annotated as '@Odd'
    // :: error: (assignment)
    @Odd MyClass<@Odd String> innerClass2 = new MyClass<@Odd String>();
    // :: error: (assignment)
    @Odd NormalClass<@Odd String> normal2 = new NormalClass<@Odd String>();

    // Variant with annotated type parameters
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd MyClass<String> innerClass3 = new @Odd MyClass<String>();
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd NormalClass<String> normal3 = new @Odd NormalClass<String>();

    // Should error because the RHS isn't annotated as '@Odd'
    // :: error: (assignment)
    @Odd MyClass<String> innerClass4 = new MyClass<String>();
    // :: error: (assignment)
    @Odd NormalClass<String> normal4 = new NormalClass<String>();
  }

  // Tests the type of the constructed class is correctly inferred when the
  // diamond operator is used.
  public void testConstructorsWithTypeParameterInferrence() {
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd MyClass<@Odd String> innerClass1 = new @Odd MyClass<>();
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd NormalClass<@Odd String> normal1 = new @Odd NormalClass<>();

    // Should error because the RHS isn't annotated as '@Odd'
    // :: error: (assignment)
    @Odd MyClass<@Odd String> innerClass2 = new MyClass<>();
    // :: error: (assignment)
    @Odd NormalClass<@Odd String> normal2 = new NormalClass<>();

    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd MyClass<String> innerClass3 = new @Odd MyClass<>();
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd NormalClass<String> normal3 = new @Odd NormalClass<>();

    // Should error because the RHS isn't annotated as '@Odd'
    // :: error: (assignment)
    @Odd MyClass<String> innerClass4 = new MyClass<>();
    // :: error: (assignment)
    @Odd NormalClass<String> normal4 = new NormalClass<>();
  }

  // Tests the type of the constructor is appropriately inferred for anonymous classes
  // N.B. This does not / cannot assert that the RHS is infact a subtype of the LHS.
  public void testAnonymousConstructors() {
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd MyClass<@Odd String> innerClass1 = new @Odd MyClass<@Odd String>() {};
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd NormalClass<@Odd String> normal1 = new @Odd NormalClass<@Odd String>() {};

    // Should error because the RHS isn't annotated as '@Odd'
    // :: error: (assignment)
    @Odd MyClass<@Odd String> innerClass2 = new MyClass<@Odd String>() {};
    // :: error: (assignment)
    @Odd NormalClass<@Odd String> normal2 = new NormalClass<@Odd String>() {};

    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd MyClass<String> innerClass3 = new @Odd MyClass<String>() {};
    // :: warning: (cast.unsafe.constructor.invocation)
    @Odd NormalClass<String> normal3 = new @Odd NormalClass<String>() {};

    // Should error because the RHS isn't annotated as '@Odd'
    // :: error: (assignment)
    @Odd MyClass<String> innerClass4 = new MyClass<String>() {};
    // :: error: (assignment)
    @Odd NormalClass<String> normal4 = new NormalClass<String>() {};
  }

  // The following test cases are not included because the Java compiler currently does
  // not seem to support the diamond operator in conjunction with anonymous classes.
  //
  //    public void testAnonymousConstructorsWithTypeParameterInferrence() {
  //          @Odd MyClass<@Odd String> innerClass1 = new @Odd MyClass<>() {};
  //          @Odd NormalClass<@Odd String> normal1 = new @Odd NormalClass<>() {};
  //
  //          // Should error because the RHS isn't annotated as '@Odd'
  //          @Odd MyClass<@Odd String> innerClass2 = new MyClass<>() {};
  //          @Odd NormalClass<@Odd String> normal2 = new NormalClass<>() {};
  //
  //          @Odd MyClass<String> innerClass3 = new @Odd MyClass<>() {};
  //          @Odd NormalClass<String> normal3 = new @Odd NormalClass<>() {};
  //
  //          // Should error because the RHS isn't annotated as '@Odd'
  //          @Odd MyClass<String> innerClass4 = new MyClass<>() {};
  //          @Odd NormalClass<String> normal4 = new NormalClass<>() {};
  //    }

  static class NormalClass<T> {
    @Odd T get() {
      return null;
    }
  }

  class MyClass<T> implements java.util.Iterator<@Odd T> {
    public boolean hasNext() {
      return true;
    }

    public @Odd T next() {
      return null;
    }

    public void remove() {}
  }
}
