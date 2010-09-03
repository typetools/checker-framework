import checkers.interning.quals.*;

public class MethodInvocation {
    @Interned MethodInvocation interned;
    MethodInvocation nonInterned;
    void nonInternedMethod() {
        nonInternedMethod();
        //:: (method.invocation.invalid)
        internedMethod();   // should emit error

        this.nonInternedMethod();
        //:: (method.invocation.invalid)
        this.internedMethod();      // should emit error

        interned.nonInternedMethod();
        interned.internedMethod();

        nonInterned.nonInternedMethod();
        //:: (method.invocation.invalid)
        nonInterned.internedMethod();   // should emit error
    }

    void internedMethod() @Interned {
        nonInternedMethod();
        internedMethod();

        this.nonInternedMethod();
        this.internedMethod();

        interned.nonInternedMethod();
        interned.internedMethod();

        nonInterned.nonInternedMethod();
        //:: (method.invocation.invalid)
        nonInterned.internedMethod();   // should emit error
    }

  // Now, test method parameters
  void internedCharacterParameter(@Interned Character a) {
  }

  void internedCharacterParametersClient() {
    // TODO: auto-boxing from char to Character //:: (argument.type.incompatible)
    internedCharacterParameter('ä');
    // TODO: auto-boxing from char to Character //:: (argument.type.incompatible)
    internedCharacterParameter('a');
    //:: (argument.type.incompatible)
    internedCharacterParameter(new Character('a'));
    //:: (argument.type.incompatible)
    internedCharacterParameter(Character.valueOf('a'));
  }

}
