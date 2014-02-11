package checkers.reflection.quals;

import checkers.quals.ImplicitFor;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Target;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the ClassVal qualifier hierarchy. This is used to
 * make the null literal a subtype of all ClassVal annotations.
 * 
 * This annotation may not be written in source code; it is an implementation
 * detail of the checker.
 */
@TypeQualifier
@InvisibleQualifier
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ ClassVal.class, ClassBound.class })
@Target({})
public @interface ClassValBottom {
}
