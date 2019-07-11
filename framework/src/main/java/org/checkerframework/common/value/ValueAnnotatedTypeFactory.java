package org.checkerframework.common.value;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntRangeFromGTENegativeOne;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;
import org.checkerframework.common.value.qual.IntRangeFromPositive;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.MinLenFieldInvariant;
import org.checkerframework.common.value.qual.PolyValue;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.StructuralEqualityComparer;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.FieldInvariants;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** AnnotatedTypeFactory for the Value type system. */
public class ValueAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The maximum number of values allowed in an annotation's array. */
    protected static final int MAX_VALUES = 10;

    /**
     * The domain of the Constant Value Checker: the types for which it estimates possible values.
     */
    protected static final Set<String> COVERED_CLASS_STRINGS =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    "int",
                                    "java.lang.Integer",
                                    "double",
                                    "java.lang.Double",
                                    "byte",
                                    "java.lang.Byte",
                                    "java.lang.String",
                                    "char",
                                    "java.lang.Character",
                                    "float",
                                    "java.lang.Float",
                                    "boolean",
                                    "java.lang.Boolean",
                                    "long",
                                    "java.lang.Long",
                                    "short",
                                    "java.lang.Short",
                                    "char[]")));

    /** The top type for this hierarchy. */
    protected final AnnotationMirror UNKNOWNVAL =
            AnnotationBuilder.fromClass(elements, UnknownVal.class);

    /** The bottom type for this hierarchy. */
    protected final AnnotationMirror BOTTOMVAL =
            AnnotationBuilder.fromClass(elements, BottomVal.class);

    /** The canonical @{@link PolyValue} annotation. */
    public final AnnotationMirror POLY = AnnotationBuilder.fromClass(elements, PolyValue.class);

    /** Should this type factory report warnings? */
    private final boolean reportEvalWarnings;

    /** Helper class that evaluates statically executable methods, constructors, and fields. */
    private final ReflectiveEvaluator evaluator;

    /** Helper class that holds references to special methods. */
    private final ValueMethodIdentifier methods;

    public ValueAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        reportEvalWarnings = checker.hasOption(ValueChecker.REPORT_EVAL_WARNS);
        Range.ignoreOverflow = checker.hasOption(ValueChecker.IGNORE_RANGE_OVERFLOW);
        evaluator = new ReflectiveEvaluator(checker, this, reportEvalWarnings);

        addAliasedAnnotation("android.support.annotation.IntRange", IntRange.class, true);

        // The actual ArrayLenRange is created by
        // {@link ValueAnnotatedTypeFactory#canonicalAnnotation(AnnotationMirror)};
        // this line just registers the alias. The BottomVal is never used.
        addAliasedAnnotation(MinLen.class, BOTTOMVAL);

        // @Positive is aliased here because @Positive provides useful
        // information about @MinLen annotations.
        // @NonNegative and @GTENegativeOne are aliased similarly so
        // that it's possible to overwrite a function annotated to return
        // @NonNegative with, for instance, a function that returns an @IntVal(0).
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.Positive", createIntRangeFromPositive());
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.NonNegative",
                createIntRangeFromNonNegative());
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.GTENegativeOne",
                createIntRangeFromGTENegativeOne());
        // Must also alias any alias of three annotations above:
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.LengthOf",
                createIntRangeFromNonNegative());
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.IndexFor",
                createIntRangeFromNonNegative());
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.IndexOrHigh",
                createIntRangeFromNonNegative());
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.IndexOrLow",
                createIntRangeFromGTENegativeOne());
        addAliasedAnnotation(
                "org.checkerframework.checker.index.qual.SubstringIndexFor",
                createIntRangeFromGTENegativeOne());

        // PolyLength is syntactic sugar for both @PolySameLen and @PolyValue
        addAliasedAnnotation("org.checkerframework.checker.index.qual.PolyLength", POLY);

        methods = new ValueMethodIdentifier(processingEnv);

        if (this.getClass().equals(ValueAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    /** Gets a helper object that holds references to methods with special handling. */
    ValueMethodIdentifier getMethodIdentifier() {
        return methods;
    }

    @Override
    public AnnotationMirror canonicalAnnotation(AnnotationMirror anno) {
        if (AnnotationUtils.areSameByClass(anno, MinLen.class)) {
            int from = getMinLenValue(anno);
            return createArrayLenRangeAnnotation(from, Integer.MAX_VALUE);
        }

        return super.canonicalAnnotation(anno);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // Because the Value Checker includes its own alias annotations,
        // the qualifiers have to be explicitly defined.
        return new LinkedHashSet<>(
                Arrays.asList(
                        ArrayLen.class,
                        ArrayLenRange.class,
                        IntVal.class,
                        IntRange.class,
                        BoolVal.class,
                        StringVal.class,
                        DoubleVal.class,
                        BottomVal.class,
                        UnknownVal.class,
                        IntRangeFromPositive.class,
                        IntRangeFromNonNegative.class,
                        IntRangeFromGTENegativeOne.class,
                        PolyValue.class,
                        PolyAll.class));
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new ValueTransfer(analysis);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new ValueQualifierHierarchy(factory);
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        // This is a lot of code to replace annotations so that annotations that are equivalent
        // qualifiers are the same annotation.
        return new DefaultTypeHierarchy(
                checker,
                getQualifierHierarchy(),
                checker.getBooleanOption("ignoreRawTypeArguments", true),
                checker.hasOption("invariantArrays")) {
            @Override
            public StructuralEqualityComparer createEqualityComparer() {
                return new StructuralEqualityComparer(typeargVisitHistory) {
                    @Override
                    protected boolean arePrimeAnnosEqual(
                            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
                        type1.replaceAnnotation(
                                convertSpecialIntRangeToStandardIntRange(
                                        type1.getAnnotationInHierarchy(UNKNOWNVAL)));
                        type2.replaceAnnotation(
                                convertSpecialIntRangeToStandardIntRange(
                                        type2.getAnnotationInHierarchy(UNKNOWNVAL)));
                        type1.replaceAnnotation(
                                convertToUnknown(type1.getAnnotationInHierarchy(UNKNOWNVAL)));
                        type2.replaceAnnotation(
                                convertToUnknown(type2.getAnnotationInHierarchy(UNKNOWNVAL)));

                        return super.arePrimeAnnosEqual(type1, type2);
                    }
                };
            }
        };
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(new ValueTypeAnnotator(this), super.createTypeAnnotator());
    }

    @Override
    public FieldInvariants getFieldInvariants(TypeElement element) {
        AnnotationMirror fieldInvarAnno = getDeclAnnotation(element, MinLenFieldInvariant.class);
        if (fieldInvarAnno == null) {
            return null;
        }
        List<String> fields =
                AnnotationUtils.getElementValueArray(fieldInvarAnno, "field", String.class, true);
        List<Integer> minlens =
                AnnotationUtils.getElementValueArray(fieldInvarAnno, "minLen", Integer.class, true);
        List<AnnotationMirror> qualifiers = new ArrayList<>();
        for (Integer minlen : minlens) {
            qualifiers.add(createArrayLenRangeAnnotation(minlen, Integer.MAX_VALUE));
        }

        FieldInvariants superInvariants = super.getFieldInvariants(element);
        return new FieldInvariants(superInvariants, fields, qualifiers);
    }

    @Override
    protected Set<Class<? extends Annotation>> getFieldInvariantDeclarationAnnotations() {
        // include FieldInvariant so that @MinLenBottom can be used.
        Set<Class<? extends Annotation>> set =
                new HashSet<>(super.getFieldInvariantDeclarationAnnotations());
        set.add(MinLenFieldInvariant.class);
        return set;
    }

    /**
     * Creates array length annotations for the result of the Enum.values() method, which is the
     * number of possible values of the enum.
     */
    @Override
    public ParameterizedExecutableType methodFromUse(
            ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {

        ParameterizedExecutableType superPair = super.methodFromUse(tree, methodElt, receiverType);
        if (ElementUtils.matchesElement(methodElt, "values")
                && methodElt.getEnclosingElement().getKind() == ElementKind.ENUM
                && ElementUtils.isStatic(methodElt)) {
            int count = 0;
            List<? extends Element> l = methodElt.getEnclosingElement().getEnclosedElements();
            for (Element el : l) {
                if (el.getKind() == ElementKind.ENUM_CONSTANT) {
                    count++;
                }
            }
            AnnotationMirror am = createArrayLenAnnotation(Collections.singletonList(count));
            superPair.executableType.getReturnType().replaceAnnotation(am);
        }
        return superPair;
    }

    /**
     * Finds the appropriate value for the {@code from} value of an annotated type mirror containing
     * an {@code IntRange} annotation.
     *
     * @param atm an annotated type mirror that contains an {@code IntRange} annotation.
     * @return either the from value from the passed int range annotation, or the minimum value of
     *     the domain of the underlying type (i.e. Integer.MIN_VALUE if the underlying type is int)
     */
    public long getFromValueFromIntRange(AnnotatedTypeMirror atm) {
        AnnotationMirror anno = atm.getAnnotation(IntRange.class);

        if (AnnotationUtils.hasElementValue(anno, "from")) {
            return AnnotationUtils.getElementValue(anno, "from", Long.class, false);
        }

        long from;
        switch (atm.getUnderlyingType().getKind()) {
            case INT:
                from = Integer.MIN_VALUE;
                break;
            case SHORT:
                from = Short.MIN_VALUE;
                break;
            case BYTE:
                from = Byte.MIN_VALUE;
                break;
            case CHAR:
                from = Character.MIN_VALUE;
                break;
            default:
                from = Long.MIN_VALUE;
        }
        return from;
    }

    /**
     * Finds the appropriate value for the {@code to} value of an annotated type mirror containing
     * an {@code IntRange} annotation.
     *
     * @param atm an annotated type mirror that contains an {@code IntRange} annotation.
     * @return either the to value from the passed int range annotation, or the maximum value of the
     *     domain of the underlying type (i.e. Integer.MAX_VALUE if the underlying type is int)
     */
    public long getToValueFromIntRange(AnnotatedTypeMirror atm) {
        AnnotationMirror anno = atm.getAnnotation(IntRange.class);

        if (AnnotationUtils.hasElementValue(anno, "to")) {
            return AnnotationUtils.getElementValue(anno, "to", Long.class, false);
        }

        long to;
        switch (atm.getUnderlyingType().getKind()) {
            case INT:
                to = Integer.MAX_VALUE;
                break;
            case SHORT:
                to = Short.MAX_VALUE;
                break;
            case BYTE:
                to = Byte.MAX_VALUE;
                break;
            case CHAR:
                to = Character.MAX_VALUE;
                break;
            default:
                to = Long.MAX_VALUE;
        }
        return to;
    }

    /**
     * Performs pre-processing on annotations written by users, replacing illegal annotations by
     * legal ones.
     */
    protected class ValueTypeAnnotator extends TypeAnnotator {

        /** Construct a new ValueTypeAnnotator */
        protected ValueTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
            replaceWithNewAnnoInSpecialCases(type);
            return super.scan(type, aVoid);
        }

        /**
         * This method performs pre-processing on annotations written by users.
         *
         * <p>If any *Val annotation has &gt; MAX_VALUES number of values provided, replaces the
         * annotation by @IntRange for integral types, @ArrayLenRange for arrays, @ArrayLen
         * or @ArrayLenRange for strings, and @UnknownVal for all other types. Works together with
         * {@link
         * org.checkerframework.common.value.ValueVisitor#visitAnnotation(com.sun.source.tree.AnnotationTree,
         * Void)} which issues warnings to users in these cases.
         *
         * <p>If any @IntRange or @ArrayLenRange annotation has incorrect parameters, e.g. the value
         * "from" is greater than the value "to", replaces the annotation by {@code @BottomVal}. The
         * {@link
         * org.checkerframework.common.value.ValueVisitor#visitAnnotation(com.sun.source.tree.AnnotationTree,
         * Void)} raises an error to users if the annotation was user-written.
         *
         * <p>If any @ArrayLen annotation has a negative number, replaces the annotation by {@code
         * BottomVal}. The {@link
         * org.checkerframework.common.value.ValueVisitor#visitAnnotation(com.sun.source.tree.AnnotationTree,
         * Void)} raises an error to users if the annotation was user-written.
         *
         * <p>If a user only writes one side of an {@code IntRange} annotation, this method also
         * computes an appropriate default based on the underlying type for the other side of the
         * range. For instance, if the user write {@code @IntRange(from = 1) short x;} then this
         * method will translate the annotation to {@code @IntRange(from = 1, to = Short.MAX_VALUE}.
         */
        private void replaceWithNewAnnoInSpecialCases(AnnotatedTypeMirror atm) {
            AnnotationMirror anno = atm.getAnnotationInHierarchy(UNKNOWNVAL);
            if (anno == null || anno.getElementValues().isEmpty()) {
                return;
            }

            if (AnnotationUtils.areSameByClass(anno, IntVal.class)) {
                List<Long> values = getIntValues(anno);
                if (values.size() > MAX_VALUES) {
                    long annoMinVal = Collections.min(values);
                    long annoMaxVal = Collections.max(values);
                    atm.replaceAnnotation(
                            createIntRangeAnnotation(new Range(annoMinVal, annoMaxVal)));
                }
            } else if (AnnotationUtils.areSameByClass(anno, ArrayLen.class)) {
                List<Integer> values = getArrayLength(anno);
                if (values.isEmpty()) {
                    atm.replaceAnnotation(BOTTOMVAL);
                } else if (Collections.min(values) < 0) {
                    atm.replaceAnnotation(BOTTOMVAL);
                } else if (values.size() > MAX_VALUES) {
                    long annoMinVal = Collections.min(values);
                    long annoMaxVal = Collections.max(values);
                    atm.replaceAnnotation(
                            createArrayLenRangeAnnotation(new Range(annoMinVal, annoMaxVal)));
                }
            } else if (AnnotationUtils.areSameByClass(anno, IntRange.class)) {
                // Compute appropriate defaults for integral ranges.
                long from = getFromValueFromIntRange(atm);
                long to = getToValueFromIntRange(atm);

                if (from > to) {
                    // from > to either indicates a user error when writing an
                    // annotation or an error in the checker's implementation -
                    // from should always be <= to. ValueVisitor#validateType will
                    // issue an error.
                    atm.replaceAnnotation(BOTTOMVAL);
                } else {
                    // Always do a replacement of the annotation here so that
                    // the defaults calculated above are correctly added to the
                    // annotation (assuming the annotation is well-formed).
                    atm.replaceAnnotation(createIntRangeAnnotation(from, to));
                }
            } else if (AnnotationUtils.areSameByClass(anno, ArrayLenRange.class)) {
                int from = AnnotationUtils.getElementValue(anno, "from", Integer.class, true);
                int to = AnnotationUtils.getElementValue(anno, "to", Integer.class, true);
                if (from > to) {
                    // from > to either indicates a user error when writing an
                    // annotation or an error in the checker's implementation -
                    // from should always be <= to. ValueVisitor#validateType will
                    // issue an error.
                    atm.replaceAnnotation(BOTTOMVAL);
                } else if (from < 0) {
                    // No array can have a length less than 0. Any time the type includes a from
                    // less than zero, it must indicate imprecision in the checker.
                    atm.replaceAnnotation(createArrayLenRangeAnnotation(0, to));
                }
            } else if (AnnotationUtils.areSameByClass(anno, StringVal.class)) {
                // The annotation is StringVal. If there are too many elements,
                // ArrayLen or ArrayLenRange is used.
                List<String> values = getStringValues(anno);

                if (values.size() > MAX_VALUES) {
                    List<Integer> lengths = ValueCheckerUtils.getLengthsForStringValues(values);
                    atm.replaceAnnotation(createArrayLenAnnotation(lengths));
                }

            } else {
                // In here the annotation is @*Val where (*) is not Int, String but other types
                // (Double, etc).
                // Therefore we extract its values in a generic way to check its size.
                List<Object> values =
                        AnnotationUtils.getElementValueArray(anno, "value", Object.class, false);
                if (values.size() > MAX_VALUES) {
                    atm.replaceAnnotation(UNKNOWNVAL);
                }
            }
        }
    }

    /** The qualifier hierarchy for the Value type system. */
    private final class ValueQualifierHierarchy extends MultiGraphQualifierHierarchy {

        /** @param factory MultiGraphFactory to use to construct this */
        public ValueQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        /**
         * Computes greatest lower bound of a @StringVal annotation with another value checker
         * annotation.
         *
         * @param stringValAnno annotation of type @StringVal
         * @param otherAnno annotation from the value checker hierarchy
         * @return greatest lower bound of {@code stringValAnno} and {@code otherAnno}
         */
        private AnnotationMirror glbOfStringVal(
                AnnotationMirror stringValAnno, AnnotationMirror otherAnno) {
            List<String> values = getStringValues(stringValAnno);

            if (AnnotationUtils.areSameByClass(otherAnno, StringVal.class)) {
                // Intersection of value lists
                List<String> otherValues = getStringValues(otherAnno);

                values.retainAll(otherValues);
            } else if (AnnotationUtils.areSameByClass(otherAnno, ArrayLen.class)) {
                // Retain strings of correct lengths

                List<Integer> otherLengths = getArrayLength(otherAnno);

                ArrayList<String> result = new ArrayList<>();
                for (String s : values) {
                    if (otherLengths.contains(s.length())) {
                        result.add(s);
                    }
                }
                values = result;
            } else if (AnnotationUtils.areSameByClass(otherAnno, ArrayLenRange.class)) {
                // Retain strings of lengths from a range

                Range otherRange = getRange(otherAnno);

                ArrayList<String> result = new ArrayList<>();
                for (String s : values) {
                    if (otherRange.contains(s.length())) {
                        result.add(s);
                    }
                }
                values = result;
            } else {
                return BOTTOMVAL;
            }

            return createStringAnnotation(values);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            } else {

                // Implementation of GLB where one of the annotations is StringVal is needed for
                // length-based refinement of constant string values. Other cases of length-based
                // refinement are handled by subtype check.
                if (AnnotationUtils.areSameByClass(a1, StringVal.class)) {
                    return glbOfStringVal(a1, a2);
                } else if (AnnotationUtils.areSameByClass(a2, StringVal.class)) {
                    return glbOfStringVal(a2, a1);
                }

                // Simply return BOTTOMVAL in other cases. Refine this if we discover use cases
                // that need a more precise GLB.
                return BOTTOMVAL;
            }
        }

        @Override
        public int numberOfIterationsBeforeWidening() {
            return MAX_VALUES + 1;
        }

        @Override
        public AnnotationMirror widenedUpperBound(
                AnnotationMirror newQualifier, AnnotationMirror previousQualifier) {
            AnnotationMirror lub = leastUpperBound(newQualifier, previousQualifier);
            if (AnnotationUtils.areSameByClass(lub, IntRange.class)) {
                Range lubRange = getRange(lub);
                Range newRange = getRange(newQualifier);
                Range oldRange = getRange(previousQualifier);
                Range wubRange = widenedRange(newRange, oldRange, lubRange);
                return createIntRangeAnnotation(wubRange);
            } else if (AnnotationUtils.areSameByClass(lub, ArrayLenRange.class)) {
                Range lubRange = getRange(lub);
                Range newRange = getRange(newQualifier);
                Range oldRange = getRange(previousQualifier);
                return createArrayLenRangeAnnotation(widenedRange(newRange, oldRange, lubRange));
            } else {
                return lub;
            }
        }

        private Range widenedRange(Range newRange, Range oldRange, Range lubRange) {
            if (newRange == null || oldRange == null || lubRange.equals(oldRange)) {
                return lubRange;
            }
            // If both bounds of the new range are bigger than the old range, then returned range
            // should use the lower bound of the new range and a MAX_VALUE.
            if ((newRange.from >= oldRange.from && newRange.to >= oldRange.to)) {
                if (lubRange.to < Byte.MAX_VALUE) {
                    return new Range(newRange.from, Byte.MAX_VALUE);
                } else if (lubRange.to < Short.MAX_VALUE) {
                    return new Range(newRange.from, Short.MAX_VALUE);
                } else if (lubRange.to < Integer.MAX_VALUE) {
                    return new Range(newRange.from, Integer.MAX_VALUE);
                } else {
                    return new Range(newRange.from, Long.MAX_VALUE);
                }
            }

            // If both bounds of the old range are bigger than the new range, then returned range
            // should use a MIN_VALUE and the upper bound of the new range.
            if ((newRange.from <= oldRange.from && newRange.to <= oldRange.to)) {
                if (lubRange.from > Byte.MIN_VALUE) {
                    return new Range(Byte.MIN_VALUE, newRange.to);
                } else if (lubRange.from > Short.MIN_VALUE) {
                    return new Range(Short.MIN_VALUE, newRange.to);
                } else if (lubRange.from > Integer.MIN_VALUE) {
                    return new Range(Integer.MIN_VALUE, newRange.to);
                } else {
                    return new Range(Long.MIN_VALUE, newRange.to);
                }
            }

            if (lubRange.isWithin(Byte.MIN_VALUE + 1, Byte.MAX_VALUE)
                    || lubRange.isWithin(Byte.MIN_VALUE, Byte.MAX_VALUE - 1)) {
                return Range.BYTE_EVERYTHING;
            } else if (lubRange.isWithin(Short.MIN_VALUE + 1, Short.MAX_VALUE)
                    || lubRange.isWithin(Short.MIN_VALUE, Short.MAX_VALUE - 1)) {
                return Range.SHORT_EVERYTHING;
            } else if (lubRange.isWithin(Long.MIN_VALUE + 1, Long.MAX_VALUE)
                    || lubRange.isWithin(Long.MIN_VALUE, Long.MAX_VALUE - 1)) {
                return Range.INT_EVERYTHING;
            } else {
                return Range.EVERYTHING;
            }
        }

        /**
         * Determines the least upper bound of a1 and a2, which contains the union of their sets of
         * possible values.
         *
         * @return the least upper bound of a1 and a2
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (!AnnotationUtils.areSameByName(getTopAnnotation(a1), getTopAnnotation(a2))) {
                // The annotations are in different hierarchies
                return null;
            }

            a1 = convertSpecialIntRangeToStandardIntRange(a1);
            a2 = convertSpecialIntRangeToStandardIntRange(a2);

            if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            }

            if (AnnotationUtils.areSameByName(a1, a2)) {
                // If both are the same type, determine the type and merge
                if (AnnotationUtils.areSameByClass(a1, IntRange.class)) {
                    // special handling for IntRange
                    Range range1 = getRange(a1);
                    Range range2 = getRange(a2);
                    return createIntRangeAnnotation(range1.union(range2));
                } else if (AnnotationUtils.areSameByClass(a1, ArrayLenRange.class)) {
                    // special handling for ArrayLenRange
                    Range range1 = getRange(a1);
                    Range range2 = getRange(a2);
                    return createArrayLenRangeAnnotation(range1.union(range2));
                } else if (AnnotationUtils.areSameByClass(a1, IntVal.class)) {
                    List<Long> a1Values = getIntValues(a1);
                    List<Long> a2Values = getIntValues(a2);
                    List<Long> newValues = new ArrayList<>();
                    newValues.addAll(a1Values);
                    newValues.addAll(a2Values);
                    return createIntValAnnotation(newValues);
                } else if (AnnotationUtils.areSameByClass(a1, ArrayLen.class)) {
                    List<Integer> a1Values = getArrayLength(a1);
                    List<Integer> a2Values = getArrayLength(a2);
                    List<Integer> newValues = new ArrayList<>();
                    newValues.addAll(a1Values);
                    newValues.addAll(a2Values);
                    return createArrayLenAnnotation(newValues);
                } else if (AnnotationUtils.areSameByClass(a1, StringVal.class)) {
                    List<String> a1Values = getStringValues(a1);
                    List<String> a2Values = getStringValues(a2);
                    List<String> newValues = new ArrayList<>();
                    newValues.addAll(a1Values);
                    newValues.addAll(a2Values);
                    return createStringAnnotation(newValues);
                } else {
                    List<Object> a1Values =
                            AnnotationUtils.getElementValueArray(a1, "value", Object.class, true);
                    List<Object> a2Values =
                            AnnotationUtils.getElementValueArray(a2, "value", Object.class, true);
                    TreeSet<Object> newValues = new TreeSet<>();
                    newValues.addAll(a1Values);
                    newValues.addAll(a2Values);

                    if (newValues.isEmpty()) {
                        return BOTTOMVAL;
                    }
                    if (newValues.size() > MAX_VALUES) {
                        return UNKNOWNVAL;
                    }
                    AnnotationBuilder builder =
                            new AnnotationBuilder(processingEnv, a1.getAnnotationType().toString());
                    List<Object> valuesList = new ArrayList<>(newValues);
                    builder.setValue("value", valuesList);
                    return builder.build();
                }
            }

            // Special handling for dealing with the lub of an ArrayLenRange and an ArrayLen
            // or a StringVal with one of them.

            AnnotationMirror arrayLenAnno = null;
            AnnotationMirror arrayLenRangeAnno = null;
            AnnotationMirror stringValAnno = null;

            if (AnnotationUtils.areSameByClass(a1, ArrayLen.class)) {
                arrayLenAnno = a1;
            } else if (AnnotationUtils.areSameByClass(a2, ArrayLen.class)) {
                arrayLenAnno = a2;
            }
            if (AnnotationUtils.areSameByClass(a1, ArrayLenRange.class)) {
                arrayLenRangeAnno = a1;
            } else if (AnnotationUtils.areSameByClass(a2, ArrayLenRange.class)) {
                arrayLenRangeAnno = a2;
            }
            if (AnnotationUtils.areSameByClass(a1, StringVal.class)) {
                stringValAnno = a1;
            } else if (AnnotationUtils.areSameByClass(a2, StringVal.class)) {
                stringValAnno = a2;
            }

            if (arrayLenAnno != null && arrayLenRangeAnno != null) {
                return leastUpperBound(
                        arrayLenRangeAnno, convertArrayLenToArrayLenRange(arrayLenAnno));
            } else if (stringValAnno != null && arrayLenAnno != null) {
                return leastUpperBound(arrayLenAnno, convertStringValToArrayLen(stringValAnno));
            } else if (stringValAnno != null && arrayLenRangeAnno != null) {
                return leastUpperBound(
                        arrayLenRangeAnno, convertStringValToArrayLenRange(stringValAnno));
            }

            // Annotations are both in the same hierarchy, but they are not the same.
            // If a1 and a2 are not the same type of *Value annotation, they may still be mergeable
            // because some values can be implicitly cast as others. For example, if a1 and a2 are
            // both in {DoubleVal, IntVal} then they will be converted upwards: IntVal -> DoubleVal
            // to arrive at a common annotation type.

            // Each of these variables is an annotation of the given type, or is null if neither of
            // the arguments to leastUpperBound is of the given types.
            AnnotationMirror intValAnno = null;
            AnnotationMirror intRangeAnno = null;
            AnnotationMirror doubleValAnno = null;
            if (AnnotationUtils.areSameByClass(a1, IntVal.class)) {
                intValAnno = a1;
            } else if (AnnotationUtils.areSameByClass(a2, IntVal.class)) {
                intValAnno = a2;
            }
            if (AnnotationUtils.areSameByClass(a1, DoubleVal.class)) {
                doubleValAnno = a1;
            } else if (AnnotationUtils.areSameByClass(a2, DoubleVal.class)) {
                doubleValAnno = a2;
            }
            if (AnnotationUtils.areSameByClass(a1, IntRange.class)) {
                intRangeAnno = a1;
            } else if (AnnotationUtils.areSameByClass(a2, IntRange.class)) {
                intRangeAnno = a2;
            }

            if (doubleValAnno != null) {
                if (intRangeAnno != null) {
                    intValAnno = convertIntRangeToIntVal(intRangeAnno);
                    intRangeAnno = null;
                    if (intValAnno == UNKNOWNVAL) {
                        intValAnno = null;
                    }
                }
                if (intValAnno != null) {
                    // Convert intValAnno to a @DoubleVal AnnotationMirror
                    AnnotationMirror doubleValAnno2 = convertIntValToDoubleVal(intValAnno);
                    return leastUpperBound(doubleValAnno, doubleValAnno2);
                }
                return UNKNOWNVAL;
            }
            if (intRangeAnno != null && intValAnno != null) {
                // Convert intValAnno to an @IntRange AnnotationMirror
                AnnotationMirror intRangeAnno2 = convertIntValToIntRange(intValAnno);
                return leastUpperBound(intRangeAnno, intRangeAnno2);
            }

            // In all other cases, the LUB is UnknownVal.
            return UNKNOWNVAL;
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
         * annotations are Value. In this case, subAnno is a subtype of superAnno iff superAnno
         * contains at least every element of subAnno.
         *
         * @return true if subAnno is a subtype of superAnno, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {

            subAnno = convertSpecialIntRangeToStandardIntRange(subAnno);
            superAnno = convertSpecialIntRangeToStandardIntRange(superAnno);
            if (AnnotationUtils.areSameByClass(subAnno, UnknownVal.class)) {
                superAnno = convertToUnknown(superAnno);
            }

            if (AnnotationUtils.areSameByClass(superAnno, UnknownVal.class)
                    || AnnotationUtils.areSameByClass(subAnno, BottomVal.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(subAnno, UnknownVal.class)
                    || AnnotationUtils.areSameByClass(superAnno, BottomVal.class)) {
                return false;
            } else if (AnnotationUtils.areSameByClass(subAnno, PolyValue.class)) {
                return AnnotationUtils.areSameByClass(superAnno, PolyValue.class);
            } else if (AnnotationUtils.areSameByClass(superAnno, PolyValue.class)) {
                return AnnotationUtils.areSameByClass(subAnno, PolyValue.class);
            } else if (AnnotationUtils.areSameByName(superAnno, subAnno)) {
                // Same type, so might be subtype
                if (AnnotationUtils.areSameByClass(subAnno, IntRange.class)
                        || AnnotationUtils.areSameByClass(subAnno, ArrayLenRange.class)) {
                    // Special case for range-based annotations
                    Range superRange = getRange(superAnno);
                    Range subRange = getRange(subAnno);
                    return superRange.contains(subRange);
                } else {
                    List<Object> superValues =
                            AnnotationUtils.getElementValueArray(
                                    superAnno, "value", Object.class, true);
                    List<Object> subValues =
                            AnnotationUtils.getElementValueArray(
                                    subAnno, "value", Object.class, true);
                    return superValues.containsAll(subValues);
                }
            } else if (AnnotationUtils.areSameByClass(superAnno, DoubleVal.class)
                    && AnnotationUtils.areSameByClass(subAnno, IntVal.class)) {
                List<Double> subValues = convertLongListToDoubleList(getIntValues(subAnno));
                List<Double> superValues = getDoubleValues(superAnno);
                return superValues.containsAll(subValues);
            } else if ((AnnotationUtils.areSameByClass(superAnno, IntRange.class)
                            && AnnotationUtils.areSameByClass(subAnno, IntVal.class))
                    || (AnnotationUtils.areSameByClass(superAnno, ArrayLenRange.class)
                            && AnnotationUtils.areSameByClass(subAnno, ArrayLen.class))) {
                List<Long> subValues = getArrayLenOrIntValue(subAnno);
                Range superRange = getRange(superAnno);
                long subMinVal = Collections.min(subValues);
                long subMaxVal = Collections.max(subValues);
                return subMinVal >= superRange.from && subMaxVal <= superRange.to;
            } else if (AnnotationUtils.areSameByClass(superAnno, DoubleVal.class)
                    && AnnotationUtils.areSameByClass(subAnno, IntRange.class)) {
                Range subRange = getRange(subAnno);
                if (subRange.isWiderThan(MAX_VALUES)) {
                    return false;
                }
                List<Double> superValues = getDoubleValues(superAnno);
                List<Double> subValues =
                        ValueCheckerUtils.getValuesFromRange(subRange, Double.class);
                return superValues.containsAll(subValues);
            } else if ((AnnotationUtils.areSameByClass(superAnno, IntVal.class)
                            && AnnotationUtils.areSameByClass(subAnno, IntRange.class))
                    || (AnnotationUtils.areSameByClass(superAnno, ArrayLen.class)
                            && AnnotationUtils.areSameByClass(subAnno, ArrayLenRange.class))) {
                Range subRange = getRange(subAnno);
                if (subRange.isWiderThan(MAX_VALUES)) {
                    return false;
                }
                List<Long> superValues = getArrayLenOrIntValue(superAnno);
                List<Long> subValues = ValueCheckerUtils.getValuesFromRange(subRange, Long.class);
                return superValues.containsAll(subValues);
            } else if (AnnotationUtils.areSameByClass(superAnno, StringVal.class)
                    && (AnnotationUtils.areSameByClass(subAnno, ArrayLen.class)
                            || AnnotationUtils.areSameByClass(subAnno, ArrayLenRange.class))) {

                // Allow @ArrayLen(0) to be converted to @StringVal("")
                List<String> superValues = getStringValues(superAnno);
                return superValues.contains("") && getMaxLenValue(subAnno) == 0;
            } else if (AnnotationUtils.areSameByClass(superAnno, ArrayLen.class)
                    && AnnotationUtils.areSameByClass(subAnno, StringVal.class)) {
                // StringVal is a subtype of ArrayLen, if all the strings have one of the correct
                // lengths
                List<String> subValues = getStringValues(subAnno);
                List<Integer> superValues = getArrayLength(superAnno);

                for (String value : subValues) {
                    if (!superValues.contains(value.length())) {
                        return false;
                    }
                }
                return true;
            } else if (AnnotationUtils.areSameByClass(superAnno, ArrayLenRange.class)
                    && AnnotationUtils.areSameByClass(subAnno, StringVal.class)) {
                // StringVal is a subtype of ArrayLenRange, if all the strings have a length in the
                // range.
                List<String> subValues = getStringValues(subAnno);
                Range superRange = getRange(superAnno);
                for (String value : subValues) {
                    if (!superRange.contains(value.length())) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Gets the values stored in either an ArrayLen annotation (ints) or an IntVal/DoubleVal/etc.
     * annotation (longs), and casts the result to a long.
     */
    private List<Long> getArrayLenOrIntValue(AnnotationMirror anno) {
        List<Long> result;
        if (AnnotationUtils.areSameByClass(anno, ArrayLen.class)) {
            List<Integer> intValues = getArrayLength(anno);
            result = new ArrayList<>(intValues.size());
            for (Integer i : intValues) {
                result.add(i.longValue());
            }
        } else {
            result = getIntValues(anno);
        }
        return result;
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        // Don't call super.createTreeAnnotator because it includes the PropagationTreeAnnotator.
        // Only use the PropagationTreeAnnotator for typing new arrays.  The Value Checker
        // computes types differently for all other trees normally typed by the
        // PropagationTreeAnnotator.
        TreeAnnotator arrayCreation =
                new TreeAnnotator(this) {
                    PropagationTreeAnnotator propagationTreeAnnotator =
                            new PropagationTreeAnnotator(atypeFactory);

                    @Override
                    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror mirror) {
                        return propagationTreeAnnotator.visitNewArray(node, mirror);
                    }
                };
        return new ListTreeAnnotator(
                new ValueTreeAnnotator(this),
                new LiteralTreeAnnotator(this).addStandardLiteralQualifiers(),
                arrayCreation);
    }

    /**
     * @return the int range annotation is that equalient to {@code anm}, or {@code anm} if one
     *     doesn't exist
     */
    private AnnotationMirror convertSpecialIntRangeToStandardIntRange(AnnotationMirror anm) {
        if (AnnotationUtils.areSameByClass(anm, IntRangeFromPositive.class)) {
            return createIntRangeAnnotation(1, Integer.MAX_VALUE);
        }

        if (AnnotationUtils.areSameByClass(anm, IntRangeFromNonNegative.class)) {
            return createIntRangeAnnotation(0, Integer.MAX_VALUE);
        }

        if (AnnotationUtils.areSameByClass(anm, IntRangeFromGTENegativeOne.class)) {
            return createIntRangeAnnotation(-1, Integer.MAX_VALUE);
        }
        return anm;
    }

    /**
     * If {@code anno} is equalient to UnknownVal, return UnknownVal; otherwise, return {@code
     * anno}.
     */
    private AnnotationMirror convertToUnknown(AnnotationMirror anno) {
        if (AnnotationUtils.areSameByClass(anno, ArrayLenRange.class)) {
            Range range = getRange(anno);
            if (range.from == 0 && range.to >= Integer.MAX_VALUE) {
                return UNKNOWNVAL;
            }
            return anno;
        } else if (AnnotationUtils.areSameByClass(anno, IntRange.class)) {
            Range range = getRange(anno);
            if (range.isLongEverything()) {
                return UNKNOWNVAL;
            }
            return anno;
        } else {
            return anno;
        }
    }

    /** The TreeAnnotator for this AnnotatedTypeFactory. It adds/replaces annotations. */
    protected class ValueTreeAnnotator extends TreeAnnotator {

        public ValueTreeAnnotator(ValueAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {

            List<? extends ExpressionTree> dimensions = tree.getDimensions();
            List<? extends ExpressionTree> initializers = tree.getInitializers();

            // Array construction can provide dimensions or use an initializer.

            // Dimensions provided
            if (!dimensions.isEmpty()) {
                handleDimensions(dimensions, (AnnotatedArrayType) type);
            } else {
                // Initializer used
                handleInitializers(initializers, (AnnotatedArrayType) type);

                AnnotationMirror newQual;
                Class<?> clazz = ValueCheckerUtils.getClassFromType(type.getUnderlyingType());
                String stringVal = null;
                if (clazz.equals(char[].class)) {
                    stringVal = getCharArrayStringVal(initializers);
                }

                if (stringVal != null) {
                    newQual = createStringAnnotation(Collections.singletonList(stringVal));
                    type.replaceAnnotation(newQual);
                }
            }

            return null;
        }

        /**
         * Recursive method to handle array initializations. Recursively descends the initializer to
         * find each dimension's size and create the appropriate annotation for it.
         *
         * <p>If the annotation of the dimension is {@code @IntVal}, create an {@code @ArrayLen}
         * with the same set of possible values. If the annotation is {@code @IntRange}, create an
         * {@code @ArrayLenRange}. If the annotation is {@code @BottomVal}, create an
         * {@code @BottomVal} instead. In other cases, no annotations are created.
         *
         * @param dimensions a list of ExpressionTrees where each ExpressionTree is a specifier of
         *     the size of that dimension
         * @param type the AnnotatedTypeMirror of the array
         */
        private void handleDimensions(
                List<? extends ExpressionTree> dimensions, AnnotatedArrayType type) {
            if (dimensions.size() > 1) {
                handleDimensions(
                        dimensions.subList(1, dimensions.size()),
                        (AnnotatedArrayType) type.getComponentType());
            }
            AnnotationMirror dimType =
                    getAnnotatedType(dimensions.get(0)).getAnnotationInHierarchy(UNKNOWNVAL);

            if (AnnotationUtils.areSameByName(dimType, BOTTOMVAL)) {
                type.replaceAnnotation(BOTTOMVAL);
            } else {
                RangeOrListOfValues rolv = null;
                if (isIntRange(dimType)) {
                    rolv = new RangeOrListOfValues(getRange(dimType));
                } else if (AnnotationUtils.areSameByClass(dimType, IntVal.class)) {
                    rolv =
                            new RangeOrListOfValues(
                                    RangeOrListOfValues.convertLongsToInts(getIntValues(dimType)));
                }
                if (rolv != null) {
                    AnnotationMirror newQual =
                            rolv.createAnnotation((ValueAnnotatedTypeFactory) atypeFactory);
                    type.replaceAnnotation(newQual);
                }
            }
        }

        /**
         * Adds the ArrayLen/ArrayLenRange annotation from the array initializers to {@code type}.
         *
         * <p>If type is a multi-dimensional array, the initializers might also contain arrays, so
         * this method adds the annotations for those initializers, too.
         *
         * @param initializers initializer trees
         * @param type array type to which annotations are added
         */
        private void handleInitializers(
                List<? extends ExpressionTree> initializers, AnnotatedArrayType type) {

            List<Integer> array = new ArrayList<>();
            array.add(initializers.size());
            type.replaceAnnotation(createArrayLenAnnotation(array));

            if (type.getComponentType().getKind() != TypeKind.ARRAY) {
                return;
            }

            // A list of arrayLens.  arrayLenOfDimensions.get(i) is the array lengths for the ith
            // dimension.
            List<RangeOrListOfValues> arrayLenOfDimensions = new ArrayList<>();
            for (ExpressionTree init : initializers) {
                AnnotatedTypeMirror componentType = getAnnotatedType(init);
                int dimension = 0;
                while (componentType.getKind() == TypeKind.ARRAY) {
                    RangeOrListOfValues rolv = null;
                    if (dimension < arrayLenOfDimensions.size()) {
                        rolv = arrayLenOfDimensions.get(dimension);
                    }
                    AnnotationMirror arrayLen = componentType.getAnnotation(ArrayLen.class);
                    if (arrayLen != null) {
                        List<Integer> currentLengths = getArrayLength(arrayLen);
                        if (rolv != null) {
                            rolv.addAll(currentLengths);
                        } else {
                            arrayLenOfDimensions.add(new RangeOrListOfValues(currentLengths));
                        }
                    } else {
                        // Check for an arrayLenRange annotation
                        AnnotationMirror arrayLenRangeAnno =
                                componentType.getAnnotation(ArrayLenRange.class);
                        Range range;
                        if (arrayLenRangeAnno != null) {
                            range = getRange(arrayLenRangeAnno);
                        } else {
                            range = Range.EVERYTHING;
                        }
                        if (rolv != null) {
                            rolv.add(range);
                        } else {
                            arrayLenOfDimensions.add(new RangeOrListOfValues(range));
                        }
                    }

                    dimension++;
                    componentType = ((AnnotatedArrayType) componentType).getComponentType();
                }
            }

            AnnotatedTypeMirror componentType = type.getComponentType();
            int i = 0;
            while (componentType.getKind() == TypeKind.ARRAY && i < arrayLenOfDimensions.size()) {
                RangeOrListOfValues rolv = arrayLenOfDimensions.get(i);
                componentType.addAnnotation(
                        rolv.createAnnotation((ValueAnnotatedTypeFactory) atypeFactory));
                componentType = ((AnnotatedArrayType) componentType).getComponentType();
                i++;
            }
        }

        /** Convert a char array to a String. Return null if unable to convert. */
        private String getCharArrayStringVal(List<? extends ExpressionTree> initializers) {
            boolean allLiterals = true;
            StringBuilder stringVal = new StringBuilder();
            for (ExpressionTree e : initializers) {
                if (e.getKind() == Tree.Kind.INT_LITERAL) {
                    char charVal = (char) ((Integer) ((LiteralTree) e).getValue()).intValue();
                    stringVal.append(charVal);
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    char charVal = (((Character) ((LiteralTree) e).getValue()));
                    stringVal.append(charVal);
                } else {
                    allLiterals = false;
                }
            }
            if (allLiterals) {
                return stringVal.toString();
            }
            // If any part of the initializer isn't known,
            // the stringval isn't known.
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror atm) {
            if (handledByValueChecker(atm)) {
                AnnotationMirror oldAnno =
                        getAnnotatedType(tree.getExpression()).getAnnotationInHierarchy(UNKNOWNVAL);
                if (oldAnno == null) {
                    return null;
                }
                TypeMirror newType = atm.getUnderlyingType();
                AnnotationMirror newAnno;
                Range range;

                if (TypesUtils.isString(newType) || newType.getKind() == TypeKind.ARRAY) {
                    // Strings and arrays do not allow conversions
                    newAnno = oldAnno;
                } else if (isIntRange(oldAnno)
                        && (range = getRange(oldAnno)).isWiderThan(MAX_VALUES)) {
                    Class<?> newClass = ValueCheckerUtils.getClassFromType(newType);
                    if (newClass == String.class) {
                        newAnno = UNKNOWNVAL;
                    } else if (newClass == Boolean.class || newClass == boolean.class) {
                        throw new UnsupportedOperationException(
                                "ValueAnnotatedTypeFactory: can't convert int to boolean");
                    } else {
                        newAnno = createIntRangeAnnotation(NumberUtils.castRange(newType, range));
                    }
                } else {
                    List<?> values = ValueCheckerUtils.getValuesCastedToType(oldAnno, newType);
                    newAnno = createResultingAnnotation(atm.getUnderlyingType(), values);
                }
                atm.addMissingAnnotations(Collections.singleton(newAnno));
            } else if (atm.getKind() == TypeKind.ARRAY) {
                if (tree.getExpression().getKind() == Kind.NULL_LITERAL) {
                    atm.addMissingAnnotations(Collections.singleton(BOTTOMVAL));
                }
            }
            return null;
        }

        /**
         * Get the "value" field of the given annotation, casted to the given type. Empty list means
         * no value is possible (dead code). Null means no information is known -- any value is
         * possible.
         */
        private List<?> getValues(AnnotatedTypeMirror type, TypeMirror castTo) {
            AnnotationMirror anno = type.getAnnotationInHierarchy(UNKNOWNVAL);
            if (anno == null) {
                // If type is an AnnotatedTypeVariable (or other type without a primary annotation)
                // then anno will be null. It would be safe to use the annotation on the upper
                // bound; however, unless the upper bound was explicitly annotated, it will be
                // unknown.  AnnotatedTypes.findEffectiveAnnotationInHierarchy(, toSearch, top)
                return null;
            }
            return ValueCheckerUtils.getValuesCastedToType(anno, castTo);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!handledByValueChecker(type)) {
                return null;
            }
            Object value = tree.getValue();
            switch (tree.getKind()) {
                case BOOLEAN_LITERAL:
                    AnnotationMirror boolAnno =
                            createBooleanAnnotation(Collections.singletonList((Boolean) value));
                    type.replaceAnnotation(boolAnno);
                    return null;

                case CHAR_LITERAL:
                    AnnotationMirror charAnno =
                            createCharAnnotation(Collections.singletonList((Character) value));
                    type.replaceAnnotation(charAnno);
                    return null;

                case DOUBLE_LITERAL:
                case FLOAT_LITERAL:
                case INT_LITERAL:
                case LONG_LITERAL:
                    AnnotationMirror numberAnno =
                            createNumberAnnotationMirror(Collections.singletonList((Number) value));
                    type.replaceAnnotation(numberAnno);
                    return null;
                case STRING_LITERAL:
                    AnnotationMirror stringAnno =
                            createStringAnnotation(Collections.singletonList((String) value));
                    type.replaceAnnotation(stringAnno);
                    return null;
                default:
                    return null;
            }
        }

        /**
         * Given a MemberSelectTree representing a method call, return true if the method's
         * declaration is annotated with {@code @StaticallyExecutable}.
         */
        private boolean methodIsStaticallyExecutable(Element method) {
            return getDeclAnnotation(method, StaticallyExecutable.class) != null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (!methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree))
                    || !handledByValueChecker(type)) {
                return null;
            }

            if (getMethodIdentifier().isStringLengthInvocation(tree, processingEnv)) {
                AnnotatedTypeMirror receiverType = getReceiverType(tree);
                AnnotationMirror resultAnno = createArrayLengthResultAnnotation(receiverType);
                if (resultAnno != null) {
                    type.replaceAnnotation(resultAnno);
                }
                return null;
            }

            // Get argument values
            List<? extends ExpressionTree> arguments = tree.getArguments();
            ArrayList<List<?>> argValues;
            if (arguments.isEmpty()) {
                argValues = null;
            } else {
                argValues = new ArrayList<>();
                for (ExpressionTree argument : arguments) {
                    AnnotatedTypeMirror argType = getAnnotatedType(argument);
                    List<?> values = getValues(argType, argType.getUnderlyingType());
                    if (values == null || values.isEmpty()) {
                        // Values aren't known, so don't try to evaluate the method.
                        return null;
                    }
                    argValues.add(values);
                }
            }

            // Get receiver values
            AnnotatedTypeMirror receiver = getReceiverType(tree);
            List<?> receiverValues;

            if (receiver != null && !ElementUtils.isStatic(TreeUtils.elementFromUse(tree))) {
                receiverValues = getValues(receiver, receiver.getUnderlyingType());
                if (receiverValues == null || receiverValues.isEmpty()) {
                    // Values aren't known, so don't try to evaluate the method.
                    return null;
                }
            } else {
                receiverValues = null;
            }

            // Evaluate method
            List<?> returnValues = evaluator.evaluateMethodCall(argValues, receiverValues, tree);
            if (returnValues == null) {
                return null;
            }
            AnnotationMirror returnType =
                    createResultingAnnotation(type.getUnderlyingType(), returnValues);
            type.replaceAnnotation(returnType);

            return null;
        }

        @Override
        public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
            if (!methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree))
                    || !handledByValueChecker(type)) {
                return null;
            }

            // get argument values
            List<? extends ExpressionTree> arguments = tree.getArguments();
            ArrayList<List<?>> argValues;
            if (arguments.isEmpty()) {
                argValues = null;
            } else {
                argValues = new ArrayList<>();
                for (ExpressionTree argument : arguments) {
                    AnnotatedTypeMirror argType = getAnnotatedType(argument);
                    List<?> values = getValues(argType, argType.getUnderlyingType());
                    if (values == null || values.isEmpty()) {
                        // Values aren't known, so don't try to evaluate the method.
                        return null;
                    }
                    argValues.add(values);
                }
            }

            // Evaluate method
            List<?> returnValues =
                    evaluator.evaluteConstructorCall(argValues, tree, type.getUnderlyingType());
            if (returnValues == null) {
                return null;
            }
            AnnotationMirror returnType =
                    createResultingAnnotation(type.getUnderlyingType(), returnValues);
            type.replaceAnnotation(returnType);

            return null;
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            if (!TreeUtils.isFieldAccess(tree) || !handledByValueChecker(type)) {
                return null;
            }

            VariableElement elem = (VariableElement) TreeUtils.elementFromTree(tree);
            Object value = elem.getConstantValue();
            if (value != null) {
                // The field is a compile time constant.
                type.replaceAnnotation(createResultingAnnotation(type.getUnderlyingType(), value));
                return null;
            }
            if (ElementUtils.isStatic(elem) && ElementUtils.isFinal(elem)) {
                // The field is static and final.
                Element e = TreeUtils.elementFromTree(tree.getExpression());
                if (e != null) {
                    String classname = ElementUtils.getQualifiedClassName(e).toString();
                    String fieldName = tree.getIdentifier().toString();
                    value = evaluator.evaluateStaticFieldAccess(classname, fieldName, tree);
                    if (value != null) {
                        type.replaceAnnotation(
                                createResultingAnnotation(type.getUnderlyingType(), value));
                    }
                    return null;
                }
            }

            if (TreeUtils.isArrayLengthAccess(tree)) {
                // The field access is to the length field, as in "someArrayExpression.length"
                AnnotatedTypeMirror receiverType = getAnnotatedType(tree.getExpression());
                if (receiverType.getKind() == TypeKind.ARRAY) {
                    AnnotationMirror resultAnno = createArrayLengthResultAnnotation(receiverType);
                    if (resultAnno != null) {
                        type.replaceAnnotation(resultAnno);
                    }
                }
            }
            return null;
        }

        /** Returns true iff the given type is in the domain of the Constant Value Checker. */
        private boolean handledByValueChecker(AnnotatedTypeMirror type) {
            TypeMirror tm = type.getUnderlyingType();
            /* TODO: compare performance to the more readable.
            return TypesUtils.isPrimitive(tm)
                    || TypesUtils.isBoxedPrimitive(tm)
                    || TypesUtils.isString(tm)
                    || tm.toString().equals("char[]"); // Why?
            */
            return COVERED_CLASS_STRINGS.contains(tm.toString());
        }

        @Override
        public Void visitConditionalExpression(
                ConditionalExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            // Work around for https://github.com/typetools/checker-framework/issues/602.
            annotatedTypeMirror.replaceAnnotation(UNKNOWNVAL);
            return null;
        }
    }

    /**
     * Returns a constant value annotation for a length of an array or string type with a constant
     * value annotation.
     */
    AnnotationMirror createArrayLengthResultAnnotation(AnnotatedTypeMirror receiverType) {
        AnnotationMirror arrayAnno = receiverType.getAnnotationInHierarchy(UNKNOWNVAL);
        if (AnnotationUtils.areSameByClass(arrayAnno, ArrayLen.class)) {
            // array.length, where array : @ArrayLen(x)
            List<Integer> lengths = ValueAnnotatedTypeFactory.getArrayLength(arrayAnno);
            return createNumberAnnotationMirror(new ArrayList<>(lengths));
        }
        // Check for an ArrayLenRange annotation.
        if (AnnotationUtils.areSameByClass(arrayAnno, ArrayLenRange.class)) {
            // array.length, where array : @ArrayLenRange(x)
            Range range = getRange(arrayAnno);
            return createIntRangeAnnotation(range);
        }

        if (AnnotationUtils.areSameByClass(arrayAnno, StringVal.class)) {
            List<String> strings = ValueAnnotatedTypeFactory.getStringValues(arrayAnno);
            List<Integer> lengths = ValueCheckerUtils.getLengthsForStringValues(strings);
            return createNumberAnnotationMirror(new ArrayList<>(lengths));
        }

        return createIntRangeAnnotation(0, Integer.MAX_VALUE);
    }

    /**
     * Returns a constant value annotation with the {@code value}. The class of the annotation
     * reflects the {@code resultType} given.
     *
     * @param resultType used to selecte which kind of value annotation is returned
     * @param value value to use
     * @return a constant value annotation with the {@code value}
     */
    AnnotationMirror createResultingAnnotation(TypeMirror resultType, Object value) {
        return createResultingAnnotation(resultType, Collections.singletonList(value));
    }

    /**
     * Returns a constant value annotation with the {@code values}. The class of the annotation
     * reflects the {@code resultType} given.
     *
     * @param resultType used to selected which kind of value annotation is returned
     * @param values must be a homogeneous list: every element of it has the same class
     * @return a constant value annotation with the {@code values}
     */
    AnnotationMirror createResultingAnnotation(TypeMirror resultType, List<?> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        // For some reason null is included in the list of values,
        // so remove it so that it does not cause a NPE elsewhere.
        values.remove(null);
        if (values.isEmpty()) {
            return BOTTOMVAL;
        }

        if (TypesUtils.isString(resultType)) {
            List<String> stringVals = new ArrayList<>(values.size());
            for (Object o : values) {
                stringVals.add((String) o);
            }
            return createStringAnnotation(stringVals);
        } else if (ValueCheckerUtils.getClassFromType(resultType) == char[].class) {
            List<String> stringVals = new ArrayList<>(values.size());
            for (Object o : values) {
                if (o instanceof char[]) {
                    stringVals.add(new String((char[]) o));
                } else {
                    stringVals.add(o.toString());
                }
            }
            return createStringAnnotation(stringVals);
        }

        TypeKind primitiveKind;
        if (TypesUtils.isPrimitive(resultType)) {
            primitiveKind = resultType.getKind();
        } else if (TypesUtils.isBoxedPrimitive(resultType)) {
            primitiveKind = types.unboxedType(resultType).getKind();
        } else {
            return UNKNOWNVAL;
        }

        switch (primitiveKind) {
            case BOOLEAN:
                List<Boolean> boolVals = new ArrayList<>(values.size());
                for (Object o : values) {
                    boolVals.add((Boolean) o);
                }
                return createBooleanAnnotation(boolVals);
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
            case BYTE:
                List<Number> numberVals = new ArrayList<>(values.size());
                List<Character> characterVals = new ArrayList<>(values.size());
                for (Object o : values) {
                    if (o instanceof Character) {
                        characterVals.add((Character) o);
                    } else {
                        numberVals.add((Number) o);
                    }
                }
                if (numberVals.isEmpty()) {
                    return createCharAnnotation(characterVals);
                }
                return createNumberAnnotationMirror(new ArrayList<>(numberVals));
            case CHAR:
                List<Character> charVals = new ArrayList<>(values.size());
                for (Object o : values) {
                    if (o instanceof Number) {
                        charVals.add((char) ((Number) o).intValue());
                    } else {
                        charVals.add((char) o);
                    }
                }
                return createCharAnnotation(charVals);
            default:
                throw new UnsupportedOperationException("Unexpected kind:" + resultType);
        }
    }

    /**
     * Returns a {@link IntVal} or {@link IntRange} annotation using the values. If {@code values}
     * is null, then UnknownVal is returned; if {@code values} is empty, then bottom is returned. If
     * the number of {@code values} is greater than MAX_VALUES, return an {@link IntRange}. In other
     * cases, the values are sorted and duplicates are removed before an {@link IntVal} is created.
     *
     * @param values list of longs; duplicates are allowed and the values may be in any order
     * @return an annotation depends on the values
     */
    public AnnotationMirror createIntValAnnotation(List<Long> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        if (values.isEmpty()) {
            return BOTTOMVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            long valMin = Collections.min(values);
            long valMax = Collections.max(values);
            return createIntRangeAnnotation(valMin, valMax);
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, IntVal.class);
            builder.setValue("value", values);
            return builder.build();
        }
    }

    /**
     * Convert an {@code @IntRange} annotation to an {@code @IntVal} annotation, or to UNKNOWNVAL if
     * the input is too wide to be represented as an {@code @IntVal}.
     */
    public AnnotationMirror convertIntRangeToIntVal(AnnotationMirror intRangeAnno) {
        Range range = getRange(intRangeAnno);
        List<Long> values = ValueCheckerUtils.getValuesFromRange(range, Long.class);
        return createIntValAnnotation(values);
    }

    /**
     * Returns a {@link DoubleVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of doubles; duplicates are allowed and the values may be in any order
     * @return a {@link DoubleVal} annotation using the values
     */
    public AnnotationMirror createDoubleValAnnotation(List<Double> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        if (values.isEmpty()) {
            return BOTTOMVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, DoubleVal.class);
            builder.setValue("value", values);
            return builder.build();
        }
    }

    /** Convert an {@code @IntVal} annotation to a {@code @DoubleVal} annotation. */
    private AnnotationMirror convertIntValToDoubleVal(AnnotationMirror intValAnno) {
        List<Long> intValues = getIntValues(intValAnno);
        return createDoubleValAnnotation(convertLongListToDoubleList(intValues));
    }

    /** Convert a {@code List<Long>} to a {@code List<Double>}. */
    private List<Double> convertLongListToDoubleList(List<Long> intValues) {
        List<Double> doubleValues = new ArrayList<>(intValues.size());
        for (Long intValue : intValues) {
            doubleValues.add(intValue.doubleValue());
        }
        return doubleValues;
    }

    /**
     * Returns a {@link StringVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created. If values is larger than
     * the max number of values allowed (10 by default), then an {@link ArrayLen} or an {@link
     * ArrayLenRange} annotation is returned.
     *
     * @param values list of strings; duplicates are allowed and the values may be in any order
     * @return a {@link StringVal} annotation using the values
     */
    public AnnotationMirror createStringAnnotation(List<String> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        if (values.isEmpty()) {
            return BOTTOMVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            // Too many strings are replaced by their lengths
            List<Integer> lengths = ValueCheckerUtils.getLengthsForStringValues(values);
            return createArrayLenAnnotation(lengths);
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, StringVal.class);
            builder.setValue("value", values);
            return builder.build();
        }
    }

    /**
     * Returns a {@link ArrayLen} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created. If values is larger than
     * the max number of values allowed (10 by default), then an {@link ArrayLenRange} annotation is
     * returned.
     *
     * @param values list of integers; duplicates are allowed and the values may be in any order
     * @return a {@link ArrayLen} annotation using the values
     */
    public AnnotationMirror createArrayLenAnnotation(List<Integer> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        if (values.isEmpty()) {
            return BOTTOMVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.isEmpty() || Collections.min(values) < 0) {
            return BOTTOMVAL;
        } else if (values.size() > MAX_VALUES) {
            return createArrayLenRangeAnnotation(Collections.min(values), Collections.max(values));
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, ArrayLen.class);
            builder.setValue("value", values);
            return builder.build();
        }
    }

    /**
     * Returns a {@link BoolVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of booleans; duplicates are allowed and the values may be in any order
     * @return a {@link BoolVal} annotation using the values
     */
    public AnnotationMirror createBooleanAnnotation(List<Boolean> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        if (values.isEmpty()) {
            return BOTTOMVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, BoolVal.class);
            builder.setValue("value", values);
            return builder.build();
        }
    }

    /**
     * Returns a {@link IntVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of characters; duplicates are allowed and the values may be in any order
     * @return a {@link IntVal} annotation using the values
     */
    public AnnotationMirror createCharAnnotation(List<Character> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        if (values.isEmpty()) {
            return BOTTOMVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        } else {
            List<Long> longValues = new ArrayList<>();
            for (char value : values) {
                longValues.add((long) value);
            }
            return createIntValAnnotation(longValues);
        }
    }

    /** @param values must be a homogeneous list: every element of it has the same class. */
    public AnnotationMirror createNumberAnnotationMirror(List<Number> values) {
        if (values == null) {
            return UNKNOWNVAL;
        } else if (values.isEmpty()) {
            return BOTTOMVAL;
        }
        Number first = values.get(0);
        if (first instanceof Integer
                || first instanceof Short
                || first instanceof Long
                || first instanceof Byte) {
            List<Long> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.longValue());
            }
            return createIntValAnnotation(intValues);
        } else if (first instanceof Double || first instanceof Float) {
            List<Double> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.doubleValue());
            }
            return createDoubleValAnnotation(intValues);
        }
        throw new UnsupportedOperationException(
                "ValueAnnotatedTypeFactory: unexpected class: " + first.getClass());
    }

    /**
     * Create an {@code @IntRange} annotation from the two (inclusive) bounds. Does not return
     * BOTTOMVAL or UNKNOWNVAL.
     */
    private AnnotationMirror createIntRangeAnnotation(long from, long to) {
        assert from <= to;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, IntRange.class);
        builder.setValue("from", from);
        builder.setValue("to", to);
        return builder.build();
    }

    /**
     * Create an {@code @IntRange} or {@code @IntVal} annotation from the range. May return
     * BOTTOMVAL or UNKNOWNVAL.
     */
    public AnnotationMirror createIntRangeAnnotation(Range range) {
        if (range.isNothing()) {
            return BOTTOMVAL;
        } else if (range.isLongEverything()) {
            return UNKNOWNVAL;
        } else if (range.isWiderThan(MAX_VALUES)) {
            return createIntRangeAnnotation(range.from, range.to);
        } else {
            List<Long> newValues = ValueCheckerUtils.getValuesFromRange(range, Long.class);
            return createIntValAnnotation(newValues);
        }
    }

    /**
     * Creates the special {@link IntRangeFromPositive} annotation, which is only used as an alias
     * for the Index Checker's {@link org.checkerframework.checker.index.qual.Positive} annotation.
     * It is treated everywhere as an IntRange annotation, but is not checked when it appears as the
     * left hand side of an assignment (because the Lower Bound Checker will check it).
     */
    private AnnotationMirror createIntRangeFromPositive() {
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, IntRangeFromPositive.class);
        return builder.build();
    }

    /**
     * Creates the special {@link IntRangeFromNonNegative} annotation, which is only used as an
     * alias for the Index Checker's {@link org.checkerframework.checker.index.qual.NonNegative}
     * annotation. It is treated everywhere as an IntRange annotation, but is not checked when it
     * appears as the left hand side of an assignment (because the Lower Bound Checker will check
     * it).
     */
    private AnnotationMirror createIntRangeFromNonNegative() {
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, IntRangeFromNonNegative.class);
        return builder.build();
    }

    /**
     * Creates the special {@link IntRangeFromGTENegativeOne} annotation, which is only used as an
     * alias for the Index Checker's {@link org.checkerframework.checker.index.qual.GTENegativeOne}
     * annotation. It is treated everywhere as an IntRange annotation, but is not checked when it
     * appears as the left hand side of an assignment (because the Lower Bound Checker will check
     * it).
     */
    private AnnotationMirror createIntRangeFromGTENegativeOne() {
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, IntRangeFromGTENegativeOne.class);
        return builder.build();
    }

    /**
     * Create an {@code @ArrayLenRange} annotation from the two (inclusive) bounds. Does not return
     * BOTTOMVAL or UNKNOWNVAL.
     */
    public AnnotationMirror createArrayLenRangeAnnotation(int from, int to) {
        assert from <= to;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, ArrayLenRange.class);
        builder.setValue("from", from);
        builder.setValue("to", to);
        return builder.build();
    }

    /**
     * Create an {@code @ArrayLenRange} annotation from the range. May return BOTTOMVAL or
     * UNKNOWNVAL.
     */
    public AnnotationMirror createArrayLenRangeAnnotation(Range range) {
        if (range.isNothing()) {
            return BOTTOMVAL;
        } else if (range.isLongEverything()
                || !range.isWithin(Integer.MIN_VALUE, Integer.MAX_VALUE)) {
            return UNKNOWNVAL;
        } else {
            return createArrayLenRangeAnnotation(
                    Long.valueOf(range.from).intValue(), Long.valueOf(range.to).intValue());
        }
    }

    /** Converts an {@code @StringVal} annotation to an {@code @ArrayLenRange} annotation. */
    private AnnotationMirror convertStringValToArrayLenRange(AnnotationMirror stringValAnno) {
        List<String> values = getStringValues(stringValAnno);
        List<Integer> lengths = ValueCheckerUtils.getLengthsForStringValues(values);
        return createArrayLenRangeAnnotation(Collections.min(lengths), Collections.max(lengths));
    }

    /**
     * Converts an {@code @StringVal} annotation to an {@code @ArrayLen} annotation. If the
     * {@code @StringVal} annotation contains string values of more than MAX_VALUES distinct
     * lengths, {@code @ArrayLenRange} annotation is returned instead.
     */
    private AnnotationMirror convertStringValToArrayLen(AnnotationMirror stringValAnno) {
        List<String> values = getStringValues(stringValAnno);
        return createArrayLenAnnotation(ValueCheckerUtils.getLengthsForStringValues(values));
    }

    /** Converts an {@code @ArrayLen} annotation to an {@code @ArrayLenRange} annotation. */
    public AnnotationMirror convertArrayLenToArrayLenRange(AnnotationMirror arrayLenAnno) {
        List<Integer> values = getArrayLength(arrayLenAnno);
        return createArrayLenRangeAnnotation(Collections.min(values), Collections.max(values));
    }

    /** Converts an {@code @IntVal} annotation to an {@code @IntRange} annotation. */
    public AnnotationMirror convertIntValToIntRange(AnnotationMirror intValAnno) {
        List<Long> intValues = getIntValues(intValAnno);
        return createIntRangeAnnotation(Collections.min(intValues), Collections.max(intValues));
    }

    /**
     * Returns a {@code Range} bounded by the values specified in the given {@code @Range}
     * annotation. Also returns an appropriate range if an {@code @IntVal} annotation is passed.
     * Returns {@code null} if the annotation is null or if the annotation is not an {@code
     * IntRange}, {@code IntRangeFromPositive}, {@code IntVal}, or {@code ArrayLenRange}.
     */
    public static Range getRange(AnnotationMirror rangeAnno) {
        if (rangeAnno == null) {
            return null;
        }

        if (AnnotationUtils.areSameByClass(rangeAnno, IntRangeFromPositive.class)) {
            return new Range(1, Integer.MAX_VALUE);
        }
        if (AnnotationUtils.areSameByClass(rangeAnno, IntRangeFromNonNegative.class)) {
            return new Range(0, Integer.MAX_VALUE);
        }
        if (AnnotationUtils.areSameByClass(rangeAnno, IntRangeFromGTENegativeOne.class)) {
            return new Range(-1, Integer.MAX_VALUE);
        }

        if (AnnotationUtils.areSameByClass(rangeAnno, IntVal.class)) {
            return ValueCheckerUtils.getRangeFromValues(getIntValues(rangeAnno));
        }

        // Assume rangeAnno is well-formed, i.e., 'from' is less than or equal to 'to'.
        if (AnnotationUtils.areSameByClass(rangeAnno, IntRange.class)) {
            return new Range(
                    AnnotationUtils.getElementValue(rangeAnno, "from", Long.class, true),
                    AnnotationUtils.getElementValue(rangeAnno, "to", Long.class, true));
        }

        if (AnnotationUtils.areSameByClass(rangeAnno, ArrayLenRange.class)) {
            return new Range(
                    AnnotationUtils.getElementValue(rangeAnno, "from", Integer.class, true),
                    AnnotationUtils.getElementValue(rangeAnno, "to", Integer.class, true));
        }

        return null;
    }

    /**
     * Returns the set of possible values as a sorted list with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * <p>The method returns a list of {@code Long} but is named {@code getIntValues} because it
     * supports the {@code @IntVal} annotation.
     *
     * @param intAnno an {@code @IntVal} annotation, or null
     */
    public static List<Long> getIntValues(AnnotationMirror intAnno) {
        if (intAnno == null) {
            return null;
        }
        List<Long> list = AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
        list = ValueCheckerUtils.removeDuplicates(list);
        return list;
    }

    /**
     * Returns the set of possible values as a sorted list with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * @param doubleAnno a {@code @DoubleVal} annotation, or null
     */
    public static List<Double> getDoubleValues(AnnotationMirror doubleAnno) {
        if (doubleAnno == null) {
            return null;
        }
        List<Double> list =
                AnnotationUtils.getElementValueArray(doubleAnno, "value", Double.class, true);
        list = ValueCheckerUtils.removeDuplicates(list);
        return list;
    }

    /**
     * Returns the set of possible array lengths as a sorted list with no duplicate values. Returns
     * the empty list if no values are possible (for dead code). Returns null if any value is
     * possible -- that is, if no estimate can be made -- and this includes when there is no
     * constant-value annotation so the argument is null.
     *
     * @param arrayAnno an {@code @ArrayLen} annotation, or null
     */
    public static List<Integer> getArrayLength(AnnotationMirror arrayAnno) {
        if (arrayAnno == null) {
            return null;
        }
        List<Integer> list =
                AnnotationUtils.getElementValueArray(arrayAnno, "value", Integer.class, true);
        list = ValueCheckerUtils.removeDuplicates(list);
        return list;
    }

    /**
     * Returns the set of possible values as a sorted list with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * @param intAnno an {@code @IntVal} annotation, or null
     */
    public static List<Character> getCharValues(AnnotationMirror intAnno) {
        if (intAnno == null) {
            return new ArrayList<>();
        }
        List<Long> intValues =
                AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
        TreeSet<Character> charValues = new TreeSet<>();
        for (Long i : intValues) {
            charValues.add((char) i.intValue());
        }
        return new ArrayList<>(charValues);
    }

    /**
     * Returns the set of possible values as a sorted list with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * @param boolAnno a {@code @BoolVal} annotation, or null
     */
    public static List<Boolean> getBooleanValues(AnnotationMirror boolAnno) {
        if (boolAnno == null) {
            return new ArrayList<>();
        }
        List<Boolean> boolValues =
                AnnotationUtils.getElementValueArray(boolAnno, "value", Boolean.class, true);
        Set<Boolean> boolSet = new TreeSet<>(boolValues);
        if (boolSet.size() > 1) {
            // boolSet={true,false};
            return null;
        }
        return new ArrayList<>(boolSet);
    }

    /**
     * Returns the set of possible values as a sorted list with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * @param stringAnno a {@code @StringVal} annotation, or null
     */
    public static List<String> getStringValues(AnnotationMirror stringAnno) {
        if (stringAnno == null) {
            return null;
        }
        List<String> list =
                AnnotationUtils.getElementValueArray(stringAnno, "value", String.class, true);
        list = ValueCheckerUtils.removeDuplicates(list);
        return list;
    }

    public boolean isIntRange(Set<AnnotationMirror> anmSet) {
        for (AnnotationMirror anm : anmSet) {
            if (isIntRange(anm)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIntRange(AnnotationMirror anm) {
        return AnnotationUtils.areSameByClass(anm, IntRange.class)
                || AnnotationUtils.areSameByClass(anm, IntRangeFromPositive.class)
                || AnnotationUtils.areSameByClass(anm, IntRangeFromNonNegative.class)
                || AnnotationUtils.areSameByClass(anm, IntRangeFromGTENegativeOne.class);
    }

    public int getMinLenValue(AnnotatedTypeMirror atm) {
        return getMinLenValue(atm.getAnnotationInHierarchy(UNKNOWNVAL));
    }

    /**
     * Used to find the maximum length of an array. Returns null if there is no minimum length
     * known, or if the passed annotation is null.
     */
    public Integer getMaxLenValue(AnnotationMirror annotation) {
        if (annotation == null) {
            return null;
        }
        if (AnnotationUtils.areSameByClass(annotation, ArrayLenRange.class)) {
            return Long.valueOf(getRange(annotation).to).intValue();
        } else if (AnnotationUtils.areSameByClass(annotation, ArrayLen.class)) {
            return Collections.max(getArrayLength(annotation));
        } else if (AnnotationUtils.areSameByClass(annotation, StringVal.class)) {
            return Collections.max(
                    ValueCheckerUtils.getLengthsForStringValues(getStringValues(annotation)));
        } else {
            return null;
        }
    }

    /**
     * Finds a minimum length of an array specified by the provided annotation. Returns null if
     * there is no minimum length known, or if the passed annotation is null.
     *
     * <p>Note that this routine handles actual {@link MinLen} annotations, because it is called by
     * {@link ValueAnnotatedTypeFactory#canonicalAnnotation(AnnotationMirror)}, which transforms
     * {@link MinLen} annotations into {@link ArrayLenRange} annotations.
     */
    private Integer getSpecifiedMinLenValue(AnnotationMirror annotation) {
        if (annotation == null) {
            return null;
        }
        if (AnnotationUtils.areSameByClass(annotation, MinLen.class)) {
            return AnnotationUtils.getElementValue(annotation, "value", Integer.class, true);
        } else if (AnnotationUtils.areSameByClass(annotation, ArrayLenRange.class)) {
            return Long.valueOf(getRange(annotation).from).intValue();
        } else if (AnnotationUtils.areSameByClass(annotation, ArrayLen.class)) {
            return Collections.min(getArrayLength(annotation));
        } else if (AnnotationUtils.areSameByClass(annotation, StringVal.class)) {
            return Collections.min(
                    ValueCheckerUtils.getLengthsForStringValues(getStringValues(annotation)));
        } else {
            return null;
        }
    }

    /**
     * Used to find the minimum length of an array, which is useful for array bounds checking.
     * Returns 0 if there is no minimum length known, or if the passed annotation is null.
     *
     * <p>Note that this routine handles actual {@link MinLen} annotations, because it is called by
     * {@link ValueAnnotatedTypeFactory#canonicalAnnotation(AnnotationMirror)}, which transforms
     * {@link MinLen} annotations into {@link ArrayLenRange} annotations.
     */
    public int getMinLenValue(AnnotationMirror annotation) {
        Integer minLen = getSpecifiedMinLenValue(annotation);
        if (minLen == null || minLen < 0) {
            return 0;
        } else {
            return minLen;
        }
    }

    /**
     * Returns the smallest possible value that an integral annotation might take on. The passed
     * {@code AnnotatedTypeMirror} should contain either an {@code @IntRange} annotation or an
     * {@code @IntVal} annotation. Returns null if it does not.
     */
    public Long getMinimumIntegralValue(AnnotatedTypeMirror atm) {
        AnnotationMirror anm = atm.getAnnotationInHierarchy(UNKNOWNVAL);
        if (AnnotationUtils.areSameByClass(anm, IntVal.class)) {
            List<Long> possibleValues = getIntValues(anm);
            return Collections.min(possibleValues);
        } else if (isIntRange(anm)) {
            Range range = getRange(anm);
            return range.from;
        }
        return null;
    }

    /**
     * Returns the minimum length of an array expression or 0 if the min length is unknown.
     *
     * @param sequenceExpression flow expression
     * @param tree expression tree or variable declaration
     * @param currentPath path to local scope
     * @return min length of sequenceExpression or 0
     */
    public int getMinLenFromString(String sequenceExpression, Tree tree, TreePath currentPath) {
        AnnotationMirror lengthAnno;
        FlowExpressions.Receiver expressionObj;
        try {
            expressionObj = getReceiverFromJavaExpressionString(sequenceExpression, currentPath);
        } catch (FlowExpressionParseException e) {
            // ignore parse errors and return 0.
            return 0;
        }

        if (expressionObj instanceof FlowExpressions.ValueLiteral) {
            FlowExpressions.ValueLiteral sequenceLiteral =
                    (FlowExpressions.ValueLiteral) expressionObj;
            Object sequenceLiteralValue = sequenceLiteral.getValue();
            if (sequenceLiteralValue instanceof String) {
                return ((String) sequenceLiteralValue).length();
            }
        }

        lengthAnno = getAnnotationFromReceiver(expressionObj, tree, ArrayLenRange.class);
        if (lengthAnno == null) {
            lengthAnno = getAnnotationFromReceiver(expressionObj, tree, ArrayLen.class);
        }
        if (lengthAnno == null) {
            lengthAnno = getAnnotationFromReceiver(expressionObj, tree, StringVal.class);
        }

        if (lengthAnno == null) {
            // Could not find a more precise type, so return 0;
            return 0;
        }

        return getMinLenValue(lengthAnno);
    }
}
