package checkers.tainting.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

import checkers.tainting.TaintingChecker;
import checkers.quals.*;

/**
 * The root of the tainting checker.
 * This annotation is associated with the {@link TaintingChecker}.
 *
 * @see Untainted
 * @see TaintingChecker
 * @checker.framework.manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
@TypeQualifier
@DefaultQualifierInHierarchy
@SubtypeOf( {} )
public @interface Tainted {
}
