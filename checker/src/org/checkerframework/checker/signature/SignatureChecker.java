package org.checkerframework.checker.signature;

import org.checkerframework.common.basetype.BaseTypeChecker;
import com.sun.source.tree.CompilationUnitTree;

/**
 * @checker_framework.manual #signature-checker Signature Checker
 */
public final class SignatureChecker extends BaseTypeChecker {

  // This method is needed only under MacOS, perhaps as a result of the
  // broken Apple Java distribution.
  public SignatureAnnotatedTypeFactory createFactory(CompilationUnitTree root) {
    return new SignatureAnnotatedTypeFactory(this);
  }

}
