package checkers.value.quals;

import checkers.quals.ImplicitFor;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import com.sun.source.tree.Tree;
import java.lang.annotation.Target;

/**
 * Represents the bottom of the Value qualifier hierarchy. This is used to make
 * the null literal a subtype of all Value annotations.
 * <p>
 * 
 * This annotation may not be written in source code; it is an implementation
 * detail of the checker.
 */
@TypeQualifier
@InvisibleQualifier
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ ArrayLen.class, BoolVal.class, CharVal.class, DoubleVal.class,
        IntVal.class, StringVal.class })
@Target({})
// empty target prevents programmers from writing this in a program
public @interface BottomVal {
}
