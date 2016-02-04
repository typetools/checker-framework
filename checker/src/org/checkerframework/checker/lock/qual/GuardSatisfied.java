package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 *
 * If a variable {@code x} has type {@code @GuardSatisfied}, then all
 * lock expressions for {@code x}'s value are held.
 * <p>
 *
 * This annotation is used as a primary annotation on a formal parameter, receiver or return type to indicate that the
 * {@literal @}{@link GuardedBy} annotation on
 * the corresponding actual parameter, receiver or return type at the method call site
 * is unknown at the method definition site, but the lock expressions
 * on the {@literal @}{@link GuardedBy} annotation are known to be held
 * prior to the method call.
 * <p>
 *
 * For example, the formal parameter of the String copy constructor,
 * {@link String#String(String s)}, is annotated with {@code @GuardSatisfied} since
 * the {@code @GuardedBy} annotation
 * on the actual parameter to a call to the constructor is unknown
 * at its definition site, because the constructor can be called by
 * arbitrary code.
 * <p>
 *
 * When type checking a call to a method with a receiver, return type and/or formal parameters with types annotated with
 * {@code @GuardSatisfied(index)}, the Lock Checker ensures that for all {@code @GuardSatisfied} annotations
 * with the same index, the corresponding primary annotations in the {@code @GuardedBy} hierarchy
 * of the types at the call site are subtypes of one another (in other words, the
 * least upper bound of the set is an exact match with one of the elements). Note that a return type can only
 * be annotated with {@code @GuardSatisfied(index)}, not {@code @GuardSatisfied}.
 * <p>
 *
 * Note that if formal parameter types are
 * annotated with {@code @GuardSatisfied} without an index, then those formal parameter
 * types are unrelated with regard to types in the {@code @GuardedBy} hierarchy.
 * <p>
 *
 * {@code @GuardSatisfied} may not be used on formal parameters, receivers or return types of a
 * method annotated with {@literal @}{@link MayReleaseLocks}
 *
 * @see GuardedBy
 * @see Holding
 * @checker_framework.manual #lock-checker Lock Checker
 */
@SubtypeOf(GuardedByInaccessible.class) // TODO: Should @GuardSatisfied be in its own hierarchy?
@Documented
@Retention(RetentionPolicy.RUNTIME)
// TODO: GuardSatisfied should only be allowed on method parameters, receivers, and return types
// but ElementType does not these choices.
@Target({  ElementType.PARAMETER, ElementType.TYPE_USE })
public @interface GuardSatisfied {
    /**
     * The index on the GuardSatisfied polymorphic qualifier.
     * Defaults to -1 so that the user can write any index starting from 0.
     */
    int value() default -1;
}
