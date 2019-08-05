package org.checkerframework.checker.signedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.SignedPositive;
import org.checkerframework.checker.signedness.qual.SignednessGlb;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;
import org.checkerframework.common.value.qual.IntRangeFromPositive;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

/** @checker_framework.manual #signedness-checker Signedness Checker */
public class SignednessAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The @SignednessGlb annotation. */
    private final AnnotationMirror SIGNEDNESS_GLB =
            AnnotationBuilder.fromClass(elements, SignednessGlb.class);
    /** The @Signed annotation. */
    private final AnnotationMirror SIGNED = AnnotationBuilder.fromClass(elements, Signed.class);
    /** The @UnknownSignedness annotation. */
    private final AnnotationMirror UNKNOWN_SIGNEDNESS =
            AnnotationBuilder.fromClass(elements, UnknownSignedness.class);

    /** The @NonNegative annotation of the Index Checker, as represented by the Value Checker. */
    private final AnnotationMirror INT_RANGE_FROM_NON_NEGATIVE =
            AnnotationBuilder.fromClass(elements, IntRangeFromNonNegative.class);
    /** The @Positive annotation of the Index Checker, as represented by the Value Checker. */
    private final AnnotationMirror INT_RANGE_FROM_POSITIVE =
            AnnotationBuilder.fromClass(elements, IntRangeFromPositive.class);

    ValueAnnotatedTypeFactory valueFactory = getTypeFactoryOfSubchecker(ValueChecker.class);

    /** Create a SignednessAnnotatedTypeFactory. */
    public SignednessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        addAliasedAnnotation(SignedPositive.class, SIGNEDNESS_GLB);

        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> result = getBundledTypeQualifiersWithoutPolyAll();
        result.remove(SignedPositive.class); // this method should not return aliases
        return result;
    }

    @Override
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        // Prevent @ImplicitFor from applying to local variables of type byte, short, int, and long,
        // but adding the top type to them, which permits flow-sensitive type refinement.
        // (When it is possible to default types based on their TypeKinds,
        // this whole method will no longer be needed.)
        addSignednessGlbAnnotation(tree, type);
        addUnknownSignednessToSomeLocals(tree, type);

        super.addComputedTypeAnnotations(tree, type, iUseFlow);
    }

    /**
     * Refines an integer expression to @SignednessGlb if its value is within the signed positive
     * range (i.e. its MSB is zero).
     *
     * @param tree an AST node, whose type may be refined
     * @param type the type of the tree
     */
    private void addSignednessGlbAnnotation(Tree tree, AnnotatedTypeMirror type) {
        TypeMirror javaType = type.getUnderlyingType();
        TypeKind javaTypeKind = javaType.getKind();
        if (tree.getKind() != Tree.Kind.VARIABLE) {
            if (javaTypeKind == TypeKind.BYTE
                    || javaTypeKind == TypeKind.CHAR
                    || javaTypeKind == TypeKind.SHORT
                    || javaTypeKind == TypeKind.INT
                    || javaTypeKind == TypeKind.LONG) {
                AnnotatedTypeMirror valueATM = valueFactory.getAnnotatedType(tree);
                // These annotations are trusted rather than checked.  Maybe have an option to
                // disable using them?
                if ((valueATM.hasAnnotation(INT_RANGE_FROM_NON_NEGATIVE)
                                || valueATM.hasAnnotation(INT_RANGE_FROM_POSITIVE))
                        && type.hasAnnotation(SIGNED)) {
                    type.replaceAnnotation(SIGNEDNESS_GLB);
                } else {
                    Range treeRange = IndexUtil.getPossibleValues(valueATM, valueFactory);

                    if (treeRange != null) {
                        switch (javaType.getKind()) {
                            case BYTE:
                            case CHAR:
                                if (treeRange.isWithin(0, Byte.MAX_VALUE)) {
                                    type.replaceAnnotation(SIGNEDNESS_GLB);
                                }
                                break;
                            case SHORT:
                                if (treeRange.isWithin(0, Short.MAX_VALUE)) {
                                    type.replaceAnnotation(SIGNEDNESS_GLB);
                                }
                                break;
                            case INT:
                                if (treeRange.isWithin(0, Integer.MAX_VALUE)) {
                                    type.replaceAnnotation(SIGNEDNESS_GLB);
                                }
                                break;
                            case LONG:
                                if (treeRange.isWithin(0, Long.MAX_VALUE)) {
                                    type.replaceAnnotation(SIGNEDNESS_GLB);
                                }
                                break;
                            default:
                                // Nothing
                        }
                    }
                }
            }
        }
    }

    /**
     * If the tree is a local variable and the type is byte, short, int, or long, then add the
     * UnknownSignedness annotation so that dataflow can refine it.
     */
    private void addUnknownSignednessToSomeLocals(Tree tree, AnnotatedTypeMirror type) {
        switch (type.getKind()) {
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
                QualifierDefaults defaults = new QualifierDefaults(elements, this);
                defaults.addCheckedCodeDefault(UNKNOWN_SIGNEDNESS, TypeUseLocation.LOCAL_VARIABLE);
                defaults.annotate(tree, type);
                break;
            default:
                // Nothing for other cases.
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new SignednessTreeAnnotator(this), super.createTreeAnnotator());
    }

    /**
     * This TreeAnnotator ensures that boolean expressions are not given Unsigned or Signed
     * annotations by {@link PropagationTreeAnnotator}, that shift results take on the type of their
     * left operand, and that the types of identifiers are refined based on the results of the Value
     * Checker.
     */
    private class SignednessTreeAnnotator extends TreeAnnotator {

        public SignednessTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Change the type of booleans to {@code @UnknownSignedness} so that the {@link
         * PropagationTreeAnnotator} does not change the type of them.
         */
        private void annotateBooleanAsUnknownSignedness(AnnotatedTypeMirror type) {
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
            annotateBooleanAsUnknownSignedness(type);
            return null;
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
            annotateBooleanAsUnknownSignedness(type);
            return null;
        }
    }
}
