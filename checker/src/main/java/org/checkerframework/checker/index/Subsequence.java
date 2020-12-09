package org.checkerframework.checker.index;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.BaseContext;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionContext;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Holds information from {@link HasSubsequence} annotations. */
public class Subsequence {

    /** Name of the Subsequence. */
    public final String array;
    /** First index of the subsequence in the backing sequence. */
    public final String from;
    /** Last index of the subsequence in the backing sequence. */
    public final String to;

    private Subsequence(String array, String from, String to) {
        this.array = array;
        this.from = from;
        this.to = to;
    }

    /**
     * Returns a Subsequence representing the {@link HasSubsequence} annotation on the declaration
     * of {@code varTree} or null if there is not such annotation.
     *
     * <p>Note that this method does not standardize or viewpoint adapt the arguments to the
     * annotation, unlike getSubsequenceFromReceiver.
     *
     * @param varTree some tree
     * @param factory an AnnotatedTypeFactory
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
        return createSubsequence(hasSub, null, null);
    }

    /**
     * @param hasSub {@link HasSubsequence} annotation or null
     * @return a new Subsequence object representing {@code hasSub} or null
     */
    private static Subsequence createSubsequence(
            AnnotationMirror hasSub, TreePath currentPath, JavaExpressionContext context) {
        if (hasSub == null) {
            return null;
        }
        String from = AnnotationUtils.getElementValue(hasSub, "from", String.class, false);
        String to = AnnotationUtils.getElementValue(hasSub, "to", String.class, false);
        String array = AnnotationUtils.getElementValue(hasSub, "subsequence", String.class, false);

        if (context != null && currentPath != null) {
            from = standardizeAndViewpointAdapt(from, currentPath, context);
            to = standardizeAndViewpointAdapt(to, currentPath, context);
            array = standardizeAndViewpointAdapt(array, currentPath, context);
        }

        return new Subsequence(array, from, to);
    }

    /**
     * Returns a Subsequence representing the {@link HasSubsequence} annotation on the declaration
     * of {@code rec} or null if there is not such annotation.
     *
     * @param expr some tree
     * @param factory an AnnotatedTypeFactory
     * @param currentPath the path at which to viewpoint adapt the subsequence
     * @param context the context in which to viewpoint adapt the subsequence
     * @return null or a new Subsequence from the declaration of {@code varTree}
     */
    public static Subsequence getSubsequenceFromReceiver(
            JavaExpression expr,
            AnnotatedTypeFactory factory,
            TreePath currentPath,
            JavaExpressionContext context) {
        if (expr == null) {
            return null;
        }

        Element element;
        if (expr instanceof FieldAccess) {
            element = ((FieldAccess) expr).getField();
        } else {
            return null;
        }
        return createSubsequence(
                factory.getDeclAnnotation(element, HasSubsequence.class), currentPath, context);
    }

    /*
     * Helper function to standardize and viewpoint adapt a String given a path and a context.
     * Wraps JavaExpressionParseUtil#parse. If a parse exception is encountered, this returns
     * its argument.
     */
    private static String standardizeAndViewpointAdapt(
            String s, TreePath currentPath, JavaExpressionContext context) {
        try {
            return JavaExpressionParseUtil.parse(s, context, currentPath, false).toString();
        } catch (JavaExpressionParseException e) {
            return s;
        }
    }

    /**
     * If the passed expression is a FieldAccess, returns the context associated with it. Otherwise
     * returns null.
     *
     * <p>Used to standardize and viewpoint adapt arguments to HasSubsequence annotations.
     */
    public static JavaExpressionContext getContextFromJavaExpression(
            JavaExpression expr, BaseContext checker) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof FieldAccess) {
            FieldAccess fa = (FieldAccess) expr;
            return new JavaExpressionParseUtil.JavaExpressionContext(
                    fa.getReceiver(), null, checker);

        } else {
            return null;
        }
    }

    /**
     * Returns the additive inverse of the given String. That is, if the result of this method is
     * some String s', then s + s' == 0 will evaluate to true. Note that this relies on the fact
     * that the JavaExpression parser cannot parse multiplication, so it naively just changes '-' to
     * '+' and vice-versa.
     *
     * <p>The passed String is standardized and viewpoint adapted before this transformation is
     * applied.
     */
    public static String negateString(
            String s, TreePath currentPath, JavaExpressionContext context) {
        String original = standardizeAndViewpointAdapt(s, currentPath, context);
        String result = "";
        if (!original.startsWith("-")) {
            result += '-';
        }
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (c == '-') {
                result += '+';
            } else if (c == '+') {
                result += '-';
            } else {
                result += c;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "Subsequence(array=" + this.array + ", from=" + this.from + ", to=" + this.to + ")";
    }
}
