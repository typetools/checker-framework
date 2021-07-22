package org.checkerframework.checker.index.upperbound;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TreeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An offset equation is 2 sets of Java expression strings, one set of added terms and one set of
 * subtracted terms, and a single integer constant. The Java expression strings have been
 * standardized and viewpoint-adapted.
 *
 * <p>An OffsetEquation is mutable.
 */
public class OffsetEquation {
    public static final OffsetEquation ZERO = createOffsetForInt(0);
    public static final OffsetEquation NEG_1 = createOffsetForInt(-1);
    public static final OffsetEquation ONE = createOffsetForInt(1);

    private final List<String> addedTerms;
    private final List<String> subtractedTerms;
    private int intValue = 0;
    private String error = null;

    private OffsetEquation() {
        addedTerms = new ArrayList<>(1);
        subtractedTerms = new ArrayList<>(1);
    }

    /**
     * Create a new OffsetEquation that is a copy of the given one.
     *
     * @param other the OffsetEquation to copy
     */
    protected OffsetEquation(OffsetEquation other) {
        this.addedTerms = new ArrayList<>(other.addedTerms);
        this.subtractedTerms = new ArrayList<>(other.subtractedTerms);
        this.error = other.error;
        this.intValue = other.intValue;
    }

    public boolean hasError() {
        return error != null;
    }

    public String getError() {
        return error;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OffsetEquation that = (OffsetEquation) o;

        if (intValue != that.intValue) {
            return false;
        }
        if (addedTerms.size() != that.addedTerms.size()
                || !addedTerms.containsAll(that.addedTerms)
                || !that.addedTerms.containsAll(addedTerms)) {
            return false;
        }
        if (subtractedTerms.size() != that.subtractedTerms.size()
                || !subtractedTerms.containsAll(that.subtractedTerms)
                || !that.subtractedTerms.containsAll(subtractedTerms)) {
            return false;
        }
        return error != null ? error.equals(that.error) : that.error == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addedTerms, subtractedTerms, intValue, error);
    }

    @Override
    public String toString() {
        if (addedTerms.isEmpty() && subtractedTerms.isEmpty()) {
            return String.valueOf(intValue);
        }
        List<String> sortedAdds = new ArrayList<>(addedTerms);
        Collections.sort(sortedAdds);

        List<String> sortedSubs = new ArrayList<>(subtractedTerms);
        Collections.sort(sortedSubs);

        String adds = String.join(" + ", sortedAdds);
        String minus = String.join(" - ", sortedSubs);
        if (sortedSubs.size() == 1 && sortedAdds.isEmpty()) {
            minus = "-" + minus;
        } else if (!sortedSubs.isEmpty()) {
            minus = " - " + minus;
        }
        String terms = (adds + minus).trim();
        if (intValue != 0) {
            char op = intValue > 0 ? '+' : '-';
            if (terms.isEmpty()) {
                terms += intValue;
            } else {
                terms += " " + op + " " + Math.abs(intValue);
            }
        }
        return terms;
    }

    /**
     * Makes a copy of this offset and removes any added terms that are accesses to the length of
     * the listed sequences. If any terms were removed, then the copy is returned. Otherwise, null
     * is returned.
     *
     * @param sequences list of sequences (arrays or strings)
     * @return a copy of this equation with array.length and string.length() removed or null if no
     *     array.lengths or string.length() could be removed
     */
    public OffsetEquation removeSequenceLengths(List<String> sequences) {
        OffsetEquation copy = new OffsetEquation(this);
        boolean simplified = false;
        for (String sequence : sequences) {
            String arrayLen = sequence + ".length";
            if (addedTerms.contains(arrayLen)) {
                copy.addedTerms.remove(arrayLen);
                simplified = true;
            }
            String stringLen = sequence + ".length()";
            if (addedTerms.contains(stringLen)) {
                copy.addedTerms.remove(stringLen);
                simplified = true;
            }
        }
        return simplified ? copy : null;
    }
    /**
     * Adds or subtracts the other equation to a copy of this one.
     *
     * <p>If subtraction is specified, then every term in other is subtracted.
     *
     * @param op '-' for subtraction or '+' for addition
     * @param other equation to add or subtract
     * @return a copy of this equation +/- other
     */
    public OffsetEquation copyAdd(char op, OffsetEquation other) {
        assert op == '-' || op == '+';
        OffsetEquation copy = new OffsetEquation(this);
        if (op == '+') {
            copy.plus(other);
        } else {
            copy.minus(other);
        }
        return copy;
    }

    private void plus(OffsetEquation eq) {
        addInt(eq.intValue);
        for (String term : eq.addedTerms) {
            addTerm('+', term);
        }
        for (String term : eq.subtractedTerms) {
            addTerm('-', term);
        }
    }

    private void minus(OffsetEquation eq) {
        addInt(-1 * eq.intValue);
        for (String term : eq.addedTerms) {
            addTerm('-', term);
        }
        for (String term : eq.subtractedTerms) {
            addTerm('+', term);
        }
    }

    /**
     * Returns whether or not this equation is known to be less than or equal to the other equation.
     *
     * @param other equation
     * @return whether or not this equation is known to be less than or equal to the other equation
     */
    public boolean lessThanOrEqual(OffsetEquation other) {
        return (isInt() && other.isInt() && intValue <= other.getInt()) || this.equals(other);
    }

    /**
     * Returns true if this equation is a single int value.
     *
     * @return true if this equation is a single int value
     */
    public boolean isInt() {
        return addedTerms.isEmpty() && subtractedTerms.isEmpty();
    }

    /**
     * Returns the int value associated with this equation.
     *
     * <p>The equation may or may not have other terms. Use {@link #isInt()} to determine if the
     * equation is only this int value.
     *
     * @return the int value associated with this equation
     */
    public int getInt() {
        return intValue;
    }

    /**
     * Returns true if this equation is exactly -1.
     *
     * @return true if this equation is exactly -1
     */
    public boolean isNegOne() {
        return isInt() && getInt() == -1;
    }

    /**
     * Returns true if this equation non-negative.
     *
     * @return true if this equation non-negative
     */
    public boolean isNonNegative() {
        return isInt() && getInt() >= 0;
    }

    /**
     * Returns true if this equation is negative or zero.
     *
     * @return true if this equation is negative or zero
     */
    public boolean isNegativeOrZero() {
        return isInt() && getInt() <= 0;
    }

    /**
     * Adds the term to this equation. If string is an integer, then it is added or subtracted,
     * depending on operator, from the int value of this equation. Otherwise, the term is placed in
     * the added or subtracted terms set, depending on operator.
     *
     * @param operator '+' or '-'
     * @param term an int value or Java expression to add to this equation
     */
    private void addTerm(char operator, String term) {
        term = term.trim();
        if (operator == '-' && term.equals("2147483648")) {
            addInt(-2147483648);
            return;
        }
        if (isInt(term)) {
            int literal = parseInt(term);
            addInt(operator == '-' ? -1 * literal : literal);
            return;
        }
        if (operator == '-') {
            if (addedTerms.contains(term)) {
                addedTerms.remove(term);
            } else {
                subtractedTerms.add(term);
            }
        } else if (operator == '+') {
            if (subtractedTerms.contains(term)) {
                subtractedTerms.remove(term);
            } else {
                addedTerms.add(term);
            }
        } else {
            assert false;
        }
    }

    private void addInt(int value) {
        intValue += value;
    }

    /**
     * Returns the offset equation that is an int value or null if there isn't one.
     *
     * @param equationSet a set of offset equations
     * @return the offset equation that is an int value or null if there isn't one
     */
    public static OffsetEquation getIntOffsetEquation(Set<OffsetEquation> equationSet) {
        for (OffsetEquation eq : equationSet) {
            if (eq.isInt()) {
                return eq;
            }
        }
        return null;
    }
    /**
     * Creates an offset equation that is only the int value specified.
     *
     * @param value int value of the equation
     * @return an offset equation that is only the int value specified
     */
    public static OffsetEquation createOffsetForInt(int value) {
        OffsetEquation equation = new OffsetEquation();
        equation.intValue = value;
        return equation;
    }

    /**
     * Creates an offset equation from the expressionEquation. The expressionEquation may be several
     * Java expressions added or subtracted from each other. The expressionEquation may also start
     * with + or -. If the expressionEquation is the empty string, then the offset equation returned
     * is zero.
     *
     * @param expressionEquation a Java expression made up of sums and differences
     * @return an offset equation created from expressionEquation
     */
    public static OffsetEquation createOffsetFromJavaExpression(String expressionEquation) {
        expressionEquation = expressionEquation.trim();
        OffsetEquation equation = new OffsetEquation();
        if (expressionEquation.isEmpty()) {
            equation.addTerm('+', "0");
            return equation;
        }

        if (DependentTypesError.isExpressionError(expressionEquation)) {
            equation.error = expressionEquation;
            return equation;
        }
        if (indexOf(expressionEquation, '-', '+', 0) == -1) {
            equation.addTerm('+', expressionEquation);
            return equation;
        }

        int index = 0;
        while (index < expressionEquation.length()) {
            char operator = expressionEquation.charAt(index);
            if (operator == '-' || operator == '+') {
                index++;
            } else {
                operator = '+';
            }

            int endIndex = indexOf(expressionEquation, '-', '+', index);
            String subexpression;
            if (endIndex == -1) {
                endIndex = expressionEquation.length();
                subexpression = expressionEquation.substring(index);
            } else {
                subexpression = expressionEquation.substring(index, endIndex);
            }

            equation.addTerm(operator, subexpression);
            index = endIndex;
        }
        return equation;
    }

    /** A regular expression that matches an integer literal. */
    private static Pattern intPattern = Pattern.compile("[-+]?[0-9]+");

    /**
     * Returns true if the given string is an integer literal
     *
     * @param string a string
     * @return true if the given string is an integer literal
     */
    private static boolean isInt(String string) {
        return intPattern.matcher(string).matches();
    }

    private static int parseInt(String intLiteral) {
        if (intLiteral.isEmpty()) {
            return 0;
        }
        return Integer.valueOf(intLiteral);
    }

    /** Returns the first index of a or b in string, or -1 if neither char is in string. */
    private static int indexOf(String string, char a, char b, int index) {
        int aIndex = string.indexOf(a, index);
        int bIndex = string.indexOf(b, index);
        if (aIndex == -1) {
            return bIndex;
        } else if (bIndex == -1) {
            return aIndex;
        } else {
            return Math.min(aIndex, bIndex);
        }
    }

    /**
     * If node is an int value known at compile time, then the returned equation is just the int
     * value or if op is '-', the return equation is the negation of the int value.
     *
     * <p>Otherwise, null is returned.
     *
     * @param node the Node from which to create an offset equation
     * @param factory an AnnotationTypeFactory
     * @param op '+' or '-'
     * @return an offset equation from value of known or null if the value isn't known
     */
    public static OffsetEquation createOffsetFromNodesValue(
            Node node, ValueAnnotatedTypeFactory factory, char op) {
        assert op == '+' || op == '-';
        if (node.getTree() != null && TreeUtils.isExpressionTree(node.getTree())) {
            Long i = ValueCheckerUtils.getExactValue(node.getTree(), factory);
            if (i != null) {
                if (op == '-') {
                    i = -i;
                }
                OffsetEquation eq = new OffsetEquation();
                eq.addInt(i.intValue());
                return eq;
            }
        }
        return null;
    }

    /**
     * Creates an offset equation from the Node.
     *
     * <p>If node is an addition or subtracted node, then this method is called recursively on the
     * left and right hand nodes and the two equations are added/subtracted to each other depending
     * on the value of op.
     *
     * <p>Otherwise the return equation is created by converting the node to a {@link
     * org.checkerframework.dataflow.expression.JavaExpression} and then added as a term to the
     * returned equation. If op is '-' then it is a subtracted term.
     *
     * @param node the Node from which to create an offset equation
     * @param factory an AnnotationTypeFactory
     * @param op '+' or '-'
     * @return an offset equation from the Node
     */
    public static OffsetEquation createOffsetFromNode(
            Node node, AnnotationProvider factory, char op) {
        assert op == '+' || op == '-';
        OffsetEquation eq = new OffsetEquation();
        createOffsetFromNode(node, factory, eq, op);
        return eq;
    }

    /**
     * Updates an offset equation from a Node.
     *
     * @param node the Node from which to create an offset equation
     * @param factory an AnnotationTypeFactory
     * @param eq an OffsetEquation to update
     * @param op '+' or '-'
     */
    private static void createOffsetFromNode(
            Node node, AnnotationProvider factory, OffsetEquation eq, char op) {
        JavaExpression je = JavaExpression.fromNode(node);
        if (je instanceof Unknown || je == null) {
            if (node instanceof NumericalAdditionNode) {
                createOffsetFromNode(
                        ((NumericalAdditionNode) node).getLeftOperand(), factory, eq, op);
                createOffsetFromNode(
                        ((NumericalAdditionNode) node).getRightOperand(), factory, eq, op);
            } else if (node instanceof NumericalSubtractionNode) {
                createOffsetFromNode(
                        ((NumericalSubtractionNode) node).getLeftOperand(), factory, eq, op);
                char other = op == '+' ? '-' : '+';
                createOffsetFromNode(
                        ((NumericalSubtractionNode) node).getRightOperand(), factory, eq, other);
            } else {
                eq.error = node.toString();
            }
        } else {
            eq.addTerm(op, je.toString());
        }
    }
}
