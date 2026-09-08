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
}
