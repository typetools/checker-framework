package org.checkerframework.checker.units.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * A polymorphic qualifier for the Rawness type system.
 *
 * <p>
 * Any method written using @PolyRaw conceptually has two versions:  one
 * in which every instance of @PolyRaw has been replaced by @Raw, and
 * one in which every instance of @PolyRaw has been replaced by @NonRaw.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@TypeQualifier
@PolymorphicQualifier(UnknownUnits.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyUnit { }
