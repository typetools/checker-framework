package org.checkerframework.checker.upperbound;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class UpperBoundUtils {
    public static String[] getValue(AnnotationMirror anno) {
        if (!hasValueMethod(anno)) {
            throw new IllegalArgumentException("anno must have a value method");
        }
        return getIndexValue(anno, getValueMethod(anno));
    }

    // returns the value method specific to the class of the anno passed in
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

    // returns the value of an annotation, given the annotation and Value method
    static String[] getIndexValue(AnnotationMirror anno, ExecutableElement valueElement) {
        return (String[])
                AnnotationUtils.getElementValuesWithDefaults(anno).get(valueElement).getValue();
    }

    // determines if the given string is a member of the LTL annotation attached to type
    public static boolean hasValue(AnnotatedTypeMirror type, String name) {
        String[] rgst = getValue(type.getAnnotation(LessThanLength.class));
        for (String st : rgst) {
            if (st.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasValueMethod(AnnotationMirror anno) {
        boolean fLTL = AnnotationUtils.areSameByClass(anno, LessThanLength.class);
        boolean fEL = AnnotationUtils.areSameByClass(anno, EqualToLength.class);
        boolean fLTEL = AnnotationUtils.areSameByClass(anno, LessThanOrEqualToLength.class);
        return fLTL || fEL || fLTEL;
    }
}
