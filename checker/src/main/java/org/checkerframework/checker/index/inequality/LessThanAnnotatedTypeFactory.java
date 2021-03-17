package org.checkerframework.checker.index.inequality;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Attribute;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.index.BaseAnnotatedTypeFactoryForIndexChecker;
import org.checkerframework.checker.index.OffsetDependentTypesHelper;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.LessThanBottom;
import org.checkerframework.checker.index.qual.LessThanUnknown;
import org.checkerframework.checker.index.upperbound.OffsetEquation;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/** The type factory for the Less Than Checker. */
public class LessThanAnnotatedTypeFactory extends BaseAnnotatedTypeFactoryForIndexChecker {
    /** The @LessThanBottom annotation. */
    private final AnnotationMirror LESS_THAN_BOTTOM =
            AnnotationBuilder.fromClass(elements, LessThanBottom.class);
    /** The @LessThanUnknown annotation. */
    public final AnnotationMirror LESS_THAN_UNKNOWN =
            AnnotationBuilder.fromClass(elements, LessThanUnknown.class);

    /** The LessThan#value() argument/element. */
    private final ExecutableElement lessThanValueElement;

    public LessThanAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        lessThanValueElement =
                TreeUtils.getMethod(LessThan.class.getCanonicalName(), "value", 0, processingEnv);
        postInit();
    }

    /**
     * Returns the Value Checker's annotated type factory.
     *
     * @return the Value Checker's annotated type factory
     */
    public ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(ValueChecker.class);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(LessThan.class, LessThanUnknown.class, LessThanBottom.class));
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        // Allows + or - in a @LessThan.
        return new OffsetDependentTypesHelper(this);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new LessThanQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
    }

    /** LessThanQualifierHierarchy. */
    class LessThanQualifierHierarchy extends ElementQualifierHierarchy {

        /**
         * Creates a LessThanQualifierHierarchy from the given classes.
         *
         * @param qualifierClasses classes of annotations that are the qualifiers
         * @param elements element utils
         */
        public LessThanQualifierHierarchy(
                Set<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            List<String> subList = getLessThanExpressions(subAnno);
            if (subList == null) {
                return true;
            }
            List<String> superList = getLessThanExpressions(superAnno);
            if (superList == null) {
                return false;
            }

            return subList.containsAll(superList);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            }

            List<String> a1List = getLessThanExpressionStrings(a1);
            List<String> a2List = getLessThanExpressionStrings(a2);
            List<String> lub = new ArrayList<>(a1List);
            lub.retainAll(a2List);

            return createLessThanQualifier(lub);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            }

            List<String> a1List = getLessThanExpressionStrings(a1);
            List<String> a2List = getLessThanExpressionStrings(a2);
            List<String> glb = new ArrayList<>(a1List);
            glb.addAll(a2List);

            return createLessThanQualifier(glb);
        }
    }

    /**
     * Returns true if {@code left} is less than {@code right}.
     *
     * @param left the first tree to compare
     * @param right the second tree to compare
     * @return is left less than right?
     */
    public boolean isLessThan(Tree left, String right) {
        AnnotatedTypeMirror leftATM = getAnnotatedType(left);
        return isLessThan(leftATM.getAnnotationInHierarchy(LESS_THAN_UNKNOWN), right);
    }

    /**
     * Returns true if {@code left} is less than {@code right}.
     *
     * @param left the first value to compare (an annotation)
     * @param right the second value to compare (an expression)
     * @return is left less than right?
     */
    public boolean isLessThan(AnnotationMirror left, String right) {
        javac.util.List<Attribute.Constant> expressions = getLessThanExpressions(left);
        if (expressions == null) {
            // `left` is @LessThanBottom
            return true;
        }
        return javacListContains(expressions, right);
    }

    /**
     * Returns true if {@code smaller < bigger}.
     *
     * @param smaller the first value to compare
     * @param bigger the second value to compare
     * @return {@code smaller < bigger}, using information from the Value Checker
     */
    public boolean isLessThanByValue(Tree smaller, String bigger, TreePath path) {
        Long smallerValue = ValueCheckerUtils.getMinValue(smaller, getValueAnnotatedTypeFactory());
        if (smallerValue == null) {
            return false;
        }

        OffsetEquation offsetEquation = OffsetEquation.createOffsetFromJavaExpression(bigger);
        if (offsetEquation.isInt()) {
            // bigger is an int literal
            return smallerValue < offsetEquation.getInt();
        }
        // If bigger is "expression + literal", then smaller < expression + literal
        // can be reduced to smaller - literal < expression + literal - literal
        smallerValue = smallerValue - offsetEquation.getInt();
        offsetEquation =
                offsetEquation.copyAdd(
                        '-', OffsetEquation.createOffsetForInt(offsetEquation.getInt()));

        long minValueOfBigger = getMinValueFromString(offsetEquation.toString(), smaller, path);
        return smallerValue < minValueOfBigger;
    }

    /**
     * Returns the minimum value of {@code expression} at {@code tree}.
     *
     * @param expression the expression whose minimum value to retrieve
     * @param tree where to determine the value
     * @param path the path to {@code tree}
     */
    private long getMinValueFromString(String expression, Tree tree, TreePath path) {
        JavaExpression expressionJe;
        try {
            expressionJe =
                    getValueAnnotatedTypeFactory().parseJavaExpressionString(expression, path);
        } catch (JavaExpressionParseException e) {
            return Long.MIN_VALUE;
        }

        AnnotationMirror intRange =
                getValueAnnotatedTypeFactory()
                        .getAnnotationFromJavaExpression(expressionJe, tree, IntRange.class);
        if (intRange != null) {
            return getValueAnnotatedTypeFactory().getRange(intRange).from;
        }
        AnnotationMirror intValue =
                getValueAnnotatedTypeFactory()
                        .getAnnotationFromJavaExpression(expressionJe, tree, IntVal.class);
        if (intValue != null) {
            List<Long> possibleValues = ValueAnnotatedTypeFactory.getIntValues(intValue);
            return Collections.min(possibleValues);
        }

        if (expressionJe instanceof FieldAccess) {
            FieldAccess fieldAccess = ((FieldAccess) expressionJe);
            if (fieldAccess.getReceiver().getType().getKind() == TypeKind.ARRAY) {
                // array.length might not be in the store, so check for the length of the array.
                AnnotationMirror arrayRange =
                        getValueAnnotatedTypeFactory()
                                .getAnnotationFromJavaExpression(
                                        fieldAccess.getReceiver(), tree, ArrayLenRange.class);
                if (arrayRange != null) {
                    return getValueAnnotatedTypeFactory().getRange(arrayRange).from;
                }
                AnnotationMirror arrayLen =
                        getValueAnnotatedTypeFactory()
                                .getAnnotationFromJavaExpression(
                                        expressionJe, tree, ArrayLen.class);
                if (arrayLen != null) {
                    List<Integer> possibleValues =
                            ValueAnnotatedTypeFactory.getArrayLength(arrayLen);
                    return Collections.min(possibleValues);
                }
                // Even arrays that we know nothing about must have at least zero length.
                return 0;
            }
        }

        return Long.MIN_VALUE;
    }

    /**
     * Returns true if left is less than or equal to right.
     *
     * @param left the first value to compare
     * @param right the second value to compare
     * @return is left less than or equal to right?
     */
    public boolean isLessThanOrEqual(Tree left, String right) {
        AnnotatedTypeMirror leftATM = getAnnotatedType(left);
        return isLessThanOrEqual(leftATM.getAnnotationInHierarchy(LESS_THAN_UNKNOWN), right);
    }

    /**
     * Returns true if left is less than or equal to right.
     *
     * @param left the first value to compare
     * @param right the second value to compare
     * @return is left less than or equal to right?
     */
    public boolean isLessThanOrEqual(AnnotationMirror left, String right) {
        com.sun.tools.javac.util.List<Attribute.Constant> expressionConstants =
                getLessThanExpressions(left);
        if (expressionConstants == null) {
            // left is bottom so it is always less than right.
            return true;
        }
        if (javacListContains(expressionConstants, right)) {
            return true;
        }

        // Check normalized version of the annotation.
        List<String> expressions = getLessThanExpressions(left);
        // {@code @LessThan("end + 1")} is equivalent to {@code @LessThanOrEqual("end")}.
        for (String expression : expressions) {
            if (expression.endsWith(" + 1")
                    && expression.substring(0, expression.length() - 4).equals(right)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a sorted, modifiable list of expressions that {@code expression} is less than. If the
     * {@code expression} is annotated with {@link LessThanBottom}, null is returned.
     *
     * @param expression an expression
     * @return expressions that {@code expression} is less than
     */
    public List<String> getLessThanExpressions(ExpressionTree expression) {
        AnnotatedTypeMirror annotatedTypeMirror = getAnnotatedType(expression);
        return getLessThanExpressions(
                annotatedTypeMirror.getAnnotationInHierarchy(LESS_THAN_UNKNOWN));
    }

    /**
     * Returns a sorted, modifiable list of expressions that {@code expression} is less than. If the
     * {@code expression} is annotated with {@link LessThanBottom}, null is returned.
     *
     * @param expression an expression
     * @return expressions that {@code expression} is less than
     */
    public List<String> getLessThanExpressionsStrings(ExpressionTree expression) {
        AnnotatedTypeMirror annotatedTypeMirror = getAnnotatedType(expression);
        return getLessThanExpressionsStrings(
                annotatedTypeMirror.getAnnotationInHierarchy(LESS_THAN_UNKNOWN));
    }

    /**
     * Creates a less than qualifier given the expressions.
     *
     * <p>If expressions is null, {@link LessThanBottom} is returned. If expressions is empty,
     * {@link LessThanUnknown} is returned. Otherwise, {@code @LessThan(expressions)} is returned.
     *
     * @param expressions a list of expressions
     * @return a @LessThan qualifier with the given arguments
     */
    public AnnotationMirror createLessThanQualifier(List<String> expressions) {
        if (expressions == null) {
            return LESS_THAN_BOTTOM;
        } else if (expressions.isEmpty()) {
            return LESS_THAN_UNKNOWN;
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, LessThan.class);
            builder.setValue("value", expressions);
            return builder.build();
        }
    }

    /** Returns {@code @LessThan(expression)}. */
    public AnnotationMirror createLessThanQualifier(String expression) {
        return createLessThanQualifier(Collections.singletonList(expression));
    }

    /**
     * If the annotation is LessThan, returns a list of expressions in the annotation. If the
     * annotation is {@link LessThanBottom}, returns null. If the annotation is {@link
     * LessThanUnknown}, returns the empty list.
     */
    public List<Attribute.Constant> getLessThanExpressions(AnnotationMirror annotation) {
        if (AnnotationUtils.areSameByClass(annotation, LessThanBottom.class)) {
            return null;
        } else if (AnnotationUtils.areSameByClass(annotation, LessThanUnknown.class)) {
            return com.sun.tools.javac.util.List.nil();
        } else {
            // The annotation is LessThan.
            @SuppressWarnings("unchecked")
            List<Attribute.Constant> lessThanValue =
                    (List<Attribute.Constant>)
                            annotation.getElementValues().get(lessThanValueElement).getValue();
            return lessThanValue;
        }
    }

    /**
     * Returns a modifiable list of expressions in the annotation sorted. If the annotation is
     * {@link LessThanBottom}, return null. If the annotation is {@link LessThanUnknown} return the
     * empty list.
     */
    public static List<String> getLessThanExpressionsStrings(AnnotationMirror annotation) {
        if (AnnotationUtils.areSameByClass(annotation, LessThanBottom.class)) {
            return null;
        } else if (AnnotationUtils.areSameByClass(annotation, LessThanUnknown.class)) {
            return new ArrayList<>();
        } else {
            List<String> list =
                    AnnotationUtils.getElementValueArrayList(
                            annotation, "value", String.class, true);
            return list;
        }
    }
}
