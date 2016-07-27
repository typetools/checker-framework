package org.checkerframework.checker.upperbound.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.upperbound.qual.*;

import org.checkerframework.framework.qual.*;

/**
 *  Indicates that whatever is annotated with this type is less than the
 *  length of the listed arrays
 *
 *  */
@SubtypeOf(UpperBoundUnknown.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LessThanLength {
    public String[] value();
}
