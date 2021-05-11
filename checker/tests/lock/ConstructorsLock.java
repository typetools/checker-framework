import org.checkerframework.checker.lock.qual.*;

// Initializers and constructors are synchronized over 'this'
// but not over their class's fields
public @GuardedBy({}) class ConstructorsLock {

  static class MyClass {
    public Object field;
  }

  final MyClass unlocked = new MyClass();

  @GuardedBy("this") MyClass guardedThis = new MyClass();

  @GuardedBy("unlocked") MyClass guardedOther = new MyClass();

  static final MyClass unlockedStatic = new MyClass();

  @GuardedBy("unlockedStatic") MyClass nonstaticGuardedByStatic = new MyClass();
  // :: error: (expression.unparsable)
  static @GuardedBy("unlocked") MyClass staticGuardedByNonStatic = new MyClass();
  static @GuardedBy("unlockedStatic") MyClass staticGuardedByStatic = new MyClass();

  Object initializedObject1 = unlocked.field;
  Object initializedObject2 = guardedThis.field;
  // :: error: (lock.not.held)
  Object initializedObject3 = guardedOther.field;
  // :: error: (expression.unparsable)
  Object initializedObject4 = staticGuardedByNonStatic.field;
  // :: error: (lock.not.held)
  Object initializedObject5 = nonstaticGuardedByStatic.field;
  // :: error: (lock.not.held)
  Object initializedObject6 = staticGuardedByStatic.field;

  ConstructorsLock() {
    unlocked.field.toString();
    guardedThis.field.toString();
    // :: error: (lock.not.held)
    guardedOther.field.toString();
    // :: error: (expression.unparsable)
    staticGuardedByNonStatic.field.toString();
    // :: error: (lock.not.held)
    nonstaticGuardedByStatic.field.toString();
    // :: error: (lock.not.held)
    staticGuardedByStatic.field.toString();
  }
}
