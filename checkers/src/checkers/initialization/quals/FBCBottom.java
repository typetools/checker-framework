package checkers.initialization.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.source.tree.Tree;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * {@link FBCBottom} marks the bottom of the Freedom Before Commitment type
 * hierarchy. It cannot be used in client code and is only used internally.
 *
 * @author Stefan Heule
 */
@Documented
@TypeQualifier
@SubtypeOf({ UnderInitializion.class, Initialized.class })
@Retention(RetentionPolicy.RUNTIME)
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL })
// empty target prevents programmers from writing this in a program
@Target({})
public @interface FBCBottom {
}
