package org.checkerframework.checker.fenum.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The bottom qualifier for fenums, its relationships are setup via the
 * FenumAnnotatedTypeFactory.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@Documented
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@SubtypeOf({}) //subtype relationships are set up by passing this class as a bottom
               //to the multigraph hierarchy constructor
@Retention(RetentionPolicy.RUNTIME)
@ImplicitFor(literals = { LiteralKind.NULL },
             typeNames = {java.lang.Void.class})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface FenumBottom {}
