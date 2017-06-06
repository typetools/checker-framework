package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class declaration to indicate the class does not override {@code equals(Object)}, and therefore
 * {@code a.equals(b)} and {@code a == b} behave identically.
 *
 * <p>A class may be annotated @UsesObjectEquals if neither it, nor any of its supertypes or
 * subtypes, overrides {@code equals}. Therefore, it cannot be written on {@code Object} itself. It
 * is most commonly written on a direct subclass of {@code Object}.
 *
 * <p>This annotation is associated with the {@link
 * org.checkerframework.checker.interning.InterningChecker}.
 *
 * @see org.checkerframework.checker.interning.InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsesObjectEquals {}
