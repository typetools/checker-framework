package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.interning.InterningChecker;

/**
 * Class declaration to indicate the class does not override
 * <code>equals(Object)</code>, and therefore <code>a.equals(b)</code> and <code>a ==
 * b</code> behave identically.
 * <p>
 *
 * A class may be annotated @UsesObjectEquals if neither it, nor any of its
 * supertypes or subtypes, overrides <code>equals</code>.  Therefore, it cannot
 * be written on <code>Object</code> itself.  It is
 * most commonly written on a direct subclass of <code>Object</code>.
 * <p>
 *
 * This annotation is associated with the {@link InterningChecker}.
 *
 * @see InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsesObjectEquals {}
