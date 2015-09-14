import org.checkerframework.checker.lock.qual.*;

// Initializers and constructors are synchronized over 'this'
// and over all their class's non-static fields
public class Constructors {

    static class MyClass { public Object field; }

    MyClass unlocked;
    @GuardedBy("this") MyClass guardedThis = new MyClass();
    @GuardedBy("unlocked") MyClass guardedOther = new MyClass();
    static MyClass unlockedStatic;
    @GuardedBy("unlockedStatic") MyClass nonstaticGuardedByStatic = new MyClass();
    static @GuardedBy("unlocked") MyClass staticGuardedByNonStatic = new MyClass();
    static @GuardedBy("unlockedStatic") MyClass staticGuardedByStatic = new MyClass();

    Object initializedObject1 = unlocked.field;
    Object initializedObject2 = guardedThis.field;
    Object initializedObject3 = guardedOther.field;
    Object initializedObject4 = staticGuardedByNonStatic.field;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject5 = nonstaticGuardedByStatic.field;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject6 = staticGuardedByStatic.field;

    Constructors() {
        unlocked.field.toString();
        guardedThis.field.toString();
        guardedOther.field.toString();
        staticGuardedByNonStatic.field.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        nonstaticGuardedByStatic.field.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        staticGuardedByStatic.field.toString();
    }
}
