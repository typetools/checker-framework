/*
TODO: Implement the functionality for @PolyGuardedBy and uncomment this.

package org.checkerframework.checker.lock.qual;

// import java.lang.annotation.Documented;
// import java.lang.annotation.ElementType;
// import java.lang.annotation.Retention;
// import java.lang.annotation.RetentionPolicy;
// import java.lang.annotation.Target;
//
// import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the GuardedBy type system.
 * Indicates that it is unknown what the guards are or whether they are held.
 * An expression whose type is {@code @PolyGuardedBy} cannot be dereferenced.
 * Hence, unlike for {@code @GuardSatisfied}, when an expression of type {@code @PolyGuardedBy}
 * is the LHS of an assignment, the locks guarding the RHS do not need to be held.
 *
 * <p>Any method written using {@code @PolyGuardedBy} conceptually has an
 * arbitrary number of versions:  one in which every instance of
 * {@code @PolyGuardedBy} has been replaced by {@code @}{@link GuardedByUnknown},
 * one in which every instance of {@code @PolyGuardedBy} has been
 * replaced by {@code @}{@link GuardedByBottom}, and ones in which every
 * instance of {@code @PolyGuardedBy} has been replaced by {@code @}{@link GuardedBy},
 * for every possible combination of map arguments.
 *
 * @see GuardedBy
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
// @PolymorphicQualifier(GuardedByUnknown.class)
// @Documented
// @Retention(RetentionPolicy.RUNTIME)
// @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
// public @interface PolyGuardedBy {}
