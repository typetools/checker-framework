package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * This annotation is used on a formal parameter to indicate that the
 * parameter for which the {@literal @}{@link GuardedBy} annotation on
 * the actual parameter is unknown at the method definition site.
 * <p>
 *
 * For example, the parameter of {@link String#String(String s)} is
 * annotated with <code>@UnknownGuard</code>, because
 * the <code>@GuardedBy</code> annotation on the actual parameter to a
 * call to <code>String(String s)</code> is unknown at its
 * definition site, because <code>String(String s)</code> can
 * be called by arbitrary code.
 *
 * @see GuardedBy
 * @checker_framework.manual #lock-checker Lock Checker
 */

@TypeQualifier
@SubtypeOf(GuardedByInaccessible.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE })
public @interface UnknownGuard {}
