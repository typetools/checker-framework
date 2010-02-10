import checkers.nullness.quals.*;

public class Exceptions {
    void exceptionParam(@Nullable Exception m) {
        //:: (dereference.of.nullable)
        m.getClass();   // should emit error
    }

    void nonnullExceptionParam(@NonNull Exception m) {
        m.getClass();
    }

    void exception(@Nullable Exception m) {
        try {

        } catch (Exception e) {
            e.getClass();
            //:: (dereference.of.nullable)
            m.getClass();   // should emit error
        }
    }

    void throwException() {
        int a = 0;
        if (a == 0)
            //:: (throwing.nullable)
            throw null;
        else if (a == 1) {
            RuntimeException e = null;
            //:: (throwing.nullable)
            throw e;
        } else {
            RuntimeException e = new RuntimeException();
            throw e;
        }
    }

    void reassignException() {
        try {
        } catch (RuntimeException e) {
            //:: (type.incompatible)
            e = null;
            throw e;
        }
    }
}
