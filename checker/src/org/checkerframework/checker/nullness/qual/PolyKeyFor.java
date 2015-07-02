package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.framework.qual.*;

/**
 * A polymorphic qualifier for the Map Key (@KeyFor) type system.
 *
 * <p>
 * Any method written using {@code @PolyKeyFor} conceptually has an
 * arbitrary number of versions:  one in which every instance of
 * {@code @PolyKeyFor} has been replaced by {@code @}{@link UnknownKeyFor},
 * one in which every instance of {@code @}{@link PolyKeyFor} has been
 * replaced by {@code @}{@link KeyForBottom}, and ones in which every
 * instance of {@code @PolyKeyFor} has been replaced by {@code @}{@code
 * KeyFor}, for every possible combination of map arguments.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@TypeQualifier
@PolymorphicQualifier(UnknownKeyFor.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyKeyFor {}
