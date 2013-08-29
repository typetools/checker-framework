package checkers.interning.quals;

import java.lang.annotation.*;

import checkers.interning.InterningChecker;

/**
 * Class declaration to indicate the class does not override
 * <tt>equals(Object)</tt>, and therefore <tt>a.equals(b)</tt> and <tt>a ==
 * b</tt> behave identically.
 * <p>
 * 
 * A class may be annotated @UsesObjectEquals if neither it, nor any of its
 * supertypes or subtypes, overrides <tt>equals</tt>.  Therefore, it is
 * most commonly written on a direct subclass of <tt>Object</tt>.
 * <p>
 *
 * This annotation is associated with the {@link InterningChecker}.
 *
 * @see InterningChecker
 * @checker.framework.manual #interning-checker Interning Checker
 */

@Documented
@Inherited 
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsesObjectEquals {
	
}
