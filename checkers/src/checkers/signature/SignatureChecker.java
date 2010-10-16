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
public final class SignatureChecker extends BaseTypeChecker {

  // This method is needed only under MacOS, perhaps as a result of the
  // broken Apple Java distribution.
  public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
    return new SignatureAnnotatedTypeFactory(this, root);
  }

}
