package org.checkerframework.checker.signature;

import com.sun.source.tree.CompilationUnitTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * The Signature Checker.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
// Don't use @RelevantJavaTypes.  Any object can be annotated, which should propagate through its
// toString().
// @RelevantJavaTypes(CharSequence.class)
@StubFiles({"java-lang-classfile.astub", "javac.astub", "javaparser.astub"})
public final class SignatureChecker extends BaseTypeChecker {

  /** Create a SignatureChecker. */
  public SignatureChecker() {}

  // This method is needed only under MacOS, perhaps as a result of the
  // broken Apple Java distribution.
  public SignatureAnnotatedTypeFactory createFactory(CompilationUnitTree root) {
    return new SignatureAnnotatedTypeFactory(this);
  }
}
