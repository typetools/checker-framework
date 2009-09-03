package checkers.igj;

import java.lang.annotation.Target;

import com.sun.source.tree.Tree.Kind;

import checkers.igj.quals.*;
import checkers.quals.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

/**
 * An annotation used to represent a place holder immutability type, that is a
 * subtype of all other types. For example, {@code null} type is a subtype
 * of all immutability types.
 *
 * However, it is an implementation detail; hence, the package-scope.
 */
@TypeQualifier
@SubtypeOf({Mutable.class, Immutable.class, I.class})
@Target({})
@ImplicitFor(
        trees = { Kind.NULL_LITERAL, Kind.CLASS, Kind.NEW_ARRAY },
        typeClasses = { AnnotatedPrimitiveType.class }
)
@interface IGJBottom { }
