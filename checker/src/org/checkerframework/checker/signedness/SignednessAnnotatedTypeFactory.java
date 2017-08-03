package org.checkerframework.checker.signedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import java.lang.Byte;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Short;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.signedness.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;

/** @checker_framework.manual #signedness-checker Signedness Checker */
public class SignednessAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private final AnnotationMirror UNKNOWN_SIGNEDNESS;
    private final AnnotationMirror UNSIGNED;
    private final AnnotationMirror SIGNED;
    private final AnnotationMirror CONSTANT;

    private ValueAnnotatedTypeFactory valueAtypefactory;

    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant propagation and folding).
     */
    private ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        if (valueAtypefactory == null)
            valueAtypefactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        return valueAtypefactory;
    }

    // These are commented out until issues with making boxed implicitly signed
    // are worked out. (https://github.com/typetools/checker-framework/issues/797)
    /*
    private final String JAVA_LANG_BYTE = "java.lang.Byte";
    private final String JAVA_LANG_SHORT = "java.lang.Short";
    private final String JAVA_LANG_INTEGER = "java.lang.Integer";
    private final String JAVA_LANG_LONG = "java.lang.Long";
    */

    public SignednessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN_SIGNEDNESS = AnnotationUtils.fromClass(elements, UnknownSignedness.class);
        UNSIGNED = AnnotationUtils.fromClass(elements, Unsigned.class);
        SIGNED = AnnotationUtils.fromClass(elements, Signed.class);
        CONSTANT = AnnotationUtils.fromClass(elements, Constant.class);

        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithoutPolyAll();
    }

    /** {@inheritDoc} */
    @Override
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        // When it is possible to default types based on their TypeKinds,
        // this method will no longer be needed.
        // Currently, it is adding the LOCAL_VARIABLE default for
        // bytes, shorts, ints, and longs so that the implicit for
        // those types is not applied when they are local variables.
        // Only the local variable default is applied first because
        // it is the only refinable location (other than fields) that could
        // have a primitive type.

        addUnknownSignednessToSomeLocals(tree, type);
        super.addComputedTypeAnnotations(tree, type, iUseFlow);
    }

    /**
     * If the tree is a local variable and the type is a byte, short, int or long, then it adds the
     * UnknownSignedness annotation so that data flow can refine it.
     */
    private void addUnknownSignednessToSomeLocals(Tree tree, AnnotatedTypeMirror type) {
        switch (type.getKind()) {
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case CHAR:
                QualifierDefaults defaults = new QualifierDefaults(elements, this);
                defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
                defaults.annotate(tree, type);
                break;
            default:
                // Nothing for other cases.
        }

        // This code commented out until issues with making boxed implicitly signed
        // are worked out. (https://github.com/typetools/checker-framework/issues/797)

        /*switch (TypesUtils.getQualifiedName(type.getUnderlyingType()).toString()) {
        case JAVA_LANG_BYTE:
        case JAVA_LANG_SHORT:
        case JAVA_LANG_INTEGER:
        case JAVA_LANG_LONG:
            QualifierDefaults defaults = new QualifierDefaults(elements, this);
            defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
            defaults.annotate(tree, type);
        }*/

    }

    /** {@inheritDoc} */
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new SignednessTreeAnnotator(this), super.createTreeAnnotator());
    }

    /**
     * This TreeAnnotator ensures that booleans expressions are not given Unsigned or Signed
     * annotations by {@link PropagationTreeAnnotator}, that shift results take on the type of their
     * left operand, and that the types of identifiers are refined based on the results of the Value
     * Checker.
     */
    // TODO: Refine the type of expressions using the Value Checker as well.
    private class SignednessTreeAnnotator extends TreeAnnotator {

        public SignednessTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Change the type of booleans to @UnknownSignedness so that the {@link
         * PropagationTreeAnnotator} does not change the type of them.
         */
        private void annotateBoolean(AnnotatedTypeMirror type) {
            switch (type.getKind()) {
                case BOOLEAN:
                    type.addAnnotation(UNKNOWN_SIGNEDNESS);
                    break;
                default:
                    // Nothing for other cases.
            }
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            switch (tree.getKind()) {
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case UNSIGNED_RIGHT_SHIFT:
                    AnnotatedTypeMirror lht = getAnnotatedType(tree.getLeftOperand());
                    type.replaceAnnotations(lht.getAnnotations());
                    break;
                default:
                    // Do nothing
            }
            annotateBoolean(type);
            return null;
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
            annotateBoolean(type);
            return null;
        }

        // Refines the type of an integer primitive to @Constant if it is within the signed positive range
        // (i.e. its MSB is zero). Note that boxed primitives are not handled because they are not yet
        // handled by the Signedness Checker (Issue #797).
        @Override
        public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror type) {
            TypeMirror javaType = type.getUnderlyingType();
            TypeKind javaTypeKind = javaType.getKind();

            if (javaTypeKind == TypeKind.BYTE
                    || javaTypeKind == TypeKind.CHAR
                    || javaTypeKind == TypeKind.SHORT
                    || javaTypeKind == TypeKind.INT
                    || javaTypeKind == TypeKind.LONG) {
                ValueAnnotatedTypeFactory valFact = getValueAnnotatedTypeFactory();
                Range treeRange =
                        IndexUtil.getPossibleValues(valFact.getAnnotatedType(tree), valFact);

                if (treeRange != null) {
                    switch (javaType.getKind()) {
                        case BYTE:
                        case CHAR:
                            if (treeRange.isWithin(0, Byte.MAX_VALUE)) {
                                type.replaceAnnotation(CONSTANT);
                            }
                            break;
                        case SHORT:
                            if (treeRange.isWithin(0, Short.MAX_VALUE)) {
                                type.replaceAnnotation(CONSTANT);
                            }
                            break;
                        case INT:
                            if (treeRange.isWithin(0, Integer.MAX_VALUE)) {
                                type.replaceAnnotation(CONSTANT);
                            }
                            break;
                        case LONG:
                            if (treeRange.isWithin(0, Long.MAX_VALUE)) {
                                type.replaceAnnotation(CONSTANT);
                            }
                            break;
                        default:
                            // Nothing
                    }
                }
            }

            return null;
        }
    }
}
