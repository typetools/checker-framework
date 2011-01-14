package checkers.signature.quals;

import java.lang.annotation.Target;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.ImplicitFor;
import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the type-qualifier hierarchy.
 * Not to be used by the annotator, only used internally.
 * @author Kivanc Muslu
 */
@TypeQualifier
@SubtypeOf({SourceName.class,
    FieldDescriptor.class,
    MethodDescriptor.class
    })
@Target( {} )
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface SignatureBottom {}
