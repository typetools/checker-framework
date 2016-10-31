package org.checkerframework.checker.signature;

import com.sun.source.tree.CompilationUnitTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/** @checker_framework.manual #signature-checker Signature Checker */
@RelevantJavaTypes(CharSequence.class)
public final class SignatureChecker extends BaseTypeChecker {

    // This method is needed only under MacOS, perhaps as a result of the
    // broken Apple Java distribution.
    public SignatureAnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new SignatureAnnotatedTypeFactory(this);
    }
}
