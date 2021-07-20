import org.checkerframework.common.value.qual.MinLen;

import java.lang.reflect.Array;

public class ReflectArray {

    void testNewInstance(int i) {
        // :: error: (argument.type.incompatible)
        Array.newInstance(Object.class, i);
        if (i >= 0) {
            Array.newInstance(Object.class, i);
        }
    }

    void testFor(Object a) {
        for (int i = 0; i < Array.getLength(a); ++i) {
            Array.setInt(a, i, 1 + Array.getInt(a, i));
        }
    }

    void testMinLen(Object @MinLen(1) [] a) {
        Array.get(a, 0);
        // :: error: (argument.type.incompatible)
        Array.get(a, 1);
    }
}
