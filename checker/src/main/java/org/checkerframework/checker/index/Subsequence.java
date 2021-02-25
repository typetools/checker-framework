package org.checkerframework.checker.index;

import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionContext;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
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
    public static Subsequence getSubsequenceFromTree(
            Tree varTree, BaseAnnotatedTypeFactoryForIndexChecker factory) {

        if (!(varTree.getKind() == Tree.Kind.IDENTIFIER
                || varTree.getKind() == Tree.Kind.MEMBER_SELECT
                || varTree.getKind() == Tree.Kind.VARIABLE)) {
            return null;
        }

        Element element = TreeUtils.elementFromTree(varTree);
        AnnotationMirror hasSub = factory.getDeclAnnotation(element, HasSubsequence.class);
        return createSubsequence(hasSub, factory);
    }

    /**
     * Factory method to create a representation of a subsequence.
     *
     * @param hasSub {@link HasSubsequence} annotation or null
     * @param factory the type factory
     * @return a new Subsequence object representing {@code hasSub} or null
     */
    private static Subsequence createSubsequence(
            AnnotationMirror hasSub, BaseAnnotatedTypeFactoryForIndexChecker factory) {
        if (hasSub == null) {
            return null;
        }
        String from = factory.hasSubsequenceFromValue(hasSub);
        String to = factory.hasSubsequenceToValue(hasSub);
        String array = factory.hasSubsequenceSubsequenceValue(hasSub);

        return new Subsequence(array, from, to);
    }

    /**
     * Returns a Subsequence representing the {@link HasSubsequence} annotation on the declaration
     * of {@code rec} or null if there is not such annotation.
     *
     * @param expr some tree
     * @param factory an AnnotatedTypeFactory
     * @return null or a new Subsequence from the declaration of {@code varTree}
     */
    public static Subsequence getSubsequenceFromReceiver(
            JavaExpression expr, BaseAnnotatedTypeFactoryForIndexChecker factory) {
        if (!(expr instanceof FieldAccess)) {
            return null;
        }

        FieldAccess fa = (FieldAccess) expr;

        Element element = fa.getField();
        AnnotationMirror hasSub = factory.getDeclAnnotation(element, HasSubsequence.class);
        if (hasSub == null) {
            return null;
        }

        JavaExpressionContext context =
                new JavaExpressionContext(fa.getReceiver(), null, factory.getChecker());

        String array =
                standardizeAndViewpointAdapt(
                        factory.hasSubsequenceSubsequenceValue(hasSub), context);
        String from =
                standardizeAndViewpointAdapt(factory.hasSubsequenceFromValue(hasSub), context);
        String to = standardizeAndViewpointAdapt(factory.hasSubsequenceToValue(hasSub), context);

        return new Subsequence(array, from, to);
    }

    /**
     * Helper function to standardize and viewpoint-adapt a String given a context. Wraps {@link
     * JavaExpressionParseUtil#parse}. If a parse exception is encountered, this returns its
     * argument.
     *
     * @param s a Java expression string
     * @param context the parse context
     * @return the argument, standardized and viewpoint-adapted
     */
    private static String standardizeAndViewpointAdapt(String s, JavaExpressionContext context) {
        try {
            return JavaExpressionParseUtil.parse(s, context).toString();
        } catch (JavaExpressionParseException e) {
            return s;
        }
    }

    /**
     * Returns the additive inverse of the given String. That is, if the result of this method is
     * some String s', then s + s' == 0 will evaluate to true. Note that this relies on the fact
     * that the JavaExpression parser cannot parse multiplication, so it naively just changes '-' to
     * '+' and vice-versa.
     *
     * @param s a Java expression string
     * @return the negated string
     */
    public static String negateString(String s) {
        String original = s;
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
