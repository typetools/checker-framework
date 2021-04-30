// Note that this file is a near duplicate in /nullness and /nullness-uninit

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class RawTypesInit {

  class Bad {
    @NonNull String field;

    public Bad() {
      // :: error: (method.invocation)
      this.init(); // error
      // :: error: (method.invocation)
      init(); // error

      this.field = "field"; // valid
      // :: error: (assignment)
      this.field = null; // error
      field = "field"; // valid
      // :: error: (assignment)
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
      // :: error: (argument)
      output(this.field);
    }

    public void initImpl1(@UnknownInitialization A this) {
      // :: error: (dereference.of.nullable)
      output(field.length());
    }

    public void initImpl2(@UnknownInitialization A this) {
      // :: error: (argument)
      output(field);
    }
  }

  class B extends A {
    @NonNull String otherField;

    public B() {
      super();
      // :: error: (assignment)
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

    // :: error: (initialization.field.uninitialized)
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
    Integer i;

    MyTest(int i) {
      this.i = i;
    }

    void myTest(@UnknownInitialization MyTest this) {
      // :: error: (unboxing.of.nullable)
      i = i + 1;
    }
  }

  class AllFieldsInitialized {
    Integer elapsedMillis = 0;
    Integer startTime = 0;

    public AllFieldsInitialized() {
      // :: error: (method.invocation)
      nonRawMethod();
    }

    public void nonRawMethod() {}
  }

  class AFSIICell {
    // :: error: (initialization.field.uninitialized)
    AllFieldsSetInInitializer afsii;
  }

  class AllFieldsSetInInitializer {
    Integer elapsedMillis;
    Integer startTime;

    public AllFieldsSetInInitializer() {
      elapsedMillis = 0;
      // :: error: (method.invocation)
      nonRawMethod(); // error
      startTime = 0;
      // :: error: (method.invocation)
      nonRawMethod(); // error
      // :: error: (initialization.field.write.initialized)
      new AFSIICell().afsii = this;
    }

    // :: error: (initialization.fields.uninitialized)
    public AllFieldsSetInInitializer(boolean b) {
      // :: error: (method.invocation)
      nonRawMethod(); // error
    }

    public void nonRawMethod() {}
  }

  class ConstructorInvocations {
    Integer v;

    public ConstructorInvocations(int v) {
      this.v = v;
    }

    public ConstructorInvocations() {
      this(0);
      // :: error: (method.invocation)
      nonRawMethod(); // invalid
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
    // :: error: (assignment)
    Object[] argsNonRaw1 = args;

    @SuppressWarnings("cast")
    Object[] argsNonRaw2 = (Object[]) args;
  }

  class RawAfterConstructorBad {
    Object o;
    // :: error: (initialization.fields.uninitialized)
    RawAfterConstructorBad() {}
  }

  class RawAfterConstructorOK1 {
    @Nullable Object o;

    RawAfterConstructorOK1() {}
  }

  class RawAfterConstructorOK2 {
    Integer a;
    // :: error: (initialization.fields.uninitialized)
    RawAfterConstructorOK2() {}
  }

  // TODO: reinstate.  This shows desired features, for initialization in
  // a helper method rather than in the constructor.
  class InitInHelperMethod {
    Integer a;
    Integer b;

    InitInHelperMethod(short constructor_inits_ab) {
      a = 1;
      b = 1;
      // :: error: (method.invocation)
      nonRawMethod();
    }

    InitInHelperMethod(boolean constructor_inits_a) {
      a = 1;
      init_b();
      // :: error: (method.invocation)
      nonRawMethod();
    }

    @RequiresNonNull("a")
    @EnsuresNonNull("b")
    void init_b(@UnknownInitialization InitInHelperMethod this) {
      b = 2;
      // :: error: (method.invocation)
      nonRawMethod();
    }

    InitInHelperMethod(int constructor_inits_none) {
      init_ab();
      // :: error: (method.invocation)
      nonRawMethod();
    }

    @EnsuresNonNull({"a", "b"})
    void init_ab(@UnknownInitialization InitInHelperMethod this) {
      a = 1;
      b = 2;
      // :: error: (method.invocation)
      nonRawMethod();
    }

    void nonRawMethod() {}
  }
}
