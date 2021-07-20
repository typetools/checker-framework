import org.checkerframework.checker.nullness.qual.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// interface Dest<DA,DB,DC,DD,DE> {
// }
//
// interface Inter1<I1A,I1B,I1C,I1D,I1E> extends Dest<I1A, I1A, I1C, I1D, String> {}
//
// interface Inter2<I2A,I2B,I2C,I2D,I2E> extends Dest<I2D,I2E,I2C,I2D,I2E> {}
//
// class Source<SA,SB,SC,SD,SE> extends HashMap<SA,SB> implements Inter2<SA,SB,SB,SD,SE> {}

public class KeyForPropagation {

    {
        List<@KeyFor("a") String> a = new ArrayList<String>();
    }

    static {
        List<@KeyFor("b") String> b = new ArrayList<String>();
    }

    List<@KeyFor("c") String> c = new ArrayList<String>();

    void method() {
        List<@KeyFor("d") String> d = new ArrayList<String>();
    }

    void method(Map<String, String> v) {
        Set<String> ks = v.keySet();
    }
}
