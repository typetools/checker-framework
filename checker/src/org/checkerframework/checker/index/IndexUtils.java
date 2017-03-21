package org.checkerframework.checker.index;

import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.minlen.MinLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.qual.MinLen;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

/** A collection of utility functions used by several index checker subcheckers. */
public class IndexUtils {

    /**
     * Used to get the list of array names that an annotation applies to. Can return null if the
     * list would be empty. Assumes that the annotation mirror is from the upperbound or samelen
     * hierarchy.
     */
    public static List<String> getValueOfAnnotationWithStringArgument(AnnotationMirror anno) {
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return null;
        }
        return AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
    }

    /**
     * Get the list of possible values from a Value Checker type. Empty list means no possible
     * values (dead code). Returns null if there is no estimate.
     */
    public static List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
        return ValueAnnotatedTypeFactory.getIntValues(valueType.getAnnotation(IntVal.class));
    }

    /**
     * Either returns the exact value of the given tree according to the Constant Value Checker, or
     * null if the exact value is not known. This method should only be used by clients who need
     * exactly one value -- such as the LBC's binary operator rules -- and not by those that need to
     * know whether a valueType belongs to a particular qualifier.
     */
    public static Long getExactValueOrNullFromTree(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
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
    public static Long getMinValueOrNullFromTree(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues != null && possibleValues.size() != 0) {
            // There must be at least one element in the list, because of the previous check.
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
    public static Long getMaxValueOrNullFromTree(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues != null && possibleValues.size() != 0) {
            // There must be at least one element in the list, because of the previous check.
            return Collections.max(possibleValues);
        } else {
            return null;
        }
    }

    /**
     * Queries the MinLen Checker to determine if there is a known minimum length for the array. If
     * not, returns -1.
     */
    public static int getMinLenFromTree(
            Tree tree, MinLenAnnotatedTypeFactory minLenAnnotatedTypeFactory) {
        AnnotatedTypeMirror minLenType = minLenAnnotatedTypeFactory.getAnnotatedType(tree);
        AnnotationMirror anm = minLenType.getAnnotation(MinLen.class);
        return getMinLenFromAnnotationMirror(anm);
    }

    /**
     * Returns the MinLen value of the given annotation mirror; or -1 if the annotation mirror is
     * null.
     */
    public static int getMinLenFromAnnotationMirror(AnnotationMirror anm) {
        if (anm == null) {
            return -1;
        }
        return AnnotationUtils.getElementValue(anm, "value", Integer.class, true);
    }
}
