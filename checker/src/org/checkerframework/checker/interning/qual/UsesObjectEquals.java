package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.interning.InterningChecker;

/**
 * TODO: CODE REVIEW:
 * TODO: THE NAME OF THIS ANNOTATION SEEMS LIKE IT INDICATES THE OPPOSITE OF
 * TODO: WHAT IT DOES.  I GET WHAT IT'S TRYING TO SAY: "This class uses
 * TODO: the implementation of .equals in Object."  BUT TO ME, @UsesReferenceEquality
 * TODO: OR @DoesNotOverrideEquals  WOULD BE MUCH MORE OBVIOUS BECAUSE
 * TODO  THIS SEEMS TO ME TO SAY THAT WE SHOUL BE USING .equals WITH THIS METHOD
 * TODO: WHEN IN FACT IT'S THE OPPOSITE THAT SAID MAYBE WE SHOULD KEEP IT FOR
 * TODO: BACKWARDS COMPATIBILITY'S SAKE OR DEPRECATE IT AND CREATE A NEW ANNOTATION
 *
 * Class declaration to indicate the class does not override
 * <tt>equals(Object)</tt>, and therefore <tt>a.equals(b)</tt> and <tt>a ==
 * b</tt> behave identically.
 * <p>
 *
 * A class may be annotated @UsesObjectEquals if neither it, nor any of its
 * supertypes or subtypes, overrides <tt>equals</tt>.  Therefore, it cannot
 * be written on <tt>Object</tt> itself.  It is
 * most commonly written on a direct subclass of <tt>Object</tt>.
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
