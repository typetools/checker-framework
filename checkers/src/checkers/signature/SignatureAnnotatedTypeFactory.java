package checkers.signature;

import checkers.signature.quals.*;

import java.lang.annotation.Annotation;
import java.util.regex.*;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeChecker;
import checkers.types.*;

// This code is copied from SignatureAnnotatedTypeFactory.
// The two could be generalized and combined, perhaps.

// TODO: Does not yet handle method signature annotations, such as
// @MethodDescriptor.

/**
 * Adds a signature annotation to the type of every tree that is a {@code
 * String} literal.  The particular annotation depends on the literal.
 */
public class SignatureAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<SignatureChecker> {

    public SignatureAnnotatedTypeFactory(SignatureChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public TreeAnnotator createTreeAnnotator(SignatureChecker checker) {
        return new SignatureTreeAnnotator(checker);
    }

    private class SignatureTreeAnnotator extends TreeAnnotator {

        public SignatureTreeAnnotator(BaseTypeChecker checker) {
            super(checker, SignatureAnnotatedTypeFactory.this);
        }

        /**
         * Case 1: String literal
         */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && tree.getKind() == Tree.Kind.STRING_LITERAL) {
                String s = (String)((LiteralTree)tree).getValue();
                type.addAnnotation(bestSignatureAnnotation(s));
            }
            return super.visitLiteral(tree, type);
        }

    }

    private Pattern fqnPat = Pattern.compile("^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\[\\])*$");
    private Pattern bnPat = Pattern.compile("^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_][A-Za-z_0-9]*)?(\\[\\])*$");
    private Pattern fdPat = Pattern.compile("^\\[*([BCDFIJSZ]|L[A-Za-z_][A-Za-z_0-9]*(/[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_][A-Za-z_0-9]*)?;)$");

    private Class<? extends Annotation> bestSignatureAnnotation(String s) {
        if (fdPat.matcher(s).matches()) {
            if (bnPat.matcher(s).matches()) {
                return SignatureBottom.class;
            } else {
                return FieldDescriptor.class;
            }
        } else {
            if (bnPat.matcher(s).matches()) {
                if (fqnPat.matcher(s).matches()) {
                    // Both a BinaryName and a FullyQualifiedName
                    return SourceName.class;
                } else {
                    return BinaryName.class;
                }
            } else {
                return UnannotatedString.class;
            }
        }
    }

}
