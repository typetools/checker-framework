package checkers.interning.quals;

import java.lang.annotation.*;

import checkers.interning.InterningChecker;
import checkers.quals.*;

/**
 * Class declaration to indicate the class does not override .equals(Object),
 * and therefore .equals and == have no difference in behavior.
 * 
 * Specifically, a class may only be @UsesObjectEquals if it's super type is
 * Object, or is also annotated @UsesObjectEquals. Similarly, once a class is 
 * documented as @UsesObjectEquals every subtype must also use the declaration.
 * 
 * This annotation is associated with the {@link InterningChecker}.
 *
 * @see InterningChecker
 * @checker.framework.manual #interning-checker Interning Checker
 */

@Documented
@Inherited 
@Target(ElementType.TYPE)
//@Retention(...) ??
public @interface UsesObjectEquals {
	
}