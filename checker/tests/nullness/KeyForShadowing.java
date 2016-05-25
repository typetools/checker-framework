// @skip-test

// Test for Checker Framework issue 273:
// https://github.com/typetools/checker-framework/issues/273

import org.checkerframework.checker.nullness.qual.*;
import java.util.Map;
import java.util.HashMap;

class KeyForShadowing {
    public static void main(String... p) {
        Map<String, Integer> m0 = new HashMap<String,Integer>();
        Map<String, Integer> m1 = new HashMap<String,Integer>();
        String k = "key";
        m0.put(k, 1); // k is @KeyFor("m0") after this line

        // We expect an error for the next one since we are not
        // respecting the method contract. It expects the
        // key to be for the second parameter, not the first.

        //:: error: (argument.type.incompatible)
        getMap3(m0,m1,k).toString();

        // We expect an error for the next one since although
        // we are respecting the method contract, since the
        // key is for the first parameter, the Nullness Checker
        // is misinterpreting "m1" to be the local m1 to this
        // method, and not the first parameter to the method.

        //:: error: (argument.type.incompatible)
        getMap2(m0,m1,k).toString();

        getMap1(m0,m1,k).toString();
        getMap4(m0,m1,k).toString();
    }

    //:: warning: (method.declaration.expression.parameter.name)
    public static @NonNull Integer getMap1(
            Map<String, Integer> m1,  // m1,m0 flipped
            Map<String, Integer> m0,
            @KeyFor("m0") String k)
    {
        return m0.get(k);
    }

    //:: warning: (method.declaration.expression.parameter.name)
    public static @NonNull Integer getMap2(
            Map<String, Integer> m1,  // m1,m0 flipped
            Map<String, Integer> m0,
            @KeyFor("m1") String k)
    {
        // This method body is incorrect.
        // We expect this error because we are indicating that
        // the key is for m1, so m0.get(k) is @Nullable.
        //:: error: (return.type.incompatible)
        return m0.get(k);
    }

    public static @NonNull Integer getMap3(
            Map<String, Integer> m1,  // m1,m0 flipped
            Map<String, Integer> m0,
            @KeyFor("#2") String k)
    {
        return m0.get(k);
    }

    public static @NonNull Integer getMap4(
            Map<String, Integer> m1,  // m1,m0 flipped
            Map<String, Integer> m0,
            @KeyFor("#1") String k)
    {
        // This method body is incorrect.
        // We expect this error because we are indicating that
        // the key is for m1, so m0.get(k) is @Nullable.
        //:: error: (return.type.incompatible)
        return m0.get(k);
    }

    public static void localVariableShadowing() {
        @KeyFor("m0") String kk;
        {
            Map<String, Integer> m0 = new HashMap<String,Integer>();
            @KeyFor("m0") String k = "key";
            kk = k;
        }
        {
            Map<String, Integer> m0 = new HashMap<String,Integer>();
            //:: error: (assignment.type.incompatible)
            @KeyFor("m0") String k2 = kk;
        }
    }
}
