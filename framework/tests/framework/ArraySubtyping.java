import org.checkerframework.framework.testchecker.util.*;

// @skip-test
public class ArraySubtyping {
    Object[] obj1 = new Object[1];
    @Odd Object[] obj2 = new @Odd Object[1];

    String[] str1 = new String[1];
    @Odd String[] str2 = new @Odd String[1];

    void m() {
        // :: error: (assignment.type.incompatible)
        obj1 = obj2;
        // :: error: (assignment.type.incompatible)
        obj2 = obj1;

        // :: error: (assignment.type.incompatible)
        str1 = str2;
        // :: error: (assignment.type.incompatible)
        str2 = str1;

        obj1 = str1;
        obj2 = str2;

        // :: error: (assignment.type.incompatible)
        obj1 = str2;
        // :: error: (assignment.type.incompatible)
        obj2 = str1;
    }
}
