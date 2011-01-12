package checkers.signature.quals;

import java.lang.annotation.Target;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.ImplicitFor;
import com.sun.source.tree.Tree;

@TypeQualifier
@SubtypeOf({SourceName.class,
    BinarySignature.class,
    FullyQualifiedSignature.class,
    FieldDescriptor.class,
    MethodDescriptor.class
    })
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface SignatureBottom {}
