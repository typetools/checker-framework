import checkers.lock.quals.*;

public class Constructors {

    Object unlocked;
    @GuardedBy("this") Object guardedThis;
    @GuardedBy("unlocked") Object guardedOther;

    // Constructors are synchronized over this
    Constructors() {
        unlocked = "m";
        guardedThis = "m";
        //:: error: (unguarded.access)
        guardedOther = "m"; //error
    }
}
