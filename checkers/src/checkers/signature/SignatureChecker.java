package checkers.signature;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.signature.quals.*;

@TypeQualifiers({
    BinaryName.class, 
    BinarySignature.class, 
    FullyQualifiedName.class, 
    FullyQualifiedSignature.class, 
    SourceName.class,
    FieldDescriptor.class, 
    MethodDescriptor.class,
    UnannotatedString.class,
    SignatureBottom.class
})
public final class SignatureChecker extends BaseTypeChecker {}
