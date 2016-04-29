import org.checkerframework.checker.lock.qual.*;

// Initializers and constructors are synchronized over 'this'
// but not over their class's fields
public @GuardedBy({})  class Constructors {

    static class MyClass { public Object field; }

    final MyClass unlocked = new MyClass();
    @GuardedBy("this") MyClass guardedThis = new MyClass();
    @GuardedBy("unlocked") MyClass guardedOther = new MyClass();
    final static MyClass unlockedStatic = new MyClass();
    @GuardedBy("unlockedStatic") MyClass nonstaticGuardedByStatic = new MyClass();
    static @GuardedBy("unlocked") MyClass staticGuardedByNonStatic = new MyClass();
    static @GuardedBy("unlockedStatic") MyClass staticGuardedByStatic = new MyClass();

    Object initializedObject1 = unlocked.field;
    Object initializedObject2 = guardedThis.field;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject3 = guardedOther.field;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject4 = staticGuardedByNonStatic.field;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject5 = nonstaticGuardedByStatic.field;
    //:: error: (contracts.precondition.not.satisfied.field)
    Object initializedObject6 = staticGuardedByStatic.field;

    Constructors() {
        unlocked.field.toString();
        guardedThis.field.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        guardedOther.field.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        staticGuardedByNonStatic.field.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        nonstaticGuardedByStatic.field.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        staticGuardedByStatic.field.toString();
    }
}
