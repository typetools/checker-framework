package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * A polymorphic qualifier for the GuardedBy type system.
 * Indicates that is unknown what the guards are or whether they are held.
 * An expression whose type is {@code @PolyGuardedBy} cannot be dereferenced.
 * <p>
 *
 * Any method written using {@code @PolyGuardedBy} conceptually has an
 * arbitrary number of versions:  one in which every instance of
 * {@code @PolyGuardedBy} has been replaced by {@code @}{@link GuardedByInaccessible},
 * one in which every instance of {@code @PolyGuardedBy} has been
 * replaced by {@code @}{@link GuardedByBottom}, and ones in which every
 * instance of {@code @PolyGuardedBy} has been replaced by {@code @}{@link GuardedBy},
 * for every possible combination of map arguments.
 * <p>
 *
 * @see GuardedBy
 * @checker_framework.manual #lock-checker Lock Checker
 */
@TypeQualifier
@PolymorphicQualifier(GuardedByInaccessible.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyGuardedBy {}
