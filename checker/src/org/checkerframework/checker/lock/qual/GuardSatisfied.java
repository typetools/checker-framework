package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 *
 * If a variable {@code x} has type {@code @GuardSatisfied}, then all
 * lock expressions for {@code x}'s value are held.
 * <p>
 *
 * This annotation is used on a formal parameter or receiver to indicate that the
 * {@literal @}{@link GuardedBy} annotation on
 * the corresponding actual parameter or receiver at the method call site
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
 * @see GuardedBy
 * @see Holding
 * @checker_framework.manual #lock-checker Lock Checker
 */
@TypeQualifier
@SubtypeOf(GuardedByInaccessible.class) // TODO: Should @GuardSatisfied be in its own hierarchy?
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({  ElementType.PARAMETER, ElementType.TYPE_USE })
public @interface GuardSatisfied {
    /**
     * The index on the polymorphic qualifier.
     * Defaults to -1 so that the user can write any index starting from 0.
     */
    int value() default -1;
}
