package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * 
 * TODO Long or long value
 * 
 */
@TypeQualifier
@SubtypeOf({ UnknownVal.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface LongVal {
    long[] value();
}