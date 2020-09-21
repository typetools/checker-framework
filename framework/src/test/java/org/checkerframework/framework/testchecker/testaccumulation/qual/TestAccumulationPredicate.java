package org.checkerframework.framework.testchecker.testaccumulation.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/** A test accumulation predicate annotation. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({TestAccumulation.class})
public @interface TestAccumulationPredicate {
    String value();
}
