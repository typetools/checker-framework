package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.UpperBoundFor;

/**
 * The value is to be interpreted as unsigned. That is, if the most significant bit in the bitwise
 * representation is set, then the bits should be interpreted as a large positive number rather than
 * as a negative number.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownSignedness.class})
@DefaultFor(
        typeKinds = {TypeKind.CHAR},
        types = {java.lang.Character.class})
@UpperBoundFor(
        typeKinds = {TypeKind.CHAR},
        types = {java.lang.Character.class})
public @interface Unsigned {}
