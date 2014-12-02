package org.checkerframework.checker.signature;

import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.BinaryNameForNonArray;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.checkerframework.checker.signature.qual.FieldDescriptorForArray;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.checker.signature.qual.MethodDescriptor;
import org.checkerframework.checker.signature.qual.PolySignature;
import org.checkerframework.checker.signature.qual.SignatureBottom;
import org.checkerframework.checker.signature.qual.SourceName;
import org.checkerframework.checker.signature.qual.SourceNameForNonArray;
import org.checkerframework.checker.signature.qual.UnannotatedString;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.TypeQualifiers;

import com.sun.source.tree.CompilationUnitTree;

/**
 * @checker_framework.manual #signature-checker Signature Checker
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
