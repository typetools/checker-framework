package checkers.igj;

import java.lang.annotation.Target;

import checkers.igj.quals.I;
import checkers.igj.quals.Immutable;
import checkers.igj.quals.Mutable;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import com.sun.source.tree.Tree.Kind;

/**
 * An annotation used to represent a place holder immutability type, that is a
 * subtype of all other types. For example, {@code null} type is a subtype
 * of all immutability types.
 *
 * However, it is an implementation detail; hence, the package-scope.
 */
@TypeQualifier
@SubtypeOf({Mutable.class, Immutable.class, I.class})
@Target({}) // empty target prevents programmers from writing this in a program
@ImplicitFor(
        trees = { Kind.NULL_LITERAL },
        typeClasses = { AnnotatedPrimitiveType.class }
)
@interface IGJBottom { }
