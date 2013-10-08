package checkers.signature;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.signature.quals.BinaryName;
import checkers.signature.quals.BinaryNameForNonArray;
import checkers.signature.quals.ClassGetName;
import checkers.signature.quals.FieldDescriptor;
import checkers.signature.quals.FieldDescriptorForArray;
import checkers.signature.quals.FullyQualifiedName;
import checkers.signature.quals.MethodDescriptor;
import checkers.signature.quals.PolySignature;
import checkers.signature.quals.SignatureBottom;
import checkers.signature.quals.SourceName;
import checkers.signature.quals.SourceNameForNonArray;
import checkers.signature.quals.UnannotatedString;

import com.sun.source.tree.CompilationUnitTree;

/**
 * @checker.framework.manual #signature-checker Signature Checker
 */
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
  public SignatureAnnotatedTypeFactory createFactory(CompilationUnitTree root) {
    return new SignatureAnnotatedTypeFactory(this);
  }

}
