package checkers.signature.quals;

import java.lang.annotation.Target;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@TypeQualifier
@SubtypeOf({SourceName.class, 
    BinarySignature.class, 
    FullyQualifiedSignature.class, 
    FieldDescriptor.class, 
    MethodDescriptor.class
    })
@Target({})
public @interface SignatureBottom {}