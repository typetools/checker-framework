package checkers.tainting.quals;

import java.lang.annotation.*;

import checkers.tainting.TaintingChecker;
import checkers.quals.*;

/**
 * The top qualifier of the tainting type system.
 * This annotation is associated with the {@link TaintingChecker}.
 *
 * @see Untainted
 * @see TaintingChecker
 * @checker.framework.manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@DefaultQualifierInHierarchy
@SubtypeOf({})
public @interface Tainted {}
