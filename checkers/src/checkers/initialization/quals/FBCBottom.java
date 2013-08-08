package checkers.initialization.quals;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.source.tree.Tree;

/**
 * {@link FBCBottom} marks the bottom of the Freedom Before Commitment type
 * hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker.framework.manual #nullness-checker Nullness Checker
 * @author Stefan Heule
 */
@TypeQualifier
@SubtypeOf({ UnderInitialization.class, Initialized.class })
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL })
@Documented
@Retention(RetentionPolicy.RUNTIME)
// empty target prevents programmers from writing this in a program
@Target({})
public @interface FBCBottom {
}
