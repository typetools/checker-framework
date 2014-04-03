package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * 
 * Represents a String object where the literal value of the String is known. If
 * there are multiple possibilities of what the literal value may be, these are
 * all included in the list of values.
 * 
 */
@TypeQualifier
@SubtypeOf({ UnknownVal.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface StringVal {
    String[] value();
}