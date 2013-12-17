package checkers.oigj;

import checkers.oigj.quals.I;
import checkers.oigj.quals.Immutable;
import checkers.oigj.quals.Modifier;
import checkers.oigj.quals.Mutable;
import checkers.oigj.quals.O;
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
@SubtypeOf({Mutable.class, Immutable.class, I.class,
    Modifier.class, O.class})
@Target({}) // empty target prevents programmers from writing this in a program
@ImplicitFor(
        trees = { Kind.NULL_LITERAL, Kind.CLASS, Kind.ENUM,
                Kind.INTERFACE, Kind.ANNOTATION_TYPE,
                Kind.NEW_ARRAY },
        typeClasses = { AnnotatedPrimitiveType.class },
        typeNames = { java.lang.Void.class }
)
@interface OIGJMutabilityBottom { }
