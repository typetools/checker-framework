package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * The bottom of the type hierarchy.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( {} )
@TypeQualifier
@SubtypeOf( { Fenum.class, FenumUnqualified.class } )
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface FenumBottom {}
