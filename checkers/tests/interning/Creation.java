import checkers.interning.quals.*;

import java.util.*;

public class Creation {    @Interned Foo[] a = new @Interned Foo[22]; // valid

    class Foo {}

    @Interned Foo[] fa_field1 = new @Interned Foo[22]; // valid
    @Interned Foo[] fa_field2 = new @Interned Foo[22];  // valid

    public void test() {
        //:: (type.incompatible)
        @Interned Foo f = new Foo();            // error
        Foo g = new Foo();                      // valid
        @Interned Foo h = new @Interned Foo();  // valid
        //:: (not.interned)
        boolean b = (f == g);                   // error

        @Interned Foo[] fa1 = new @Interned Foo[22]; // valid
        @Interned Foo[] fa2 = new @Interned Foo[22];  // valid
    }

    public @Interned Object read_data_0() {
        //:: (type.incompatible)
        return new Object();
    }

    public @Interned Object read_data_1() {
        //:: (type.incompatible)
        return new Integer(22);
    }

    public @Interned Integer read_data_2() {
        //:: (type.incompatible)
        return new Integer(22);
    }

    public @Interned Object read_data_3() {
        //:: (type.incompatible)
        return new String("hello");
    }

    public @Interned String read_data_4() {
        //:: (type.incompatible)
        return new String("hello");
    }


}
