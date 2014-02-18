package checkers.igj;

import checkers.igj.quals.I;
import checkers.igj.quals.Immutable;
import checkers.igj.quals.Mutable;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import java.lang.annotation.Target;

import com.sun.source.tree.Tree.Kind;

/**
 * An annotation used to represent a place holder immutability type, that is a
 * subtype of all other types. For example, {@code null} type is a subtype
 * of all immutability types.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 */
@TypeQualifier
@SubtypeOf({Mutable.class, Immutable.class, I.class})
@ImplicitFor(
        trees = { Kind.NULL_LITERAL },
        typeClasses = { AnnotatedPrimitiveType.class }
)
@Target({}) // empty target prevents programmers from writing this in a program
@interface IGJBottom {}
