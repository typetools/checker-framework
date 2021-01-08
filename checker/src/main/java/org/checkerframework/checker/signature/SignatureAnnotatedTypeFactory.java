package org.checkerframework.checker.signature;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.signature.qual.ArrayWithoutPackage;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.BinaryNameOrPrimitiveType;
import org.checkerframework.checker.signature.qual.BinaryNameWithoutPackage;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiersOrPrimitiveType;
import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.checkerframework.checker.signature.qual.FieldDescriptorForPrimitive;
import org.checkerframework.checker.signature.qual.FieldDescriptorWithoutPackage;
import org.checkerframework.checker.signature.qual.FqBinaryName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.checker.signature.qual.Identifier;
import org.checkerframework.checker.signature.qual.IdentifierOrPrimitiveType;
import org.checkerframework.checker.signature.qual.InternalForm;
import org.checkerframework.checker.signature.qual.PrimitiveType;
import org.checkerframework.checker.signature.qual.SignatureBottom;
import org.checkerframework.checker.signature.qual.SignatureUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.reflection.SignatureRegexes;

// TODO: Does not yet handle method signature annotations, such as
// @MethodDescriptor.

/** Accounts for the effects of certain calls to String.replace. */
public class SignatureAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The {@literal @}{@link SignatureUnknown} annotation. */
    protected final AnnotationMirror SIGNATURE_UNKNOWN =
            AnnotationBuilder.fromClass(elements, SignatureUnknown.class);
    /** The {@literal @}{@link BinaryName} annotation. */
    protected final AnnotationMirror BINARY_NAME =
            AnnotationBuilder.fromClass(elements, BinaryName.class);
    /** The {@literal @}{@link InternalForm} annotation. */
    protected final AnnotationMirror INTERNAL_FORM =
            AnnotationBuilder.fromClass(elements, InternalForm.class);
    /** The {@literal @}{@link DotSeparatedIdentifiers} annotation. */
    protected final AnnotationMirror DOT_SEPARATED_IDENTIFIERS =
            AnnotationBuilder.fromClass(elements, DotSeparatedIdentifiers.class);
    /** The {@literal @}{@link CanonicalName} annotation. */
    protected final AnnotationMirror CANONICAL_NAME =
            AnnotationBuilder.fromClass(elements, CanonicalName.class);

    /** The {@link String#replace(char, char)} method. */
    private final ExecutableElement replaceCharChar =
            TreeUtils.getMethod(
                    java.lang.String.class.getCanonicalName(),
                    "replace",
                    processingEnv,
                    "char",
                    "char");

    /** The {@link String#replace(CharSequence, CharSequence)} method. */
    private final ExecutableElement replaceCharSequenceCharSequence =
            TreeUtils.getMethod(
                    java.lang.String.class.getCanonicalName(),
                    "replace",
                    processingEnv,
                    "java.lang.CharSequence",
                    "java.lang.CharSequence");

    /** The {@link Class#getName()} method. */
    private final ExecutableElement classGetName =
            TreeUtils.getMethod(java.lang.Class.class.getCanonicalName(), "getName", processingEnv);

    /**
     * Creates a SignatureAnnotatedTypeFactory.
     *
     * @param checker the type-checker assocated with this type factory
     */
    public SignatureAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(SignatureUnknown.class, SignatureBottom.class);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        // It is slightly inefficient that super also adds a LiteralTreeAnnotator, but it seems
        // better than hard-coding the behavior of super here.
        return new ListTreeAnnotator(
                signatureLiteralTreeAnnotator(this),
                new SignatureTreeAnnotator(this),
                super.createTreeAnnotator());
    }

    /**
     * Create a LiteralTreeAnnotator for the Signature Checker.
     *
     * @param atypeFactory the type factory
     * @return a LiteralTreeAnnotator for the Signature Checker
     */
    private LiteralTreeAnnotator signatureLiteralTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        LiteralTreeAnnotator result = new LiteralTreeAnnotator(atypeFactory);
        result.addStandardLiteralQualifiers();

        // The below code achieves the same effect as writing a meta-annotation
        //     @QualifierForLiterals(stringPatterns = "...")
        // on each type qualifier definition.  Annotation elements cannot be computations (not even
        // string concatenations of literal strings) and cannot be not references to compile-time
        // constants such as effectively-final fields.  So every `stringPatterns = "..."` would have
        // to be a literal string, which would be verbose ard hard to maintain.
        result.addStringPattern(
                SignatureRegexes.ArrayWithoutPackageRegex,
                AnnotationBuilder.fromClass(elements, ArrayWithoutPackage.class));
        result.addStringPattern(
                SignatureRegexes.BinaryNameRegex,
                AnnotationBuilder.fromClass(elements, BinaryName.class));
        result.addStringPattern(
                SignatureRegexes.BinaryNameOrPrimitiveTypeRegex,
                AnnotationBuilder.fromClass(elements, BinaryNameOrPrimitiveType.class));
        result.addStringPattern(
                SignatureRegexes.BinaryNameWithoutPackageRegex,
                AnnotationBuilder.fromClass(elements, BinaryNameWithoutPackage.class));
        result.addStringPattern(
                SignatureRegexes.ClassGetNameRegex,
                AnnotationBuilder.fromClass(elements, ClassGetName.class));
        result.addStringPattern(
                SignatureRegexes.ClassGetSimpleNameRegex,
                AnnotationBuilder.fromClass(elements, ClassGetSimpleName.class));
        result.addStringPattern(
                SignatureRegexes.DotSeparatedIdentifiersRegex,
                AnnotationBuilder.fromClass(elements, DotSeparatedIdentifiers.class));
        result.addStringPattern(
                SignatureRegexes.DotSeparatedIdentifiersOrPrimitiveTypeRegex,
                AnnotationBuilder.fromClass(
                        elements, DotSeparatedIdentifiersOrPrimitiveType.class));
        result.addStringPattern(
                SignatureRegexes.FieldDescriptorRegex,
                AnnotationBuilder.fromClass(elements, FieldDescriptor.class));
        result.addStringPattern(
                SignatureRegexes.FieldDescriptorForPrimitiveRegex,
                AnnotationBuilder.fromClass(elements, FieldDescriptorForPrimitive.class));
        result.addStringPattern(
                SignatureRegexes.FieldDescriptorWithoutPackageRegex,
                AnnotationBuilder.fromClass(elements, FieldDescriptorWithoutPackage.class));
        result.addStringPattern(
                SignatureRegexes.FqBinaryNameRegex,
                AnnotationBuilder.fromClass(elements, FqBinaryName.class));
        result.addStringPattern(
                SignatureRegexes.FullyQualifiedNameRegex,
                AnnotationBuilder.fromClass(elements, FullyQualifiedName.class));
        result.addStringPattern(
                SignatureRegexes.IdentifierRegex,
                AnnotationBuilder.fromClass(elements, Identifier.class));
        result.addStringPattern(
                SignatureRegexes.IdentifierOrPrimitiveTypeRegex,
                AnnotationBuilder.fromClass(elements, IdentifierOrPrimitiveType.class));
        result.addStringPattern(
                SignatureRegexes.InternalFormRegex,
                AnnotationBuilder.fromClass(elements, InternalForm.class));
        result.addStringPattern(
                SignatureRegexes.PrimitiveTypeRegex,
                AnnotationBuilder.fromClass(elements, PrimitiveType.class));
        return result;
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
         * form and binary name:
         *
         * <pre><code>
         * {@literal @}InternalForm String internalForm = binaryName.replace('.', '/');
         * {@literal @}BinaryName String binaryName = internalForm.replace('/', '.');
         * </code></pre>
         *
         * Class.getName sometimes returns a binary name (which is more specific than its
         * annotation, which is ClassGetName:
         *
         * <pre><code>
         * {@literal @}BinaryName String binaryName = MyClass.class.getName();
         * </code></pre>
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isMethodInvocation(tree, replaceCharChar, processingEnv)
                    || TreeUtils.isMethodInvocation(
                            tree, replaceCharSequenceCharSequence, processingEnv)) {
                char oldChar = ' '; // initial dummy value
                char newChar = ' '; // initial dummy value
                if (TreeUtils.isMethodInvocation(tree, replaceCharChar, processingEnv)) {
                    ExpressionTree arg0 = tree.getArguments().get(0);
                    ExpressionTree arg1 = tree.getArguments().get(1);
                    if (arg0.getKind() == Tree.Kind.CHAR_LITERAL
                            && arg1.getKind() == Tree.Kind.CHAR_LITERAL) {
                        oldChar = (char) ((LiteralTree) arg0).getValue();
                        newChar = (char) ((LiteralTree) arg1).getValue();
                    }
                } else {
                    ExpressionTree arg0 = tree.getArguments().get(0);
                    ExpressionTree arg1 = tree.getArguments().get(1);
                    if (arg0.getKind() == Tree.Kind.STRING_LITERAL
                            && arg1.getKind() == Tree.Kind.STRING_LITERAL) {
                        String const0 = (String) ((LiteralTree) arg0).getValue();
                        String const1 = (String) ((LiteralTree) arg1).getValue();
                        if (const0.length() == 1 && const1.length() == 1) {
                            oldChar = const0.charAt(0);
                            newChar = const1.charAt(0);
                        }
                    }
                }
                ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
                final AnnotatedTypeMirror receiverType = getAnnotatedType(receiver);
                if ((oldChar == '.' && newChar == '/')
                        && receiverType.getAnnotation(BinaryName.class) != null) {
                    type.replaceAnnotation(INTERNAL_FORM);
                } else if ((oldChar == '/' && newChar == '.')
                        && receiverType.getAnnotation(InternalForm.class) != null) {
                    type.replaceAnnotation(BINARY_NAME);
                }
            }

            if (TreeUtils.isMethodInvocation(tree, classGetName, processingEnv)) {
                ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
                if (TreeUtils.isClassLiteral(receiver)) {
                    ExpressionTree classExpr = ((MemberSelectTree) receiver).getExpression();
                    if (classExpr.getKind() == Tree.Kind.IDENTIFIER
                            || (classExpr.getKind() == Tree.Kind.PRIMITIVE_TYPE
                                    && ((PrimitiveTypeTree) classExpr).getPrimitiveTypeKind()
                                            != TypeKind.VOID)) {
                        type.replaceAnnotation(BINARY_NAME);
                    }
                }
            }

            return super.visitMethodInvocation(tree, type);
        }
    }
}
