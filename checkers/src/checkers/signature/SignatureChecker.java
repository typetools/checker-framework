package checkers.signature;

import checkers.signature.quals.*;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.PolyAll;
import checkers.types.AnnotatedTypeFactory;
import com.sun.source.tree.CompilationUnitTree;

@TypeQualifiers({
    UnannotatedString.class,
    FullyQualifiedName.class,
    BinaryName.class,
    SourceName.class,
    SourceNameForNonArray.class,
    ClassGetName.class,
    BinaryNameForNonArray.class,
    FieldDescriptor.class,
    FieldDescriptorForArray.class,
    SignatureBottom.class,

    MethodDescriptor.class,

    PolySignature.class,
    PolyAll.class
})
public final class SignatureChecker extends BaseTypeChecker {

  // This method is needed only under MacOS, perhaps as a result of the
  // broken Apple Java distribution.
  public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
    return new SignatureAnnotatedTypeFactory(this, root);
  }

}
