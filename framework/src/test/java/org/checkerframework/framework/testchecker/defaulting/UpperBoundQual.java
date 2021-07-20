package org.checkerframework.framework.testchecker.defaulting;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Created by jburke on 9/29/14. */
public class UpperBoundQual {

    @DefaultQualifierInHierarchy
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({})
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UbTop {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(UbTop.class)
    @DefaultFor(TypeUseLocation.IMPLICIT_UPPER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UbImplicit {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(UbTop.class)
    @DefaultFor(TypeUseLocation.EXPLICIT_UPPER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UbExplicit {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({UbImplicit.class, UbExplicit.class})
    @DefaultFor(TypeUseLocation.LOWER_BOUND)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public static @interface UbBottom {}
}
