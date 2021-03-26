import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import org.checkerframework.checker.interning.qual.InternMethod;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.UnknownInterned;

// The @Interned annotation indicates that much like an enum, all variables
// declared of this type are interned (except the constructor return value).
public @Interned class InternedClass {

  int value;

  InternedClass factory(int i) {
    return new InternedClass(i).intern();
  }

  // Private constructor
  private InternedClass(int i) {
    value = i;
    // "this" in the constructor is not interned.
    // :: error: (assignment.type.incompatible)
    @Interned InternedClass that = this;
  }

  // Overriding method
  @org.checkerframework.dataflow.qual.Pure
  public String toString() {
    @Interned InternedClass c = this;
    return Integer.valueOf(value).toString();
  }

  // Factory method
  private InternedClass(InternedClass ic) {
    value = ic.value;
  }

  // Equals method (used only by interning; clients should use ==)
  @org.checkerframework.dataflow.qual.Pure
  public boolean equals(Object other) {
    if (!(other instanceof InternedClass)) {
      return false;
    }
    return value == ((InternedClass) other).value;
  }

  // Interning method
  @SuppressWarnings("type.invalid.annotations.on.use")
  private static Map<@UnknownInterned InternedClass, @Interned InternedClass> pool =
      new HashMap<>();

  @InternMethod
  public @Interned InternedClass intern() {
    if (!pool.containsKey(this)) {
      pool.put(this, (@Interned InternedClass) this);
    }
    return pool.get(this);
  }

  public void myMethod(InternedClass ic, InternedClass[] ica) {
    boolean b1 = (this == ic); // valid
    boolean b2 = (this == returnInternedObject()); // valid
    boolean b3 = (this == ica[0]); // valid
    InternedClass ic2 = returnArray()[0]; // valid
    // :: error: (interned.object.creation)
    ica[0] = new InternedClass(22);
    InternedClass[] arr1 = returnArray(); // valid
    InternedClass[] arr2 = new InternedClass[22]; // valid
    InternedClass[] arr3 = new InternedClass[] {}; // valid

    Map<InternedClass, Integer> map = new LinkedHashMap<>();
    for (Map.Entry<InternedClass, Integer> e : map.entrySet()) {
      InternedClass ic3 = e.getKey(); // valid
    }
  }

  public InternedClass returnInternedObject() {
    return this;
  }

  public InternedClass[] returnArray() {
    return new InternedClass[] {};
  }

  public void internedVarargs(String name, InternedClass... args) {
    InternedClass arg = args[0]; // valid
  }

  public void internedVarargs2(String name, @Interned String... args) {
    @Interned String arg = args[0]; // valid
  }

  public static InternedClass[] arrayclone_simple(InternedClass[] a_old) {
    int len = a_old.length;
    InternedClass[] a_new = new InternedClass[len];
    for (int i = 0; i < len; i++) {
      // :: error: (interned.object.creation)
      a_new[i] = new InternedClass(a_old[i]);
    }
    return a_new;
  }

  public @Interned class Subclass extends InternedClass {
    // Private constructor
    private Subclass(int i) {
      super(i);
    }
  }

  public static void castFromInternedClass(InternedClass ic) {
    Subclass s = (Subclass) ic;
  }

  public static void castToInternedClass(Object o) {
    InternedClass ic = (InternedClass) o;
  }

  // Default implementation
  @org.checkerframework.dataflow.qual.Pure
  public InternedClass clone() throws CloneNotSupportedException {
    return (InternedClass) super.clone();
  }

  // java.lang.Class should be considered interned
  public static void classTest() {
    Integer i = 5;
    assert i.getClass() == Integer.class;
  }

  // java.lang.Class is interned
  public static void arrayOfClass() throws Exception {
    Class<?> c = String.class;
    Class[] parameterTypes = new Class[1];
    parameterTypes[0] = String.class;
    java.lang.reflect.Constructor<?> ctor = c.getConstructor(parameterTypes);
  }

  Class[] getSuperClasses(Class<?> c) {
    Vector<Class<?>> v = new Vector<>();
    while (true) {
      // :: warning: (unnecessary.equals)
      if (c.getSuperclass().equals((new Object()).getClass())) {
        break;
      }
      c = c.getSuperclass();
      v.addElement(c);
    }
    return (Class[]) v.toArray(new Class[0]);
  }

  void testCast(Object o) {
    Object i = (InternedClass) o;
    if (i == this) {}
  }
}
