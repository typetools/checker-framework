package org.checkerframework.framework.testchecker.testaccumulation.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/** A test accumulation analysis qualifier. It accumulates generic strings. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface TestAccumulation {
    /**
     * Accumulated strings.
     *
     * @return the strings
     */
    public String[] value() default {};
}
