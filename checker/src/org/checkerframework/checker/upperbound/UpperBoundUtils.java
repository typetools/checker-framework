package org.checkerframework.checker.upperbound;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class UpperBoundUtils {

    /**
     *  Used to get the list of array names that an annotation applies to.
     *  Can return null if the list would be empty.
     */
    public static String[] getValue(AnnotationMirror anno) {
        if (!hasValueMethod(anno)) {
            return null;
        }
        return getIndexValue(anno, getValueMethod(anno));
    }

    /**
     *  Returns the value method specific to the class of the anno passed in.
     */
    static ExecutableElement getValueMethod(AnnotationMirror anno) {
        if (AnnotationUtils.areSameIgnoringValues(anno, UpperBoundAnnotatedTypeFactory.LTL)) {
            return TreeUtils.getMethod(
                    "org.checkerframework.checker.upperbound.qual.LessThanLength",
                    "value",
                    0,
                    UpperBoundAnnotatedTypeFactory.env);
        }
        if (AnnotationUtils.areSameIgnoringValues(anno, UpperBoundAnnotatedTypeFactory.EL)) {
            return TreeUtils.getMethod(
                    "org.checkerframework.checker.upperbound.qual.EqualToLength",
                    "value",
                    0,
                    UpperBoundAnnotatedTypeFactory.env);
        }
        if (AnnotationUtils.areSameIgnoringValues(anno, UpperBoundAnnotatedTypeFactory.LTEL)) {
            return TreeUtils.getMethod(
                    "org.checkerframework.checker.upperbound.qual.LessThanOrEqualToLength",
                    "value",
                    0,
                    UpperBoundAnnotatedTypeFactory.env);
        }
        return null;
    }

    /**
     *  Returns the value of an annotation, given the annotation and Value method.
     */
    static String[] getIndexValue(AnnotationMirror anno, ExecutableElement valueElement) {
        Object val =
                AnnotationUtils.getElementValuesWithDefaults(anno).get(valueElement).getValue();
        if (val instanceof List) {
            // Bad and evil but not sure how else to do it.
            @SuppressWarnings("unchecked")
            List<Object> l = (List<Object>) val;
            String[] values = new String[l.size()];
            for (int i = 0; i < l.size(); i++) {
                values[i] = l.get(i).toString();
                // The function toString() puts quotes around things that are already strings,
                // and we don't want that.
                values[i] = values[i].replaceAll("\"", "");
            }
            return values;
        } else if (val instanceof Object[]) {
            return Arrays.copyOf((Object[]) val, ((Object[]) val).length, String[].class);
        }
        return null; // Shouldn't ever happen.
    }

    /**
     *  Determines if the given string is a member of the LTL annotation attached to type.
     */
    public static boolean hasValue(AnnotatedTypeMirror type, String name) {
        String[] rgst = getValue(type.getAnnotation(LessThanLength.class));
        for (String st : rgst) {
            if (st.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Checks if a given annotation is one of those that can have a value.
     */
    public static boolean hasValueMethod(AnnotationMirror anno) {
        boolean fLTL = AnnotationUtils.areSameByClass(anno, LessThanLength.class);
        boolean fEL = AnnotationUtils.areSameByClass(anno, EqualToLength.class);
        boolean fLTEL = AnnotationUtils.areSameByClass(anno, LessThanOrEqualToLength.class);
        return fLTL || fEL || fLTEL;
    }
}
