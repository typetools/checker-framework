package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 *
 * If a variable {@code x} has type {@code @GuardSatisfied}, then all
 * lock expressions for {@code x}'s value are held.
 * <p>
 *
 * This annotation is used on a formal parameter to indicate that the
 * {@literal @}{@link GuardedBy} annotation on
 * the corresponding actual parameter at the method call site
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
@SubtypeOf(GuardedByInaccessible.class) // TODO: As an implementation detail, should this be in its own hierarchy?
@Documented
@DefaultFor({DefaultLocation.RECEIVERS, DefaultLocation.PARAMETERS})
@Retention(RetentionPolicy.RUNTIME)
@Target({  ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE })
public @interface GuardSatisfied {
    /**
     * The index on the polymorphic qualifier.
     * Defaults to 0.
     */
    int value() default 0;
}
