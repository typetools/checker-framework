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
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return null;
        }
        return AnnotationUtils.getElementValueArray(anno, "value", String.class, true)
                .toArray(new String[0]);
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
        boolean fLTEL = AnnotationUtils.areSameByClass(anno, LessThanOrEqualToLength.class);
        return fLTL || fLTEL;
    }
}
