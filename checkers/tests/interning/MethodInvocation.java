import checkers.interning.quals.*;

public class MethodInvocation {
    @Interned MethodInvocation interned;
    MethodInvocation nonInterned;
    void nonInternedMethod() {
        nonInternedMethod();
        //:: error: (method.invocation.invalid)
        internedMethod();   // should emit error

        this.nonInternedMethod();
        //:: error: (method.invocation.invalid)
        this.internedMethod();      // should emit error

        interned.nonInternedMethod();
        interned.internedMethod();

        nonInterned.nonInternedMethod();
        //:: error: (method.invocation.invalid)
        nonInterned.internedMethod();   // should emit error
    }

    void internedMethod(@Interned MethodInvocation this) {
        nonInternedMethod();
        internedMethod();

        this.nonInternedMethod();
        this.internedMethod();

        interned.nonInternedMethod();
        interned.internedMethod();

        nonInterned.nonInternedMethod();
        //:: error: (method.invocation.invalid)
        nonInterned.internedMethod();   // should emit error
    }

    // Now, test method parameters
    void internedCharacterParameter(@Interned Character a) {
    }

    // See http://code.google.com/p/checker-framework/issues/detail?id=84
    void internedCharacterParametersClient() {
        // TODO: auto-boxing from char to Character //:: error: (argument.type.incompatible)
        internedCharacterParameter('\u00E4'); // lowercase a with umlaut
        // TODO: auto-boxing from char to Character //:: error: (argument.type.incompatible)
        internedCharacterParameter('a');
        //:: error: (argument.type.incompatible)
        internedCharacterParameter(new Character('a'));
        //:: error: (argument.type.incompatible)
        internedCharacterParameter(Character.valueOf('a'));
    }

}
