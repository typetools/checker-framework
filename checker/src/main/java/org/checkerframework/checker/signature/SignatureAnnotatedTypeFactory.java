package org.checkerframework.checker.signature;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.BinaryNameForNonArray;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.checker.signature.qual.InternalForm;
import org.checkerframework.checker.signature.qual.InternalFormForNonArray;
import org.checkerframework.checker.signature.qual.SignatureBottom;
import org.checkerframework.checker.signature.qual.SignatureUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

// TODO: Does not yet handle method signature annotations, such as
// @MethodDescriptor.

/** Accounts for the effects of certain calls to String.replace. */
public class SignatureAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror SIGNATURE_UNKNOWN;
    protected final AnnotationMirror BINARY_NAME;
    protected final AnnotationMirror INTERNAL_FORM;
    protected final AnnotationMirror FULLY_QUALIFIED_NAME;
    protected final AnnotationMirror CLASS_GET_NAME;
    protected final AnnotationMirror BINARY_NAME_FOR_NON_ARRAY;
    protected final AnnotationMirror INTERNAL_FORM_FOR_NON_ARRAY;

    /** The {@link String#replace(char, char)} method. */
    private final ExecutableElement replaceCharChar;

    /** The {@link String#replace(CharSequence, CharSequence)} method. */
    private final ExecutableElement replaceCharSequenceCharSequence;

    public SignatureAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        SIGNATURE_UNKNOWN = AnnotationBuilder.fromClass(elements, SignatureUnknown.class);
        BINARY_NAME = AnnotationBuilder.fromClass(elements, BinaryName.class);
        INTERNAL_FORM = AnnotationBuilder.fromClass(elements, InternalForm.class);
        FULLY_QUALIFIED_NAME = AnnotationBuilder.fromClass(elements, FullyQualifiedName.class);
        CLASS_GET_NAME = AnnotationBuilder.fromClass(elements, ClassGetName.class);
        BINARY_NAME_FOR_NON_ARRAY =
                AnnotationBuilder.fromClass(elements, BinaryNameForNonArray.class);
        INTERNAL_FORM_FOR_NON_ARRAY =
                AnnotationBuilder.fromClass(elements, InternalFormForNonArray.class);

        replaceCharChar =
                TreeUtils.getMethod(
                        java.lang.String.class.getName(), "replace", processingEnv, "char", "char");
        replaceCharSequenceCharSequence =
                TreeUtils.getMethod(
                        java.lang.String.class.getName(),
                        "replace",
                        processingEnv,
                        "java.lang.CharSequence",
                        "java.lang.CharSequence");

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithPolyAll(SignatureUnknown.class, SignatureBottom.class);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new SignatureTreeAnnotator(this), super.createTreeAnnotator());
    }

    private class SignatureTreeAnnotator extends TreeAnnotator {

        public SignatureTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringConcatenation(tree)) {
                type.removeAnnotationInHierarchy(SIGNATURE_UNKNOWN);
                // This could be made more precise.
                type.addAnnotation(SignatureUnknown.class);
            }
            return null; // super.visitBinary(tree, type);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringCompoundConcatenation(node)) {
                type.removeAnnotationInHierarchy(SIGNATURE_UNKNOWN);
                // This could be made more precise.
                type.addAnnotation(SignatureUnknown.class);
            }
            return null; // super.visitCompoundAssignment(node, type);
        }

        /**
         * String.replace, when called with specific constant arguments, converts between internal
         * form and binary name.
         *
         * <pre><code>
         * {@literal @}InternalForm String internalForm = binaryName.replace('.', '/');
         * {@literal @}BinaryName String binaryName = internalForm.replace('/', '.');
         * </code></pre>
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isMethodInvocation(tree, replaceCharChar, processingEnv)
                    || TreeUtils.isMethodInvocation(
                            tree, replaceCharSequenceCharSequence, processingEnv)) {
                char c1 = ' ';
                char c2 = ' ';
                if (TreeUtils.isMethodInvocation(tree, replaceCharChar, processingEnv)) {
                    ExpressionTree arg0 = tree.getArguments().get(0);
                    ExpressionTree arg1 = tree.getArguments().get(1);
                    if (arg0.getKind() == Tree.Kind.CHAR_LITERAL
                            && arg1.getKind() == Tree.Kind.CHAR_LITERAL) {
                        c1 = (char) ((LiteralTree) arg0).getValue();
                        c2 = (char) ((LiteralTree) arg1).getValue();
                    }
                } else {
                    ExpressionTree arg0 = tree.getArguments().get(0);
                    ExpressionTree arg1 = tree.getArguments().get(1);
                    if (arg0.getKind() == Tree.Kind.STRING_LITERAL
                            && arg1.getKind() == Tree.Kind.STRING_LITERAL) {
                        String const0 = (String) ((LiteralTree) arg0).getValue();
                        String const1 = (String) ((LiteralTree) arg1).getValue();
                        if (const0.length() == 1 && const1.length() == 1) {
                            c1 = const0.charAt(0);
                            c2 = const1.charAt(0);
                        }
                    }
                }
                if (!(c1 == ' ' || c2 == ' ')) {
                    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
                    final AnnotatedTypeMirror receiverType = getAnnotatedType(receiver);
                    if (receiverType.getAnnotation(BinaryName.class) != null) {
                        handleReplaceOnBinaryName(c1, c2, type);
                    } else if (receiverType.getAnnotation(InternalForm.class) != null) {
                        handleReplaceOnInternalForm(c1, c2, type);
                    } else if (receiverType.getAnnotation(InternalFormForNonArray.class) != null) {
                        handleReplaceOnInternalFormForNonArray(c1, c2, type);
                    } else if (receiverType.getAnnotation(BinaryNameForNonArray.class) != null) {
                        handleReplaceOnBinaryNameForNonArray(c1, c2, type);
                    }
                }
            }
            return super.visitMethodInvocation(tree, type);
        }

        /**
         * Handles String.replace calls when the receiver is a {@link
         * org.checkerframework.checker.signature.qual.BinaryName}
         */
        private void handleReplaceOnBinaryName(char c1, char c2, AnnotatedTypeMirror type) {
            if (c1 == '.' && c2 == '/') {
                type.replaceAnnotation(INTERNAL_FORM);
            }
        }

        /**
         * Handles String.replace calls when the receiver is a {@link
         * org.checkerframework.checker.signature.qual.BinaryNameForNonArray}
         */
        private void handleReplaceOnBinaryNameForNonArray(
                char c1, char c2, AnnotatedTypeMirror type) {
            if (c1 == '.' && c2 == '/') {
                type.replaceAnnotation(INTERNAL_FORM_FOR_NON_ARRAY);
            }
        }

        /**
         * Handles String.replace calls when the receiver is a {@link
         * org.checkerframework.checker.signature.qual.InternalFormForNonArray}
         */
        private void handleReplaceOnInternalFormForNonArray(
                char c1, char c2, AnnotatedTypeMirror type) {
            if (c1 == '/' && c2 == '.') {
                type.replaceAnnotation(BINARY_NAME_FOR_NON_ARRAY);
            }
        }

        /**
         * Handles String.replace calls when the receiver is a {@link
         * org.checkerframework.checker.signature.qual.InternalForm}
         */
        private void handleReplaceOnInternalForm(char c1, char c2, AnnotatedTypeMirror type) {
            if (c1 == '/' && c2 == '.') {
                type.replaceAnnotation(BINARY_NAME);
            }
        }
    }
}
