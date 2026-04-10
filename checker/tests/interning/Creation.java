import org.checkerframework.checker.interning.qual.Interned;

public class Creation {
  @Interned Foo[] a = new @Interned Foo[22]; // valid

  class Foo {}

  @Interned Foo[] fa_field1 = new @Interned Foo[22]; // valid
  @Interned Foo[] fa_field2 = new @Interned Foo[22]; // valid

  public void test() {
    @Interned Foo f = new Foo(); // valid: new Foo() is @InternedDistinct
    Foo g = new Foo(); // valid
    // :: warning: [cast.unsafe.constructor.invocation]
    @Interned Foo h = new @Interned Foo(); // valid
    boolean b = (f == g); // valid: both are @InternedDistinct

    @Interned Foo[] fa1 = new @Interned Foo[22]; // valid
    @Interned Foo[] fa2 = new @Interned Foo[22]; // valid
  }

  @Interned Foo f2 = new Foo();
  Foo g2 = new Foo();
  // :: warning: [cast.unsafe.constructor.invocation]
  @Interned Foo h2 = new @Interned Foo();

  public void test2() {
    // :: error: [not.interned]
    boolean b = (f2 == g2);

    @Interned Foo[] fa1 = new @Interned Foo[22]; // valid
    @Interned Foo[] fa2 = new @Interned Foo[22]; // valid
  }

  public @Interned Object read_data_0() {
    return new Object();
  }

  public @Interned Object read_data_0(Object o) {
    // :: error: [return]
    return o;
  }

  public @Interned Object read_data_1() {
    // :: error: [return]
    return Integer.valueOf(22);
  }

  public @Interned Integer read_data_2() {
    // :: error: [return]
    return Integer.valueOf(22);
  }

  public @Interned Object read_data_3() {
    return new String("hello"); // valid: new String() is @InternedDistinct
  }

  public @Interned Object read_data_3(String s) {
    // :: error: [return]
    return s;
  }

  public @Interned String read_data_4() {
    return new String("hello"); // valid: new String() is @InternedDistinct
  }

  public @Interned String read_data_4(String s) {
    // :: error: [return]
    return s;
  }
}
