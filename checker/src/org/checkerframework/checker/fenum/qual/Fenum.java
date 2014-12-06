package org.checkerframework.checker.fenum.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * A generic fake enumeration qualifier that is parameterized by a name.
 * It is written in source code as, for example,
 * <tt>@Fenum("cardSuit")</tt> and <tt>@Fenum("faceValue")</tt>, which
 * would be distinct fake enumerations.
 *
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(FenumTop.class)
public @interface Fenum {
    String value();
}
