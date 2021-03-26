import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.interning.qual.InternMethod;
import org.checkerframework.checker.interning.qual.Interned;

public @Interned class InternedClass2 {
  private final int i;
  // @UnknownInterned is the default annotation on constructor results even for @Interned classes.
  private InternedClass2(int i) {
    // Type of "this" inside a constructor of an @Interned class is @UnknownInterned.
    // :: error: (assignment.type.incompatible)
    @Interned InternedClass2 that = this;
    this.i = i;
  }

  InternedClass2 factory(int i) {
    // :: error: (interned.object.creation) :: error: (method.invocation.invalid)
    new InternedClass2(i).someMethod(); // error, call to constructor on for @Interned class.
    (new InternedClass2(i)).intern(); // ok, call to constructor receiver to @InternMethod
    ((((new InternedClass2(i))))).intern(); // ok, call to constructor receiver to @InternMethod
    return new InternedClass2(i).intern(); // ok, call to constructor receiver to @InternMethod
  }

  void someMethod() {
    // Type of "this" inside a method (not marked @InternedMethod) is @Interned,
    // assuming the method is declared in an @Interned class.
    @Interned InternedClass2 that = this; // ok
  }

  private static Map<Integer, InternedClass2> pool = new HashMap<>();

  @InternMethod
  public InternedClass2 intern() {
    // Type of "this" inside an @InternMethod is @UnknownInterned
    // :: error: (assignment.type.incompatible)
    @Interned InternedClass2 that = this;
    if (!pool.containsKey(this.i)) {
      // The above check proves "this" is interned.
      @SuppressWarnings("interning:assignment.type.incompatible")
      @Interned InternedClass2 internedThis = this;
      pool.put(this.i, internedThis);
    }
    return pool.get(this.i);
  }

  @Override // ok, no override invalid receiver error is issued.
  public String toString() {
    @Interned InternedClass2 that = this; // ok
    return super.toString();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    InternedClass2 that = (InternedClass2) object;

    return i == that.i;
  }

  @Override
  public int hashCode() {
    return i;
  }

  public boolean hasNodeOfType(Class<?> type) {
    if (type == this.getClass()) {
      return true;
    }
    return false;
  }
}
