package org.checkerframework.checker.signedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.SignedPositive;
import org.checkerframework.checker.signedness.qual.SignednessBottom;
import org.checkerframework.checker.signedness.qual.SignednessGlb;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;
import org.checkerframework.common.value.qual.IntRangeFromPositive;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The type factory for the Signedness Checker.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
public class SignednessAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The @UnknownSignedness annotation. */
    private final AnnotationMirror UNKNOWN_SIGNEDNESS =
            AnnotationBuilder.fromClass(elements, UnknownSignedness.class);
    /** The @Signed annotation. */
    private final AnnotationMirror SIGNED = AnnotationBuilder.fromClass(elements, Signed.class);
    /** The @Unigned annotation. */
    private final AnnotationMirror UNSIGNED = AnnotationBuilder.fromClass(elements, Unsigned.class);
    /** The @SignednessGlb annotation. */
    private final AnnotationMirror SIGNEDNESS_GLB =
            AnnotationBuilder.fromClass(elements, SignednessGlb.class);
    /** The @SignednessBottom annotation. */
    private final AnnotationMirror SIGNEDNESS_BOTTOM =
            AnnotationBuilder.fromClass(elements, SignednessBottom.class);

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

        addAliasedAnnotation("jdk.jfr.Unsigned", UNSIGNED);

        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> result = getBundledTypeQualifiers();
        result.remove(SignedPositive.class); // this method should not return aliases
        return result;
    }

    @Override
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        if (!computingAnnotatedTypeMirrorOfLHS) {
            addSignednessGlbAnnotation(tree, type);
        }

        super.addComputedTypeAnnotations(tree, type, iUseFlow);
    }

    /**
     * True when the AnnotatedTypeMirror currently being computed is the left hand side of an
     * assignment or pseudo-assignment.
     *
     * @see #addComputedTypeAnnotations(Tree, AnnotatedTypeMirror, boolean)
     * @see #getAnnotatedTypeLhs(Tree)
     */
    private boolean computingAnnotatedTypeMirrorOfLHS = false;

    @Override
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
        boolean oldComputingAnnotatedTypeMirrorOfLHS = computingAnnotatedTypeMirrorOfLHS;
        computingAnnotatedTypeMirrorOfLHS = true;
        AnnotatedTypeMirror result = super.getAnnotatedTypeLhs(lhsTree);
        computingAnnotatedTypeMirrorOfLHS = oldComputingAnnotatedTypeMirrorOfLHS;
        return result;
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
                    Range treeRange = ValueCheckerUtils.getPossibleValues(valueATM, valueFactory);

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

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new SignednessTreeAnnotator(this), super.createTreeAnnotator());
    }

    /**
     * This TreeAnnotator ensures that:
     *
     * <ul>
     *   <li>boolean expressions are not given Unsigned or Signed annotations by {@link
     *       PropagationTreeAnnotator},
     *   <li>shift results take on the type of their left operand,
     *   <li>the types of identifiers are refined based on the results of the Value Checker.
     * </ul>
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

    @Override
    protected void addAnnotationsFromDefaultForType(
            @Nullable Element element, AnnotatedTypeMirror type) {
        if (TypesUtils.isFloatingPrimitive(type.getUnderlyingType())
                || TypesUtils.isBoxedFloating(type.getUnderlyingType())
                || type.getKind() == TypeKind.CHAR
                || TypesUtils.isDeclaredOfName(type.getUnderlyingType(), "java.lang.Character")) {
            // Floats are always signed and chars are always unsigned.
            super.addAnnotationsFromDefaultForType(null, type);
        } else {
            super.addAnnotationsFromDefaultForType(element, type);
        }
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new SignednessTypeHierarchy(
                checker,
                getQualifierHierarchy(),
                checker.getBooleanOption("ignoreRawTypeArguments", true),
                checker.hasOption("invariantArrays"));
    }

    /**
     * The type hierarchy for the signedness type system. If A is narrower (fewer bits) than B, then
     * A with any qualifier is a subtype of @SignedPositive B.
     */
    protected class SignednessTypeHierarchy extends DefaultTypeHierarchy {

        /**
         * Create a new SignednessTypeHierarchy.
         *
         * @param checker the checker
         * @param qualifierHierarchy the qualifier hierarchy
         * @param ignoreRawTypes from -AignoreRawTypes
         * @param invariantArrayComponents from -AinvariantArrays
         */
        public SignednessTypeHierarchy(
                BaseTypeChecker checker,
                QualifierHierarchy qualifierHierarchy,
                boolean ignoreRawTypes,
                boolean invariantArrayComponents) {
            super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
        }

        @Override
        public Boolean visitPrimitive_Primitive(
                AnnotatedPrimitiveType subtype, AnnotatedPrimitiveType supertype, Void p) {

            boolean superResult = super.visitPrimitive_Primitive(subtype, supertype, p);
            if (superResult) {
                return true;
            }

            PrimitiveType subPrimitive = subtype.getUnderlyingType();
            PrimitiveType superPrimitive = supertype.getUnderlyingType();
            if (TypesUtils.isNarrowerIntegral(subPrimitive, superPrimitive)) {
                AnnotationMirror superAnno = supertype.getAnnotationInHierarchy(UNKNOWN_SIGNEDNESS);
                if (!AnnotationUtils.areSameByName(superAnno, SIGNEDNESS_BOTTOM)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Boolean visitPrimitive_Declared(
                AnnotatedPrimitiveType subtype, AnnotatedDeclaredType supertype, Void p) {
            boolean superBoxed = TypesUtils.isBoxedPrimitive(supertype.getUnderlyingType());
            if (superBoxed) {
                return visitPrimitive_Primitive(subtype, getUnboxedType(supertype), p);
            }
            return super.visitPrimitive_Declared(subtype, supertype, p);
        }

        @Override
        public Boolean visitDeclared_Declared(
                AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype, Void p) {
            boolean subBoxed = TypesUtils.isBoxedPrimitive(subtype.getUnderlyingType());
            if (subBoxed) {
                boolean superBoxed = TypesUtils.isBoxedPrimitive(supertype.getUnderlyingType());
                if (superBoxed) {
                    return visitPrimitive_Primitive(
                            getUnboxedType(subtype), getUnboxedType(supertype), p);
                }
            }
            return super.visitDeclared_Declared(subtype, supertype, p);
        }

        @Override
        public Boolean visitDeclared_Primitive(
                AnnotatedDeclaredType subtype, AnnotatedPrimitiveType supertype, Void p) {
            boolean subBoxed = TypesUtils.isBoxedPrimitive(subtype.getUnderlyingType());
            if (subBoxed) {
                return visitPrimitive_Primitive(getUnboxedType(subtype), supertype, p);
            }
            return super.visitDeclared_Primitive(subtype, supertype, p);
        }
    }
}
