package org.checkerframework.framework.testchecker.lib;

import org.checkerframework.common.value.qual.StaticallyExecutable;

/** Used by framework/tests/value/VarArgRe.java */
public class VarArgMethods {
    @StaticallyExecutable
    public static int test0(Object... objects) {
        if (objects == null) {
            return -1;
        } else {
            return objects.length;
        }
    }

    @StaticallyExecutable
    public static int test1(String s, Object... objects) {
        if (objects == null) {
            return -1;
        } else {
            return objects.length;
        }
    }

    @StaticallyExecutable
    public static int test2(String s, String s2, Object... objects) {
        if (objects == null) {
            return -1;
        } else {
            return objects.length;
        }
    }
}
