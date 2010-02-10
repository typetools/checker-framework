import checkers.interning.quals.*;

public class MethodInvocation {
    @Interned MethodInvocation interned;
    MethodInvocation nonInterned;
    void nonInternedMethod() {
        nonInternedMethod();
        internedMethod();   // should emit error

        this.nonInternedMethod();
        this.internedMethod();      // should emit error

        interned.nonInternedMethod();
        interned.internedMethod();

        nonInterned.nonInternedMethod();
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
        nonInterned.internedMethod();   // should emit error
    }

}
