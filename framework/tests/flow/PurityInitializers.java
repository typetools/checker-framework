import org.checkerframework.dataflow.qual.SideEffectFree;

// Tests that a constructor's purity is checked against the initializers that run as part of it:
// instance initializer blocks and instance field initializers.
public class PurityInitializers {

  static int counter = 0;

  static int bump() {
    return counter++;
  }

  @SideEffectFree
  static int pureValue() {
    return 0;
  }

  // The effects of a field initializer are effects of the constructor.
  static class FieldInitializer {
    // :: error: [purity.not.sideeffectfree.call]
    int x = bump();

    @SideEffectFree
    FieldInitializer() {}
  }

  // The effects of an instance initializer block are effects of the constructor.
  static class InitializerBlock {
    int x;

    {
      // :: error: [purity.not.sideeffectfree.call]
      bump();
    }

    @SideEffectFree
    InitializerBlock() {}
  }

  // The initializers run as part of each constructor, but each of their effects is one error, not
  // one error per constructor.
  static class TwoConstructors {
    // :: error: [purity.not.sideeffectfree.call]
    int x = bump();

    @SideEffectFree
    TwoConstructors() {}

    @SideEffectFree
    TwoConstructors(int i) {}
  }

  // A constructor may assign the fields of its own class, in an initializer as well as in the
  // constructor's body.
  static class AssignOwnField {
    int x;
    int y = 1;

    {
      x = 2;
    }

    @SideEffectFree
    AssignOwnField() {
      y = 3;
    }
  }

  // A static field is visible to other code even while an object is being constructed, so assigning
  // one is a side effect, in a constructor and in an instance initializer alike.
  static class AssignStaticField {
    static int s;

    {
      // :: error: [purity.not.sideeffectfree.assign.field]
      s = 1;
    }

    @SideEffectFree
    AssignStaticField() {
      // :: error: [purity.not.sideeffectfree.assign.field]
      s = 2;
    }
  }

  // Pure initializers do not make the constructor impure.
  static class PureInitializer {
    int x = pureValue();

    {
      x = pureValue();
    }

    @SideEffectFree
    PureInitializer() {}
  }

  // Static initializers do not run as part of a constructor, so they are not its effects.
  static class StaticInitializer {
    static int x = bump();

    static {
      bump();
    }

    @SideEffectFree
    StaticInitializer() {}
  }

  // A constructor that delegates via this(...) does not run the initializers a second time, so
  // their effects are reported only once, for the constructor that does run them.
  static class Delegating {
    // :: error: [purity.not.sideeffectfree.call]
    int x = bump();

    @SideEffectFree
    Delegating() {
      this(0);
    }

    @SideEffectFree
    Delegating(int i) {}
  }

  // An enum constant is a static field, so it is not an initializer of the enum's constructor.
  enum SomeEnum {
    A(bump()),
    B(1);

    final int x;

    // The error is for the implicit call to the superclass constructor `Enum(String, int)`, which
    // is not annotated; it is unrelated to the enum constants above.
    @SideEffectFree
    // :: error: [purity.not.sideeffectfree.call]
    SomeEnum(int i) {
      x = i;
    }
  }

  // The same holds for a local class.  Its initializers run when it is instantiated, so what
  // matters is the class member that encloses them, not the method that encloses the class.
  Object localClass() {
    class Local {
      int x;

      // :: error: [purity.not.sideeffectfree.call]
      int y = bump();

      {
        x = 1;
      }

      @SideEffectFree
      Local() {}
    }
    return new Local();
  }

  // The same holds for an anonymous class:  its initializers run when it is instantiated, so what
  // matters is the class member that encloses them.  The Purity Checker conservatively attributes
  // them to the method that contains the class declaration.
  @SideEffectFree
  Object anonymousClass() {
    // The error is for the call to the superclass constructor `Object()`, which is not annotated;
    // it is unrelated to the initializers below.
    // :: error: [purity.not.sideeffectfree.call]
    return new Object() {
      int x;

      // :: error: [purity.not.sideeffectfree.call]
      int y = bump();

      {
        // No error:  assigning a field of the object that is being constructed.
        x = 1;
      }
    };
  }

  // A static initializer runs at class initialization rather than during construction, so assigning
  // a static field in one is a side effect even though the field belongs to the same class.
  @SideEffectFree
  Object staticInitializerAssignment() {
    class Local {
      static int s;

      static {
        // :: error: [purity.not.sideeffectfree.assign.field]
        s = 1;
      }

      @SideEffectFree
      Local() {}
    }
    return new Local();
  }
}
