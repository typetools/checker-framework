import checkers.nullness.quals.*;

public class Synchronization {

    // Plain
    public void bad() {
        Object o = null;
        synchronized(o) { }   // should emit error
    }

    public void ok() {
        // NonNull specifically
        @NonNull Object o1 = "m";
        synchronized(o1) { }
    }

    public void flow() {
        Object o = null;
        o = "m";
        synchronized(o) { }; // valid
        o = null;
        synchronized(o) { }; // invalid
    }

    public Synchronization() {
        synchronized(this) {
        }
    }

}
