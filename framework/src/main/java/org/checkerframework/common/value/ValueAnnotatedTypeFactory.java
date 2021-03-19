package org.checkerframework.common.value;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
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
import java.util.regex.Pattern;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.EnumVal;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntRangeFromGTENegativeOne;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;
import org.checkerframework.common.value.qual.IntRangeFromPositive;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.MatchesRegex;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.MinLenFieldInvariant;
import org.checkerframework.common.value.qual.PolyValue;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ArrayCreation;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
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
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.checkerframework.javacutil.TypesUtils;

/** AnnotatedTypeFactory for the Value type system. */
public class ValueAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    /** Fully-qualified class name of {@link UnknownVal}. */
    public static final String UNKNOWN_NAME = "org.checkerframework.common.value.qual.UnknownVal";
    /** Fully-qualified class name of {@link BottomVal}. */
    public static final String BOTTOMVAL_NAME = "org.checkerframework.common.value.qual.BottomVal";
    /** Fully-qualified class name of {@link PolyValue}. */
    public static final String POLY_NAME = "org.checkerframework.common.value.qual.PolyValue";
    /** Fully-qualified class name of {@link ArrayLen}. */
    public static final String ARRAYLEN_NAME = "org.checkerframework.common.value.qual.ArrayLen";
    /** Fully-qualified class name of {@link BoolVal}. */
    public static final String BOOLVAL_NAME = "org.checkerframework.common.value.qual.BoolVal";
    /** Fully-qualified class name of {@link DoubleVal}. */
    public static final String DOUBLEVAL_NAME = "org.checkerframework.common.value.qual.DoubleVal";
    /** Fully-qualified class name of {@link IntVal}. */
    public static final String INTVAL_NAME = "org.checkerframework.common.value.qual.IntVal";
    /** Fully-qualified class name of {@link StringVal}. */
    public static final String STRINGVAL_NAME = "org.checkerframework.common.value.qual.StringVal";
    /** Fully-qualified class name of {@link ArrayLenRange}. */
    public static final String ARRAYLENRANGE_NAME =
            "org.checkerframework.common.value.qual.ArrayLenRange";
    /** Fully-qualified class name of {@link IntRange}. */
    public static final String INTRANGE_NAME = "org.checkerframework.common.value.qual.IntRange";

    /** Fully-qualified class name of {@link IntRangeFromGTENegativeOne}. */
    public static final String INTRANGE_FROMGTENEGONE_NAME =
            "org.checkerframework.common.value.qual.IntRangeFromGTENegativeOne";
    /** Fully-qualified class name of {@link IntRangeFromNonNegative}. */
    public static final String INTRANGE_FROMNONNEG_NAME =
            "org.checkerframework.common.value.qual.IntRangeFromNonNegative";
    /** Fully-qualified class name of {@link IntRangeFromPositive}. */
    public static final String INTRANGE_FROMPOS_NAME =
            "org.checkerframework.common.value.qual.IntRangeFromPositive";
    /** Fully-qualified class name of {@link MinLen}. */
    public static final String MINLEN_NAME = "org.checkerframework.common.value.qual.MinLen";
    /** Fully-qualified class name of {@link MatchesRegex}. */
    public static final String MATCHES_REGEX_NAME =
            "org.checkerframework.common.value.qual.MatchesRegex";

    /** The maximum number of values allowed in an annotation's array. */
    protected static final int MAX_VALUES = 10;

    /** The top type for this hierarchy. */
    protected final AnnotationMirror UNKNOWNVAL =
            AnnotationBuilder.fromClass(elements, UnknownVal.class);

    /** The bottom type for this hierarchy. */
    protected final AnnotationMirror BOTTOMVAL =
            AnnotationBuilder.fromClass(elements, BottomVal.class);

    /** The canonical @{@link PolyValue} annotation. */
    public final AnnotationMirror POLY = AnnotationBuilder.fromClass(elements, PolyValue.class);

    /** The canonical @{@link BoolVal}(true) annotation. */
    public final AnnotationMirror BOOLEAN_TRUE =
            createBooleanAnnotation(Collections.singletonList(true));

    /** The canonical @{@link BoolVal}(false) annotation. */
    public final AnnotationMirror BOOLEAN_FALSE =
            createBooleanAnnotation(Collections.singletonList(false));

    /** The from() element/field of an @IntRange annotation. */
    protected final ExecutableElement intRangeFromElement =
            TreeUtils.getMethod(IntRange.class, "from", 0, processingEnv);

    /** The to() element/field of an @IntRange annotation. */
    protected final ExecutableElement intRangeToElement =
            TreeUtils.getMethod(IntRange.class, "to", 0, processingEnv);

    /** The from() element/field of an @ArrayLenRange annotation. */
    protected final ExecutableElement arrayLenRangeFromElement =
            TreeUtils.getMethod(ArrayLenRange.class, "from", 0, processingEnv);

    /** The to() element/field of an @ArrayLenRange annotation. */
    protected final ExecutableElement arrayLenRangeToElement =
            TreeUtils.getMethod(ArrayLenRange.class, "to", 0, processingEnv);

    /** The value() element/field of a @MinLen annotation. */
    protected final ExecutableElement minLenValueElement =
            TreeUtils.getMethod(MinLen.class, "value", 0, processingEnv);

    /** Should this type factory report warnings? */
    private final boolean reportEvalWarnings;

    /** Helper class that evaluates statically executable methods, constructors, and fields. */
    // TODO: only used in ValueTreeAnnotator. Should it move there?
    protected final ReflectiveEvaluator evaluator;

    /** Helper class that holds references to special methods. */
    private final ValueMethodIdentifier methods;

    @SuppressWarnings("StaticAssignmentInConstructor") // static Range.ignoreOverflow is gross
    public ValueAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        reportEvalWarnings = checker.hasOption(ValueChecker.REPORT_EVAL_WARNS);
        Range.ignoreOverflow = checker.hasOption(ValueChecker.IGNORE_RANGE_OVERFLOW);
        evaluator = new ReflectiveEvaluator(checker, this, reportEvalWarnings);

        addAliasedTypeAnnotation("android.support.annotation.IntRange", IntRange.class, true);

        // The actual ArrayLenRange is created by
        // {@link ValueAnnotatedTypeFactory#canonicalAnnotation(AnnotationMirror)};
        // this line just registers the alias. The BottomVal is never used.
        addAliasedTypeAnnotation(MinLen.class, BOTTOMVAL);

        // @Positive is aliased here because @Positive provides useful
        // information about @MinLen annotations.
        // @NonNegative and @GTENegativeOne are aliased similarly so
        // that it's possible to overwrite a function annotated to return
        // @NonNegative with, for instance, a function that returns an @IntVal(0).
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.Positive", createIntRangeFromPositive());
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.NonNegative",
                createIntRangeFromNonNegative());
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.GTENegativeOne",
                createIntRangeFromGTENegativeOne());
        // Must also alias any alias of three annotations above:
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.LengthOf",
                createIntRangeFromNonNegative());
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.IndexFor",
                createIntRangeFromNonNegative());
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.IndexOrHigh",
                createIntRangeFromNonNegative());
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.IndexOrLow",
                createIntRangeFromGTENegativeOne());
        addAliasedTypeAnnotation(
                "org.checkerframework.checker.index.qual.SubstringIndexFor",
                createIntRangeFromGTENegativeOne());

        // PolyLength is syntactic sugar for both @PolySameLen and @PolyValue
        addAliasedTypeAnnotation("org.checkerframework.checker.index.qual.PolyLength", POLY);

        // EnumVal is treated as StringVal internally by the checker.
        addAliasedTypeAnnotation(EnumVal.class, StringVal.class, true);

        methods = new ValueMethodIdentifier(processingEnv);

        if (this.getClass() == ValueAnnotatedTypeFactory.class) {
            this.postInit();
        }
    }

    /** Gets a helper object that holds references to methods with special handling. */
    ValueMethodIdentifier getMethodIdentifier() {
        return methods;
    }

    @Override
    public AnnotationMirror canonicalAnnotation(AnnotationMirror anno) {
        if (AnnotationUtils.areSameByName(anno, MINLEN_NAME)) {
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
                        MatchesRegex.class,
                        DoubleVal.class,
                        BottomVal.class,
                        UnknownVal.class,
                        IntRangeFromPositive.class,
                        IntRangeFromNonNegative.class,
                        IntRangeFromGTENegativeOne.class,
                        PolyValue.class));
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new ValueTransfer(analysis);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new ValueQualifierHierarchy(this, this.getSupportedTypeQualifiers());
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
                return new StructuralEqualityComparer(areEqualVisitHistory) {
                    @Override
                    protected boolean arePrimeAnnosEqual(
                            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
                        type1.replaceAnnotation(
                                convertToUnknown(
                                        convertSpecialIntRangeToStandardIntRange(
                                                type1.getAnnotationInHierarchy(UNKNOWNVAL))));
                        type2.replaceAnnotation(
                                convertToUnknown(
                                        convertSpecialIntRangeToStandardIntRange(
                                                type2.getAnnotationInHierarchy(UNKNOWNVAL))));

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
                AnnotationUtils.getElementValueArray(fieldInvarAnno, "field", String.class, false);
        List<Integer> minlens =
                AnnotationUtils.getElementValueArray(
                        fieldInvarAnno, "minLen", Integer.class, false);
        List<AnnotationMirror> qualifiers =
                SystemUtil.mapList(
                        (Integer minlen) ->
                                createArrayLenRangeAnnotation(minlen, Integer.MAX_VALUE),
                        minlens);

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
     * @param atm an annotated type mirror that contains an {@code IntRange} annotation
     * @return either the from value from the passed int range annotation, or the minimum value of
     *     the domain of the underlying type (i.e. Integer.MIN_VALUE if the underlying type is int)
     */
    public long getFromValueFromIntRange(AnnotatedTypeMirror atm) {
        AnnotationMirror anno = atm.getAnnotation(IntRange.class);

        if (AnnotationUtils.hasElementValue(anno, "from")) {
            return getIntRangeFromValue(anno);
        }

        TypeMirror type = atm.getUnderlyingType();
        return Range.create(toPrimitiveIntegralTypeKind(type)).from;
    }

    /**
     * Gets the from() element/field out of an IntRange annotation. The from() element/field must
     * exist. Clients should call {@link #getFromValueFromIntRange} if it might not exist.
     *
     * @param anno an IntRange annotation
     * @return its from() element/field
     */
    private long getIntRangeFromValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValueLong(anno, intRangeFromElement, Long.MIN_VALUE);
    }

    /**
     * Gets the to() element/field out of an IntRange annotation. The to() element/field must exist.
     * Clients should call {@link #getToValueFromIntRange} if it might not exist.
     *
     * @param anno an IntRange annotation
     * @return its to() element/field
     */
    private long getIntRangeToValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValueLong(anno, intRangeToElement, Long.MAX_VALUE);
    }

    /**
     * Gets the from() element/field out of an ArrayLenRange annotation.
     *
     * @param anno an ArrayLenRange annotation
     * @return its from() element/field
     */
    private int getArrayLenRangeFromValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValueInt(anno, arrayLenRangeFromElement, 0);
    }

    /**
     * Gets the to() element/field out of an ArrayLenRange annotation.
     *
     * @param anno an ArrayLenRange annotation
     * @return its to() element/field
     */
    private int getArrayLenRangeToValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValueInt(anno, arrayLenRangeToElement, Integer.MAX_VALUE);
    }

    /**
     * Gets the value() element/field out of a MinLen annotation.
     *
     * @param anno a MinLen annotation
     * @return its value() element/field
     */
    private int getMinLenValueValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValueInt(anno, minLenValueElement, 0);
    }

    /**
     * Finds the appropriate value for the {@code to} value of an annotated type mirror containing
     * an {@code IntRange} annotation.
     *
     * @param atm an annotated type mirror that contains an {@code IntRange} annotation
     * @return either the to value from the passed int range annotation, or the maximum value of the
     *     domain of the underlying type (i.e. Integer.MAX_VALUE if the underlying type is int)
     */
    public long getToValueFromIntRange(AnnotatedTypeMirror atm) {
        AnnotationMirror anno = atm.getAnnotation(IntRange.class);

        if (AnnotationUtils.hasElementValue(anno, "to")) {
            return getIntRangeToValue(anno);
        }

        TypeMirror type = atm.getUnderlyingType();
        return Range.create(toPrimitiveIntegralTypeKind(type)).to;
    }

    /**
     * Determine the primitive integral TypeKind for the given integral type.
     *
     * @param type the type to convert, must be an integral type, boxed or primitive
     * @return one of INT, SHORT, BYTE, CHAR, or LONG
     */
    private static TypeKind toPrimitiveIntegralTypeKind(TypeMirror type) {
        TypeKind typeKind = TypeKindUtils.primitiveOrBoxedToTypeKind(type);
        if (typeKind != null && TypeKindUtils.isIntegral(typeKind)) {
            return typeKind;
        }
        throw new BugInCF(type.toString() + " expected to be an integral type.");
    }

    /**
     * Gets the values stored in either an ArrayLen annotation (ints) or an IntVal/DoubleVal/etc.
     * annotation (longs), and casts the result to a long.
     *
     * @param anno annotation mirror from which to get values
     * @return the values in {@code anno} casted to longs
     */
    /* package-private*/ List<Long> getArrayLenOrIntValue(AnnotationMirror anno) {
        if (AnnotationUtils.areSameByName(anno, ARRAYLEN_NAME)) {
            return SystemUtil.mapList(Integer::longValue, getArrayLength(anno));
        } else {
            return getIntValues(anno);
        }
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
     * Converts {@link IntRangeFromPositive}, {@link IntRangeFromNonNegative}, or {@link
     * IntRangeFromGTENegativeOne} to {@link IntRange}. Any other annotation is just return.
     *
     * @param anm any annotation mirror
     * @return the int range annotation is that equivalent to {@code anm}, or {@code anm} if one
     *     doesn't exist
     */
    /* package-private */ AnnotationMirror convertSpecialIntRangeToStandardIntRange(
            AnnotationMirror anm) {
        if (AnnotationUtils.areSameByName(anm, INTRANGE_FROMPOS_NAME)) {
            return createIntRangeAnnotation(1, Integer.MAX_VALUE);
        }

        if (AnnotationUtils.areSameByName(anm, INTRANGE_FROMNONNEG_NAME)) {
            return createIntRangeAnnotation(0, Integer.MAX_VALUE);
        }

        if (AnnotationUtils.areSameByName(anm, INTRANGE_FROMGTENEGONE_NAME)) {
            return createIntRangeAnnotation(-1, Integer.MAX_VALUE);
        }
        return anm;
    }

    /**
     * If {@code anno} is equivalent to UnknownVal, return UnknownVal; otherwise, return {@code
     * anno}.
     *
     * @param anno any annotation mirror
     * @return UnknownVal if {@code anno} is equivalent to it; otherwise, return {@code anno}
     */
    /* package-private */ AnnotationMirror convertToUnknown(AnnotationMirror anno) {
        if (AnnotationUtils.areSameByName(anno, ARRAYLENRANGE_NAME)) {
            Range range = getRange(anno);
            if (range.from == 0 && range.to >= Integer.MAX_VALUE) {
                return UNKNOWNVAL;
            }
        } else if (AnnotationUtils.areSameByName(anno, INTRANGE_NAME)) {
            Range range = getRange(anno);
            if (range.isLongEverything()) {
                return UNKNOWNVAL;
            }
        }
        return anno;
    }

    /**
     * Returns the estimate for the length of a string or array with whose annotated type is {@code
     * type}.
     *
     * @param type annotated typed
     * @return the estimate for the length of a string or array with whose annotated type is {@code
     *     type}.
     */
    /* package-private */ AnnotationMirror createArrayLengthResultAnnotation(
            AnnotatedTypeMirror type) {
        AnnotationMirror arrayAnno = type.getAnnotationInHierarchy(UNKNOWNVAL);
        switch (AnnotationUtils.annotationName(arrayAnno)) {
            case ARRAYLEN_NAME:
                // array.length, where array : @ArrayLen(x)
                List<Integer> lengths = ValueAnnotatedTypeFactory.getArrayLength(arrayAnno);
                return createNumberAnnotationMirror(new ArrayList<>(lengths));
            case ARRAYLENRANGE_NAME:
                // array.length, where array : @ArrayLenRange(x)
                Range range = getRange(arrayAnno);
                return createIntRangeAnnotation(range);
            case STRINGVAL_NAME:
                List<String> strings = ValueAnnotatedTypeFactory.getStringValues(arrayAnno);
                List<Integer> lengthsS = ValueCheckerUtils.getLengthsForStringValues(strings);
                return createNumberAnnotationMirror(new ArrayList<>(lengthsS));
            default:
                return createIntRangeAnnotation(0, Integer.MAX_VALUE);
        }
    }

    /**
     * Returns a constant value annotation with the {@code value}. The class of the annotation
     * reflects the {@code resultType} given.
     *
     * @param resultType used to select which kind of value annotation is returned
     * @param value value to use
     * @return a constant value annotation with the {@code value}
     */
    /* package-private */ AnnotationMirror createResultingAnnotation(
            TypeMirror resultType, Object value) {
        return createResultingAnnotation(resultType, Collections.singletonList(value));
    }

    /**
     * Returns a constant value annotation with the {@code values}. The class of the annotation
     * reflects the {@code resultType} given.
     *
     * @param resultType used to select which kind of value annotation is returned
     * @param values must be a homogeneous list: every element of it has the same class
     * @return a constant value annotation with the {@code values}
     */
    /* package-private */ AnnotationMirror createResultingAnnotation(
            TypeMirror resultType, List<?> values) {
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
            List<String> stringVals = SystemUtil.mapList((Object o) -> (String) o, values);
            return createStringAnnotation(stringVals);
        } else if (TypesUtils.getClassFromType(resultType) == char[].class) {
            List<String> stringVals =
                    SystemUtil.mapList(
                            (Object o) -> {
                                if (o instanceof char[]) {
                                    return new String((char[]) o);
                                } else {
                                    return o.toString();
                                }
                            },
                            values);
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
                List<Boolean> boolVals = SystemUtil.mapList((Object o) -> (Boolean) o, values);
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
    /* package-private */ AnnotationMirror convertIntValToDoubleVal(AnnotationMirror intValAnno) {
        List<Long> intValues = getIntValues(intValAnno);
        return createDoubleValAnnotation(convertLongListToDoubleList(intValues));
    }

    /**
     * Convert a {@code List<Long>} to a {@code List<Double>}.
     *
     * @param intValues a list of long integers
     * @return a list of double floating-point values
     */
    /* package-private */ List<Double> convertLongListToDoubleList(List<Long> intValues) {
        return SystemUtil.mapList(Long::doubleValue, intValues);
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
            // TODO: This seems wasteful.  Why not create the 3 interesting AnnotationMirrors (with
            // arguments {true}, {false}, and {true, false}, respectively) in advance and return one
            // of them?  (Maybe an advantage of this implementation is that it is identical to
            // some other implementations and therefore might be less error-prone.)
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
            List<Long> longValues =
                    SystemUtil.mapList((Character value) -> (long) (char) value, values);
            return createIntValAnnotation(longValues);
        }
    }

    /**
     * Returns an annotation that represents the given set of values.
     *
     * @param values a homogeneous list: every element of it has the same class
     * @return an annotation that represents the given set of values
     */
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
            List<Long> intValues = SystemUtil.mapList(Number::longValue, values);
            return createIntValAnnotation(intValues);
        } else if (first instanceof Double || first instanceof Float) {
            List<Double> intValues = SystemUtil.mapList(Number::doubleValue, values);
            return createDoubleValAnnotation(intValues);
        }
        throw new UnsupportedOperationException(
                "ValueAnnotatedTypeFactory: unexpected class: " + first.getClass());
    }

    /**
     * Create an {@code @IntRange} annotation from the two (inclusive) bounds. Does not return
     * BOTTOMVAL or UNKNOWNVAL.
     */
    /* package-private */ AnnotationMirror createIntRangeAnnotation(long from, long to) {
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
        } else if (range.isLongEverything() || !range.isWithinInteger()) {
            return UNKNOWNVAL;
        } else {
            return createArrayLenRangeAnnotation(
                    Long.valueOf(range.from).intValue(), Long.valueOf(range.to).intValue());
        }
    }

    /**
     * Creates an {@code MatchesRegex} annotation for the given regular expressions.
     *
     * @param regexes a list of Java regular expressions
     * @return a MatchesRegex annotation with those values
     */
    private AnnotationMirror createMatchesRegexAnnotation(@Nullable List<@Regex String> regexes) {
        if (regexes == null) {
            return UNKNOWNVAL;
        }
        if (regexes.isEmpty()) {
            return BOTTOMVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MatchesRegex.class);
        builder.setValue("value", regexes.toArray(new String[0]));
        return builder.build();
    }

    /**
     * Converts an {@code @StringVal} annotation to an {@code @ArrayLenRange} annotation.
     *
     * @param stringValAnno a StringVal annotation
     * @return an ArrayLenRange annotation representing the possible lengths of the values of the
     *     given StringVal annotation
     */
    /* package-private */ AnnotationMirror convertStringValToArrayLenRange(
            AnnotationMirror stringValAnno) {
        List<String> values = getStringValues(stringValAnno);
        List<Integer> lengths = ValueCheckerUtils.getLengthsForStringValues(values);
        return createArrayLenRangeAnnotation(Collections.min(lengths), Collections.max(lengths));
    }

    /**
     * Converts an {@code @StringVal} annotation to an {@code @ArrayLen} annotation. If the
     * {@code @StringVal} annotation contains string values of more than MAX_VALUES distinct
     * lengths, {@code @ArrayLenRange} annotation is returned instead.
     */
    /* package-private */ AnnotationMirror convertStringValToArrayLen(
            AnnotationMirror stringValAnno) {
        List<String> values = getStringValues(stringValAnno);
        return createArrayLenAnnotation(ValueCheckerUtils.getLengthsForStringValues(values));
    }

    /**
     * Converts an {@code StringVal} annotation to an {@code MatchesRegex} annotation that matches
     * exactly the string values listed in the {@code StringVal}.
     *
     * @param stringValAnno a StringVal annotation
     * @return an equivalent MatchesReges annotation
     */
    /* package-private */ AnnotationMirror convertStringValToMatchesRegex(
            AnnotationMirror stringValAnno) {
        List<String> values = getStringValues(stringValAnno);
        List<@Regex String> valuesAsRegexes = SystemUtil.mapList(Pattern::quote, values);
        return createMatchesRegexAnnotation(valuesAsRegexes);
    }

    /**
     * Converts an {@code @ArrayLen} annotation to an {@code @ArrayLenRange} annotation.
     *
     * @param arrayLenAnno an ArrayLen annotation
     * @return an ArrayLenRange annotation representing the bounds of the given ArrayLen annotation
     */
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
     * Returns a {@link Range} bounded by the values specified in the given {@code @Range}
     * annotation. Also returns an appropriate range if an {@code @IntVal} annotation is passed.
     * Returns {@code null} if the annotation is null or if the annotation is not an {@code
     * IntRange}, {@code IntRangeFromPositive}, {@code IntVal}, or {@code ArrayLenRange}.
     *
     * @param rangeAnno a {@code @Range} annotation
     * @return the {@link Range} that the annotation represents
     */
    public Range getRange(AnnotationMirror rangeAnno) {
        if (rangeAnno == null) {
            return null;
        }
        switch (AnnotationUtils.annotationName(rangeAnno)) {
            case INTRANGE_FROMPOS_NAME:
                return Range.create(1, Integer.MAX_VALUE);
            case INTRANGE_FROMNONNEG_NAME:
                return Range.create(0, Integer.MAX_VALUE);
            case INTRANGE_FROMGTENEGONE_NAME:
                return Range.create(-1, Integer.MAX_VALUE);
            case INTVAL_NAME:
                return ValueCheckerUtils.getRangeFromValues(getIntValues(rangeAnno));
            case INTRANGE_NAME:
                // Assume rangeAnno is well-formed, i.e., 'from' is less than or equal to 'to'.
                return Range.create(getIntRangeFromValue(rangeAnno), getIntRangeToValue(rangeAnno));
            case ARRAYLENRANGE_NAME:
                return Range.create(
                        getArrayLenRangeFromValue(rangeAnno), getArrayLenRangeToValue(rangeAnno));
            default:
                return null;
        }
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
        List<Long> list = AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, false);
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
                AnnotationUtils.getElementValueArray(doubleAnno, "value", Double.class, false);
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
                AnnotationUtils.getElementValueArray(arrayAnno, "value", Integer.class, false);
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
     * @return the values represented by the given {@code @IntVal} annotation
     */
    public static List<Character> getCharValues(AnnotationMirror intAnno) {
        if (intAnno == null) {
            return Collections.emptyList();
        }
        List<Long> intValues =
                AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, false);
        TreeSet<Character> charValues = new TreeSet<>();
        for (Long i : intValues) {
            charValues.add((char) i.intValue());
        }
        return new ArrayList<>(charValues);
    }

    /**
     * Returns the single possible boolean value, or null if there is not exactly one possible
     * value.
     *
     * @see #getBooleanValues
     * @param boolAnno a {@code @BoolVal} annotation, or null
     * @return the single possible boolean value, on null if that is not the case
     */
    public static Boolean getBooleanValue(AnnotationMirror boolAnno) {
        if (boolAnno == null) {
            return null;
        }
        List<Boolean> boolValues =
                AnnotationUtils.getElementValueArray(boolAnno, "value", Boolean.class, false);
        Set<Boolean> boolSet = new TreeSet<>(boolValues);
        if (boolSet.size() == 1) {
            return boolSet.iterator().next();
        }
        return null;
    }

    /**
     * Returns the set of possible boolean values as a sorted list with no duplicate values. Returns
     * the empty list if no values are possible (for dead code). Returns null if any value is
     * possible -- that is, if no estimate can be made -- and this includes when there is no
     * constant-value annotation so the argument is null.
     *
     * @param boolAnno a {@code @BoolVal} annotation, or null
     * @return a list of possible boolean values
     */
    public static List<Boolean> getBooleanValues(AnnotationMirror boolAnno) {
        if (boolAnno == null) {
            return Collections.emptyList();
        }
        List<Boolean> boolValues =
                AnnotationUtils.getElementValueArray(boolAnno, "value", Boolean.class, false);
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
                AnnotationUtils.getElementValueArray(stringAnno, "value", String.class, false);
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

    /**
     * Returns true if {@code anno} is an {@link IntRange}, {@link IntRangeFromPositive}, {@link
     * IntRangeFromNonNegative}, or {@link IntRangeFromGTENegativeOne}.
     *
     * @param anno annotation mirror
     * @return true if {@code anno} is an {@link IntRange}, {@link IntRangeFromPositive}, {@link
     *     IntRangeFromNonNegative}, or {@link IntRangeFromGTENegativeOne}
     */
    public boolean isIntRange(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        return name.equals(INTRANGE_NAME)
                || name.equals(INTRANGE_FROMPOS_NAME)
                || name.equals(INTRANGE_FROMNONNEG_NAME)
                || name.equals(INTRANGE_FROMGTENEGONE_NAME);
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
        switch (AnnotationUtils.annotationName(annotation)) {
            case ARRAYLENRANGE_NAME:
                return Long.valueOf(getRange(annotation).to).intValue();
            case ARRAYLEN_NAME:
                return Collections.max(getArrayLength(annotation));
            case STRINGVAL_NAME:
                return Collections.max(
                        ValueCheckerUtils.getLengthsForStringValues(getStringValues(annotation)));
            default:
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
        switch (AnnotationUtils.annotationName(annotation)) {
            case MINLEN_NAME:
                return getMinLenValueValue(annotation);
            case ARRAYLENRANGE_NAME:
                return Long.valueOf(getRange(annotation).from).intValue();
            case ARRAYLEN_NAME:
                return Collections.min(getArrayLength(annotation));
            case STRINGVAL_NAME:
                return Collections.min(
                        ValueCheckerUtils.getLengthsForStringValues(getStringValues(annotation)));
            default:
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
     *
     * @param atm annotated type
     * @return the smallest possible integral for which the {@code atm} could be the type
     */
    public Long getMinimumIntegralValue(AnnotatedTypeMirror atm) {
        AnnotationMirror anm = atm.getAnnotationInHierarchy(UNKNOWNVAL);
        if (AnnotationUtils.areSameByName(anm, INTVAL_NAME)) {
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
     * @param sequenceExpression Java expression
     * @param tree expression tree or variable declaration
     * @param currentPath path to local scope
     * @return min length of sequenceExpression or 0
     */
    public int getMinLenFromString(String sequenceExpression, Tree tree, TreePath currentPath) {
        AnnotationMirror lengthAnno;
        JavaExpression expressionObj;
        try {
            expressionObj = parseJavaExpressionString(sequenceExpression, currentPath);
        } catch (JavaExpressionParseException e) {
            // ignore parse errors and return 0.
            return 0;
        }

        if (expressionObj instanceof ValueLiteral) {
            ValueLiteral sequenceLiteral = (ValueLiteral) expressionObj;
            Object sequenceLiteralValue = sequenceLiteral.getValue();
            if (sequenceLiteralValue instanceof String) {
                return ((String) sequenceLiteralValue).length();
            }
        } else if (expressionObj instanceof ArrayCreation) {
            ArrayCreation arrayCreation = (ArrayCreation) expressionObj;
            // This is only expected to support array creations in varargs methods
            return arrayCreation.getInitializers().size();
        } else if (expressionObj instanceof ArrayAccess) {
            List<? extends AnnotationMirror> annoList =
                    expressionObj.getType().getAnnotationMirrors();
            for (AnnotationMirror anno : annoList) {
                String ANNO_NAME = AnnotationUtils.annotationName(anno);
                if (ANNO_NAME.equals(MINLEN_NAME)) {
                    return getMinLenValue(canonicalAnnotation(anno));
                } else if (ANNO_NAME.equals(ARRAYLEN_NAME)
                        || ANNO_NAME.equals(ARRAYLENRANGE_NAME)) {
                    return getMinLenValue(anno);
                }
            }
        }

        lengthAnno = getAnnotationFromJavaExpression(expressionObj, tree, ArrayLenRange.class);
        if (lengthAnno == null) {
            lengthAnno = getAnnotationFromJavaExpression(expressionObj, tree, ArrayLen.class);
        }
        if (lengthAnno == null) {
            lengthAnno = getAnnotationFromJavaExpression(expressionObj, tree, StringVal.class);
        }

        if (lengthAnno == null) {
            // Could not find a more precise type, so return 0;
            return 0;
        }

        return getMinLenValue(lengthAnno);
    }

    /**
     * Returns the annotation type mirror for the type of {@code expressionTree} with default
     * annotations applied.
     */
    @Override
    public AnnotatedTypeMirror getDummyAssignedTo(ExpressionTree expressionTree) {
        TypeMirror type = TreeUtils.typeOf(expressionTree);
        if (type.getKind() != TypeKind.VOID) {
            AnnotatedTypeMirror atm = type(expressionTree);
            addDefaultAnnotations(atm);
            return atm;
        }
        return null;
    }
}
