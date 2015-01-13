package org.checkerframework.checker.javari;

import org.checkerframework.checker.javari.qual.Mutable;
import org.checkerframework.checker.javari.qual.PolyRead;
import org.checkerframework.checker.javari.qual.QReadOnly;
import org.checkerframework.checker.javari.qual.ReadOnly;
import org.checkerframework.checker.javari.qual.ThisMutable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * An annotation processor that checks a program's use of the Javari
 * type annotations ({@code @ReadOnly}, {@code @Mutable},
 * {@code @Assignable}, {@code @PolyRead} and {@code @QReadOnly}).
 *
 * @checker_framework.manual #javari-checker Javari Checker
 */
@TypeQualifiers( { ReadOnly.class, ThisMutable.class, Mutable.class,
    PolyRead.class, QReadOnly.class, PolyAll.class })
public class JavariChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
