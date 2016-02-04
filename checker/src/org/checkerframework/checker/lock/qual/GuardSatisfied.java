package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If a variable {@code x} has type {@code @GuardSatisfied}, then all
 * lock expressions for {@code x}'s value are held.
 * <p>
 *
 * Written on a formal parameter (including receiver), this annotation
 * indicates that the {@literal @}{@link GuardedBy} type for
 * the corresponding actual argument at the method call site
 * is unknown at the method definition site, but any lock expressions
 * that guard it are known to be held prior to the method call.
 * <p>
 *
 * For example, the formal parameter of the String copy constructor,
 * {@link String#String(String s)}, is annotated with {@code @GuardSatisfied}.
 * This requires that all locks guarding the actual argument are held when
 * the constructor is called.  However, the definition of the constructor
 * does not need to know what those locks are (and it cannot know, because
 * the constructor can be called by arbitrary code).
 * <p>
 *
 * For all {@code @GuardSatisfied(index)} annotations
 * with the same index, the corresponding primary annotations in the {@code @GuardedBy} hierarchy
 * of the types at the call site are subtypes of one another (in other words, the
 * least upper bound of the set is an exact match with one of the elements).
 * Note that a return type can only
 * be annotated with {@code @GuardSatisfied(index)}, not {@code @GuardSatisfied}.
 * <p>
 *
 * Note that if formal parameter types are
 * annotated with {@code @GuardSatisfied} without an index, then those formal parameter
 * types are unrelated with regard to types in the {@code @GuardedBy} hierarchy.
 * <p>
 *
 * {@code @GuardSatisfied} may not be used on formal parameters, receivers, or return types of a
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
