import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

public class AnnotatedForTest {
  /*
       Test a mix of @SuppressWarnings with @AnnotatedFor. @SuppressWarnings should win, but only within the kinds of warnings it promises to suppress. It should win because
       it is a specific intent of suppressing warnings, whereas NOT suppressing warnings using AnnotatedFor is a default behavior, and SW is a user-specified behavior.
  */

  // Test unannotated class initializer - no warnings should be issued
  @SuperQual Object o1 = annotatedMethod(new Object());
  @SubQual Object o2 = annotatedMethod(new Object());
  Object o3 = unannotatedMethod(o2);
  Object o4 = unannotatedMethod(o1);

  static @SuperQual Object so1;
  static @SubQual Object so2;
  static Object so3, so4;

  // Test unannotated static initializer block - no warnings should be issued
  static {
    so1 = staticAnnotatedMethod(new Object());
    so2 = staticAnnotatedMethod(new Object());
    so3 = staticUnannotatedMethod(so2);
    so4 = staticUnannotatedMethod(so1);
  }

  @SuperQual Object o5;
  @SubQual Object o6;
  Object o7, o8;

  // Test unannotated nonstatic initializer block - no warnings should be issued
  {
    o5 = annotatedMethod(new Object());
    o6 = annotatedMethod(new Object());
    o7 = unannotatedMethod(o6);
    o8 = unannotatedMethod(o5);
  }

  // This method is @AnnotatedFor("subtyping") so it can cause errors to be issued by calling
  // other methods.
  @AnnotatedFor("subtyping")
  void method1() {
    // When calling annotatedMethod, we expect the usual (non-conservative) defaults, since
    // @SuperQual is annotated with @DefaultQualifierInHierarchy.
    @SuperQual Object o1 = annotatedMethod(new Object());
    // :: error: (assignment)
    @SubQual Object o2 = annotatedMethod(new Object());

    // When calling unannotatedMethod, we expect the conservative defaults.
    Object o3 = unannotatedMethod(o2);
    // :: error: (argument)
    Object o4 = unannotatedMethod(o1);

    // Testing that @AnnotatedFor({}) behaves the same way as not putting an @AnnotatedFor
    // annotation.
    Object o5 = unannotatedMethod(o2);
    // :: error: (argument)
    Object o6 = unannotatedMethod(o1);

    // Testing that @AnnotatedFor(a different typesystem) behaves the same way @AnnotatedFor({})
    Object o7 = unannotatedMethod(o2);
    // :: error: (argument)
    Object o8 = unannotatedMethod(o1);
  }

  @SuppressWarnings("all")
  @AnnotatedFor(
      "subtyping") // Same as method1, but the @SuppressWarnings overrides the @AnnotatedFor.
  void method2() {
    // When calling annotatedMethod, we expect the usual (non-conservative) defaults, since
    // @SuperQual is annotated with @DefaultQualifierInHierarchy.
    @SuperQual Object o1 = annotatedMethod(new Object());
    @SubQual Object o2 = annotatedMethod(new Object());

    // When calling unannotatedMethod, we expect the conservative defaults.
    Object o3 = unannotatedMethod(o2);
    Object o4 = unannotatedMethod(o1);

    // Testing that @AnnotatedFor({}) behaves the same way as not putting an @AnnotatedFor
    // annotation.
    Object o5 = unannotatedMethod(o2);
    Object o6 = unannotatedMethod(o1);

    // Testing that @AnnotatedFor(a different typesystem) behaves the same way @AnnotatedFor({})
    Object o7 = unannotatedMethod(o2);
    Object o8 = unannotatedMethod(o1);
  }

  @SuppressWarnings("nullness")
  @AnnotatedFor("subtyping") // Similar to method1. The @SuppressWarnings does not override the
  // @AnnotatedFor because it suppressing warnings for a different typesystem.
  void method3() {
    // When calling annotatedMethod, we expect the usual (non-conservative) defaults, since
    // @SuperQual is annotated with @DefaultQualifierInHierarchy.
    @SuperQual Object o1 = annotatedMethod(new Object());
    // :: error: (assignment)
    @SubQual Object o2 = annotatedMethod(new Object());
  }

  @AnnotatedFor("subtyping")
  Object annotatedMethod(Object p) {
    return new Object();
  }

  Object unannotatedMethod(Object p) {
    return new Object();
  }

  @AnnotatedFor("subtyping")
  static Object staticAnnotatedMethod(Object p) {
    return new Object();
  }

  static Object staticUnannotatedMethod(Object p) {
    return new Object();
  }

  @AnnotatedFor({})
  Object unannotatedMethod2(Object p) {
    return new Object();
  }

  @AnnotatedFor("nullness")
  Object annotatedForADifferentTypeSystemMethod(Object p) {
    return new Object();
  }

  // Annotated for more than one type system
  @AnnotatedFor({"nullness", "subtyping"})
  void method4() {
    // :: error: (assignment)
    @SubQual Object o2 = new @SuperQual Object();
  }

  // Different way of writing the checker name
  @AnnotatedFor("SubtypingChecker")
  void method5() {
    // :: error: (assignment)
    @SubQual Object o2 = new @SuperQual Object();
  }

  // Different way of writing the checker name
  @AnnotatedFor("org.checkerframework.common.subtyping.SubtypingChecker")
  void method6() {
    // :: error: (assignment)
    @SubQual Object o2 = new @SuperQual Object();
  }

  // Every method in this class should issue warnings for subtyping even if it's not marked with
  // @AnnotatedFor, unless it's marked with @SuppressWarnings.
  @AnnotatedFor("subtyping")
  class annotatedClass {
    // Test annotated class initializer
    // When calling annotatedMethod, we expect the usual (non-conservative) defaults, since
    // @SuperQual is annotated with @DefaultQualifierInHierarchy.
    @SuperQual Object o1 = annotatedMethod(new Object());
    // :: error: (assignment)
    @SubQual Object o2 = annotatedMethod(new Object());

    // When calling unannotatedMethod, we expect the conservative defaults.
    Object o3 = unannotatedMethod(o2);
    // :: error: (argument)
    Object o4 = unannotatedMethod(o1);

    @SuperQual Object o5;
    @SubQual Object o6;
    Object o7, o8;

    // Test annotated nonstatic initializer block
    {
      o5 = annotatedMethod(new Object());
      // :: error: (assignment)
      o6 = annotatedMethod(new Object());
      o7 = unannotatedMethod(o6);
      // :: error: (argument)
      o8 = unannotatedMethod(o5);
    }

    void method1() {
      // :: error: (assignment)
      @SubQual Object o2 = new @SuperQual Object();
    }

    @SuppressWarnings("all")
    void method2() {
      @SubQual Object o2 = new @SuperQual Object();
    }
  }

  @AnnotatedFor("subtyping")
  static class staticAnnotatedForClass {
    static @SuperQual Object so1;
    static @SubQual Object so2;
    static Object so3, so4;

    // Test annotated static initializer block
    static {
      so1 = staticAnnotatedMethod(new Object());
      // :: error: (assignment)
      so2 = staticAnnotatedMethod(new Object());
      so3 = staticUnannotatedMethod(so2);
      // :: error: (argument)
      so4 = staticUnannotatedMethod(so1);
    }
  }

  @SuppressWarnings("all") // @SuppressWarnings("all") overrides @AnnotatedFor("subtyping")
  @AnnotatedFor("subtyping")
  class annotatedAndWarningsSuppressedClass {
    // Test annotated class initializer whose warnings are suppressed.
    @SuperQual Object o1 = annotatedMethod(new Object());
    @SubQual Object o2 = annotatedMethod(new Object());
    Object o3 = unannotatedMethod(o2);
    Object o4 = unannotatedMethod(o1);

    @SuperQual Object o5;
    @SubQual Object o6;
    Object o7, o8;

    // Test annotated nonstatic initializer block whose warnings are suppressed.
    {
      o5 = annotatedMethod(new Object());
      o6 = annotatedMethod(new Object());
      o7 = unannotatedMethod(o6);
      o8 = unannotatedMethod(o5);
    }

    void method1() {
      @SubQual Object o2 = new @SuperQual Object();
    }
  }

  @SuppressWarnings("all")
  @AnnotatedFor("subtyping")
  static class staticAnnotatedAndWarningsSuppressedClass {
    static @SuperQual Object so1;
    static @SubQual Object so2;
    static Object so3, so4;

    // Test annotated static initializer block whose warnings are suppressed.
    static {
      so1 = staticAnnotatedMethod(new Object());
      so2 = staticAnnotatedMethod(new Object());
      so3 = staticUnannotatedMethod(so2);
      so4 = staticUnannotatedMethod(so1);
    }
  }
}
