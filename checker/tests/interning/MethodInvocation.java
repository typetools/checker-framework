import org.checkerframework.checker.interning.qual.*;

public class MethodInvocation {
  @Interned MethodInvocation interned;
  MethodInvocation nonInterned;

  void nonInternedMethod() {
    nonInternedMethod();
    // :: error: (method.invocation)
    internedMethod(); // should emit error

    this.nonInternedMethod();
    // :: error: (method.invocation)
    this.internedMethod(); // should emit error

    interned.nonInternedMethod();
    interned.internedMethod();

    nonInterned.nonInternedMethod();
    // :: error: (method.invocation)
    nonInterned.internedMethod(); // should emit error
  }

  void internedMethod(@Interned MethodInvocation this) {
    nonInternedMethod();
    internedMethod();

    this.nonInternedMethod();
    this.internedMethod();

    interned.nonInternedMethod();
    interned.internedMethod();

    nonInterned.nonInternedMethod();
    // :: error: (method.invocation)
    nonInterned.internedMethod(); // should emit error
  }

  // Now, test method parameters
  void internedCharacterParameter(@Interned Character a) {}

  // See https://github.com/typetools/checker-framework/issues/84
  void internedCharacterParametersClient() {
    // TODO: autoboxing from char to Character // :: error: (argument)
    internedCharacterParameter('\u00E4'); // lowercase a with umlaut
    // TODO: autoboxing from char to Character // :: error: (argument)
    internedCharacterParameter('a');
    // :: error: (argument)
    internedCharacterParameter(Character.valueOf('a'));
    // :: error: (argument)
    internedCharacterParameter(Character.valueOf('a'));
  }
}
