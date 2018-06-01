package org.checkerframework.checker.index;

import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Holds information from {@link HasSubsequence} annotations. */
public class Subsequence {
    public final String from;
    public final String to;
    public final String array;

    private Subsequence(String from, String to, String array) {
        this.from = from;
        this.to = to;
        this.array = array;
    }

    /**
     * Returns a Subsequence representing the {@link HasSubsequence} annotation on the declaration
     * of {@code varTree} or null if there is not such annotation.
     *
     * @param varTree some tree
     * @param factory AnnotatedTypeFactory
     * @return null or a new Subsequence from the declaration of {@code varTree}
     */
    public static Subsequence getSubsequenceFromTree(Tree varTree, AnnotatedTypeFactory factory) {

        if (!(varTree.getKind() == Tree.Kind.IDENTIFIER
                || varTree.getKind() == Tree.Kind.MEMBER_SELECT
                || varTree.getKind() == Tree.Kind.VARIABLE)) {
            return null;
        }

        Element element = TreeUtils.elementFromTree(varTree);
        AnnotationMirror hasSub = factory.getDeclAnnotation(element, HasSubsequence.class);
        return createSubsequence(hasSub);
    }

    /**
     * @param hasSub {@link HasSubsequence} annotation or null
     * @return a new Subsequence object representing {@code hasSub} or null
     */
    private static Subsequence createSubsequence(AnnotationMirror hasSub) {
        if (hasSub == null) {
            return null;
        }
        String from =
                AnnotationUtils.getElementValueArray(hasSub, "from", String.class, false).get(0);
        String to = AnnotationUtils.getElementValueArray(hasSub, "to", String.class, false).get(0);
        String array =
                AnnotationUtils.getElementValueArray(hasSub, "value", String.class, false).get(0);
        return new Subsequence(from, to, array);
    }

    /**
     * Returns a Subsequence representing the {@link HasSubsequence} annotation on the declaration
     * of {@code rec} or null if there is not such annotation.
     *
     * @param rec some tree
     * @param factory AnnotatedTypeFactory
     * @return null or a new Subsequence from the declaration of {@code varTree}
     */
    public static Subsequence getSubsequenceFromReceiver(
            Receiver rec, AnnotatedTypeFactory factory) {
        if (rec == null) {
            return null;
        }

        Element element;
        if (rec instanceof FieldAccess) {
            element = ((FieldAccess) rec).getField();
        } else if (rec instanceof FlowExpressions.LocalVariable) {
            element = ((LocalVariable) rec).getElement();
        } else {
            return null;
        }
        return createSubsequence(factory.getDeclAnnotation(element, HasSubsequence.class));
    }
}
