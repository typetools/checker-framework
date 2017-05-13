package org.checkerframework.checker.index;

import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

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
     * Get the list of possible values from an AnnotatedTypeMirror containing an IntVal. Empty list
     * means no possible values (dead code). Returns null if the AnnotatedTypeMirror doesn't contain
     * an IntVal.
     */
    public static List<Long> getPossibleValues(AnnotatedTypeMirror valueType) {
        return ValueAnnotatedTypeFactory.getIntValues(valueType.getAnnotation(IntVal.class));
    }

    /**
     * Either returns the exact value of the given tree according to the Constant Value Checker, or
     * null if the exact value is not known. This method should only be used by clients who need
     * exactly one value -- such as the LBC's binary operator rules -- and not by those that need to
     * know whether a valueType belongs to a particular qualifier.
     */
    public static Long getExactValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        List<Long> possibleValues = getPossibleValues(valueType);
        if (possibleValues != null && possibleValues.size() == 1) {
            return possibleValues.get(0);
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
        List<Long> possibleValues = getPossibleValues(valueType);
        if (possibleValues != null && possibleValues.size() != 0) {
            return Collections.min(possibleValues);
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
        List<Long> possibleValues = getPossibleValues(valueType);
        if (possibleValues != null && possibleValues.size() != 0) {
            return Collections.max(possibleValues);
        } else {
            return null;
        }
    }

    /**
     * Queries the Value Checker to determine if there is a known minimum length for the array
     * represented by {@code tree}. If not, returns null.
     */
    public static Integer getMinLen(
            Tree tree, ValueAnnotatedTypeFactory valueAnnotatedTypeFactory) {
        AnnotatedTypeMirror minLenType = valueAnnotatedTypeFactory.getAnnotatedType(tree);
        return valueAnnotatedTypeFactory.getMinLenValue(minLenType);
    }
}
