// Test case for issue #1214:
// https://github.com/typetools/checker-framework/issues/1214

import org.checkerframework.common.value.qual.*;

public class Issue1214 {

    static void noException() {
        int n = 0;
        try {
        } catch (Exception e) {
            n = 1;
        }
        @IntVal(0) int ok = n;
    }

    static void arrayAccess(String[] array) {
        int n = 0;
        try {
            String s = array[0];
        } catch (NullPointerException e) {
            n = 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            n = 2;
        } catch (Exception e) {
            n = 3;
        }
        @IntVal({0, 1, 2}) int ok = n;
        //:: error: (assignment.type.incompatible)
        @IntVal({0, 1}) int ng1 = n;
        //:: error: (assignment.type.incompatible)
        @IntVal({0, 2}) int ng2 = n;
        //:: error: (assignment.type.incompatible)
        @IntVal(0) int ng3 = n;
    }

    static void forArray(String[] array) {
        int n = 0;
        try {
            for (String s : array) ;
        } catch (NullPointerException e) {
            n = 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            n = 2;
        } catch (Exception e) {
            n = 3;
        }
        @IntVal({0, 1}) int ok = n;
        //:: error: (assignment.type.incompatible)
        @IntVal(0) int ng = n;
    }

    static void forIterable(Iterable<String> itr) {
        int n = 0;
        try {
            for (String s : itr) ;
        } catch (NullPointerException e) {
            n = 1;
        } catch (Exception e) {
            n = 2;
        }
        @IntVal({0, 1, 2}) int ok = n;
        //:: error: (assignment.type.incompatible)
        @IntVal({0, 1}) int ng1 = n;
        //:: error: (assignment.type.incompatible)
        @IntVal({0, 2}) int ng2 = n;
        //:: error: (assignment.type.incompatible)
        @IntVal(0) int ng3 = n;
    }
}
