package org.checkerframework.common.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * This represents a Class&lt;T&gt; object where the exact value of T is known. This
 * is a list of values, so that if the object could have multiple different
 * exact values each is represented
 */
@TypeQualifier
@SubtypeOf({ UnknownClass.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE })
public @interface ClassVal {
    String[] value();
}