package org.checkerframework.checker.fenum.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A generic fake enumeration qualifier that is parameterized by a name. It is written in source
 * code as, for example, {@code @Fenum("cardSuit")} and {@code @Fenum("faceValue")}, which would be
 * distinct fake enumerations.
 *
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
public @interface Fenum {
    String value();
}
