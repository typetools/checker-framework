package org.checkerframework.checker.signature;

import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;

import org.checkerframework.checker.signature.qual.SignatureBottom;
import org.checkerframework.checker.signature.qual.SignatureUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;


// TODO: Does not yet handle method signature annotations, such as
// @MethodDescriptor.


public class SignatureAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror SIGNATURE_UNKNOWN;

    public SignatureAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        SIGNATURE_UNKNOWN = AnnotationUtils.fromClass(elements, SignatureUnknown.class);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithPolyAll(
                SignatureUnknown.class, SignatureBottom.class);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new SignatureTreeAnnotator(this),
                super.createTreeAnnotator()
        );
    }

    private class SignatureTreeAnnotator extends TreeAnnotator {

        public SignatureTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringConcatenation(tree)) {
                type.removeAnnotationInHierarchy(SIGNATURE_UNKNOWN);
                // This could be made more precise, when desired.
                type.addAnnotation(SignatureUnknown.class);
            }
            return null; // super.visitBinary(tree, type);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringCompoundConcatenation(node)) {
                type.removeAnnotationInHierarchy(SIGNATURE_UNKNOWN);
                // This could be made more precise, when desired.
                type.addAnnotation(SignatureUnknown.class);
            }
            return null; // super.visitCompoundAssignment(node, type);
        }

    }

}
