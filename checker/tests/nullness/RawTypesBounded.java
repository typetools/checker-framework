// Note that this file is a near duplicate in /nullness and /nullness-uninit

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
public class RawTypesBounded {

  class Bad {
    @NonNull String field;

    public Bad() {
      // :: error: (method.invocation.invalid)
      this.init(); // error
      // :: error: (method.invocation.invalid)
      init(); // error

      this.field = "field"; // valid
      // :: error: (assignment.type.incompatible)
      this.field = null; // error
      field = "field"; // valid
      // :: error: (assignment.type.incompatible)
      field = null; // error
    }

    void init() {
      output(this.field.length()); // valid
    }
  }

  class A {
    @NonNull String field;

    public A() {
      this.field = "field"; // valid
      field = "field"; // valid
      this.init(); // valid
      init(); // valid
    }

    public void init(@UnknownInitialization A this) {
      // :: error: (dereference.of.nullable)
      output(this.field.length());
    }

    public void initExpl2(@UnknownInitialization A this) {
      // :: error: (argument.type.incompatible)
      output(this.field);
    }

    public void initImpl1(@UnknownInitialization A this) {
      // :: error: (dereference.of.nullable)
      output(field.length());
    }

    public void initImpl2(@UnknownInitialization A this) {
      // :: error: (argument.type.incompatible)
      output(field);
    }
  }

  class B extends A {
    @NonNull String otherField;

    public B() {
      super();
      // :: error: (assignment.type.incompatible)
      this.otherField = null; // error
      this.otherField = "otherField"; // valid
    }

    @Override
    public void init(@UnknownInitialization B this) {
      // :: error: (dereference.of.nullable)
      output(this.field.length()); // error (TODO: substitution)
      super.init(); // valid
    }

    public void initImpl1(@UnknownInitialization B this) {
      // :: error: (dereference.of.nullable)
      output(field.length()); // error (TODO: substitution)
    }

    public void initExpl2(@UnknownInitialization B this) {
      // :: error: (dereference.of.nullable)
      output(this.otherField.length()); // error
    }

    public void initImpl2(@UnknownInitialization B this) {
      // :: error: (dereference.of.nullable)
      output(otherField.length()); // error
    }

    void other() {
      init(); // valid
      this.init(); // valid
    }

    void otherRaw(@UnknownInitialization B this) {
      init(); // valid
      this.init(); // valid
    }
  }

  class C extends B {

    @NonNull String[] strings;

    @Override
    public void init(@UnknownInitialization C this) {
      // :: error: (dereference.of.nullable)
      output(this.strings.length); // error
      System.out.println(); // valid
    }
  }

  // To test whether the argument is @NonNull and @Initialized
  static void output(@NonNull Object o) {}

  class D extends C {
    @Override
    public void init(@UnknownInitialization D this) {
      this.field = "s";
      output(this.field.length());
    }
  }

  class MyTest {
    int i;

    MyTest(int i) {
      this.i = i;
    }

    void myTest(@UnknownInitialization MyTest this) {
      i++;
    }
  }

  class AllFieldsInitialized {
    long elapsedMillis = 0;
    long startTime = 0;

    // If all fields have an initializer, then the type of "this"
    // should still not be non-raw (there might be uninitilized subclasses)
    public AllFieldsInitialized() {
      // :: error: (method.invocation.invalid)
      nonRawMethod();
    }

    public void nonRawMethod() {}
  }

  class AFSIICell {
    AllFieldsSetInInitializer afsii;
  }

  class AllFieldsSetInInitializer {
    long elapsedMillis;
    long startTime;

    public AllFieldsSetInInitializer() {
      elapsedMillis = 0;
      // :: error: (method.invocation.invalid)
      nonRawMethod();
      startTime = 0;
      // :: error: (method.invocation.invalid)
      nonRawMethod(); // still error (subclasses...)
    }

    public AllFieldsSetInInitializer(boolean b) {
      // :: error: (method.invocation.invalid)
      nonRawMethod();
    }

    public void nonRawMethod() {}
  }

  class ConstructorInvocations {
    int v;

    public ConstructorInvocations(int v) {
      this.v = v;
    }

    public ConstructorInvocations() {
      this(0);
      // :: error: (method.invocation.invalid)
      nonRawMethod();
    }

    public void nonRawMethod() {}
  }

  class MethodAccess {
    public MethodAccess() {
      @NonNull String s = string();
    }

    public @NonNull String string(@UnknownInitialization MethodAccess this) {
      return "nonnull";
    }
  }

  void cast(@UnknownInitialization Object... args) {

    @SuppressWarnings("rawtypes")
    // :: error: (assignment.type.incompatible)
    Object[] argsNonRaw1 = args;

    @SuppressWarnings("cast")
    Object[] argsNonRaw2 = (Object[]) args;
  }

  // default qualifier is @Nullable, so this is OK.
  class RawAfterConstructorBad {
    Object o;

    RawAfterConstructorBad() {}
  }

  class RawAfterConstructorOK1 {
    @Nullable Object o;

    RawAfterConstructorOK1() {}
  }

  class RawAfterConstructorOK2 {
    int a;

    RawAfterConstructorOK2() {}
  }
}
