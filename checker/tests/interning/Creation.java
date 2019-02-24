import org.checkerframework.checker.interning.qual.Interned;

public class Creation {
    @Interned Foo[] a = new @Interned Foo[22]; // valid

    class Foo {}

    @Interned Foo[] fa_field1 = new @Interned Foo[22]; // valid
    @Interned Foo[] fa_field2 = new @Interned Foo[22]; // valid

    public void test() {
        // :: error: (assignment.type.incompatible)
        @Interned Foo f = new Foo(); // error
        Foo g = new Foo(); // valid
        // :: warning: (cast.unsafe.constructor.invocation)
        @Interned Foo h = new @Interned Foo(); // valid
        // :: error: (not.interned)
        boolean b = (f == g); // error

        @Interned Foo[] fa1 = new @Interned Foo[22]; // valid
        @Interned Foo[] fa2 = new @Interned Foo[22]; // valid
    }

    public @Interned Object read_data_0() {
        // :: error: (return.type.incompatible)
        return new Object();
    }

    public @Interned Object read_data_1() {
        // :: error: (return.type.incompatible)
        return new Integer(22);
    }

    public @Interned Integer read_data_2() {
        // :: error: (return.type.incompatible)
        return new Integer(22);
    }

    public @Interned Object read_data_3() {
        // :: error: (return.type.incompatible)
        return new String("hello");
    }

    public @Interned String read_data_4() {
        // :: error: (return.type.incompatible)
        return new String("hello");
    }
}
