package org.checkerframework.checker.javari;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * An annotation processor that checks a program's use of the Javari
 * type annotations ({@code @ReadOnly}, {@code @Mutable},
 * {@code @Assignable}, {@code @PolyRead} and {@code @QReadOnly}).
 *
 * @checker_framework.manual #javari-checker Javari Checker
 */
public class JavariChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
