package org.checkerframework.checker.optional.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom type qualifier for the Optional Checker. The only value of this type is {@code null}.
 * Programmers rarely write this annotation.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@RelevantJavaTypes(Optional.class)
@SubtypeOf({Present.class})
public @interface OptionalBottom {}
