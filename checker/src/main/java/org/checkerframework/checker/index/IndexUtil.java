package org.checkerframework.checker.index;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A collection of utility functions used by several Index Checker subcheckers. */
public class IndexUtil {

    /**
     * Gets the value field of an annotation with a list of strings in its value field. Null is
     * returned if the annotation has no value field.
     *
     * <p>For the Index Checker, this will get a list of array names from an Upper Bound or SameLen
     * annotation. making this safe to call on any Upper Bound or SameLen annotation.
     */
    public static List<String> getValueOfAnnotationWithStringArgument(AnnotationMirror anno) {
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return null;
        }
        return AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
    }

    /**
     * Returns a range representing the possible integral values represented by the passed {@code
     * AnnotatedTypeMirror}. If the passed {@code AnnotatedTypeMirror} does not contain an {@code
     * IntRange} annotation or an {@code IntVal} annotation, returns null.
     */
    public static Range getPossibleValues(
            AnnotatedTypeMirror valueType, ValueAnnotatedTypeFactory valueAnnotatedTypeFactory) {
        if (valueAnnotatedTypeFactory.isIntRange(valueType.getAnnotations())) {
            return ValueAnnotatedTypeFactory.getRange(valueType.getAnnotation(IntRange.class));
        } else {
            List<Long> values =
                    ValueAnnotatedTypeFactory.getIntValues(valueType.getAnnotation(IntVal.class));
            if (values != null) {
                return new Range(Collections.min(values), Collections.max(values));
            } else {
                return null;
            }
        }
    }

    /**
     * Either returns the exact value of the given tree according to the Constant Value Checker, or
     * null if the exact value is not known. This method should only be used by clients who need
     * exactly one value -- such as the LBC's binary operator rules -- and not by those that need to
     * know whether a valueType belongs to a particular qualifier.
     */
    public static Long getExactValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        Range possibleValues = getPossibleValues(valueType, factory);
        if (possibleValues != null && possibleValues.from == possibleValues.to) {
            return possibleValues.from;
        } else {
            return null;
        }
    }

    /**
     * Finds the minimum value in a Value Checker type. If there is no information (such as when the
     * list of possible values is empty or null), returns null. Otherwise, returns the smallest
     * value in the list of possible values.
     */
    public static Long getMinValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        Range possibleValues = getPossibleValues(valueType, factory);
        if (possibleValues != null) {
            return possibleValues.from;
        } else {
            return null;
        }
    }

    /**
     * Finds the maximum value in a Value Checker type. If there is no information (such as when the
     * list of possible values is empty or null), returns null. Otherwise, returns the smallest
     * value in the list of possible values.
     */
    public static Long getMaxValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        Range possibleValues = getPossibleValues(valueType, factory);
        if (possibleValues != null) {
            return possibleValues.to;
        } else {
            return null;
        }
    }

    /**
     * Queries the Value Checker to determine if there is a known minimum length for the array
     * represented by {@code tree}. If not, returns 0.
     */
    public static int getMinLen(Tree tree, ValueAnnotatedTypeFactory valueAnnotatedTypeFactory) {
        AnnotatedTypeMirror minLenType = valueAnnotatedTypeFactory.getAnnotatedType(tree);
        return valueAnnotatedTypeFactory.getMinLenValue(minLenType);
    }

    /** Determines whether the type is a sequence supported by this checker. */
    public static boolean isSequenceType(TypeMirror type) {
        return type.getKind() == TypeKind.ARRAY || TypesUtils.isString(type);
    }

    /** Gets a sequence tree for a length access tree, or null if it is not a length access. */
    public static ExpressionTree getLengthSequenceTree(
            Tree lengthTree, IndexMethodIdentifier imf, ProcessingEnvironment processingEnv) {
        if (TreeUtils.isArrayLengthAccess(lengthTree)) {
            return ((MemberSelectTree) lengthTree).getExpression();
        } else if (imf.isLengthOfMethodInvocation(lengthTree)) {
            return TreeUtils.getReceiverTree((MethodInvocationTree) lengthTree);
        }

        return null;
    }

    /**
     * Looks up the minlen of a member select tree. The tree must be an access to a sequence length.
     */
    public static Integer getMinLenFromTree(Tree tree, ValueAnnotatedTypeFactory valueATF) {
        AnnotatedTypeMirror minLenType = valueATF.getAnnotatedType(tree);
        Long min = valueATF.getMinimumIntegralValue(minLenType);
        if (min == null) {
            return null;
        }
        if (min < 0 || min > Integer.MAX_VALUE) {
            min = 0L;
        }
        return min.intValue();
    }
}
