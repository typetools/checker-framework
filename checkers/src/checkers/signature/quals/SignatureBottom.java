package checkers.signature.quals;

import java.lang.annotation.Target;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.ImplicitFor;
import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the type-qualifier hierarchy.
 * <p>
 * Not to be written by programmers, only used internally.
 */
@TypeQualifier
@SubtypeOf({SourceNameForNonArray.class,
    FieldDescriptorForArray.class,
    MethodDescriptor.class
    })
@Target( {} )
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface SignatureBottom {}
