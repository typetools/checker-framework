package org.checkerframework.checker.propkey.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom qualifier for the PropertyKeyChecker and associated checkers.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(
    typeNames = {java.lang.Void.class},
    literals = {LiteralKind.NULL}
)
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface PropertyKeyBottom {}
