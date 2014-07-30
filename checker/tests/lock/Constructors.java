import org.checkerframework.checker.lock.qual.*;

// Initializers and constructors are synchronized over 'this'
// and over all their class's non-static fields
public class Constructors {

    Object unlocked;
    @GuardedBy("this") Object guardedThis = new Object();
    @GuardedBy("unlocked") Object guardedOther = new Object();
    static Object unlockedStatic;
    @GuardedBy("unlockedStatic") Object nonstaticGuardedByStatic = new Object();
    static @GuardedBy("unlocked") Object staticGuardedByNonStatic = new Object();
    static @GuardedBy("unlockedStatic") Object staticGuardedByStatic = new Object();

    Object initializedObject1 = unlocked;
    Object initializedObject2 = guardedThis;
    Object initializedObject3 = guardedOther;
    Object initializedObject4 = staticGuardedByNonStatic;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject5 = nonstaticGuardedByStatic;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject6 = staticGuardedByStatic;

    Constructors() {
        unlocked = "m";
        guardedThis = "m";
        guardedOther = "m";
        staticGuardedByNonStatic = "m";
        //:: error: (contracts.precondition.not.satisfied.field)
        nonstaticGuardedByStatic = "m";
        //:: error: (contracts.precondition.not.satisfied.field)
        staticGuardedByStatic = "m";
    }
}
