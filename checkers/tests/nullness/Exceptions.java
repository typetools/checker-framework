import checkers.nullness.quals.*;

public class Exceptions {
    void exceptionParam(@Nullable Exception m) {
        m.getClass();   // should emit error
    }

    void nonnullExceptionParam(@NonNull Exception m) {
        m.getClass();
    }

    void exception(@Nullable Exception m) {
        try {

        } catch (Exception e) {
            e.getClass();
            m.getClass();   // should emit error
        }
    }

    void throwException() {
        int a = 0;
        if (a == 0)
            throw null;
        else if (a == 1) {
            RuntimeException e = null;
            throw e;
        } else {
            RuntimeException e = new RuntimeException();
            throw e;
        }
    }

    void reassignException() {
        try {
        } catch (RuntimeException e) {
            e = null;
            throw e;
        }
    }
}
