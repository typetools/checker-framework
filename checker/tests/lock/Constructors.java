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

    Object initializedObject1 = unlocked.toString();
    Object initializedObject2 = guardedThis.toString();
    Object initializedObject3 = guardedOther.toString();
    Object initializedObject4 = staticGuardedByNonStatic.toString();
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject5 = nonstaticGuardedByStatic.toString();
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject6 = staticGuardedByStatic.toString();

    Constructors() {
        unlocked.toString();
        guardedThis.toString();
        guardedOther.toString();
        staticGuardedByNonStatic.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        nonstaticGuardedByStatic.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        staticGuardedByStatic.toString();
    }
}
