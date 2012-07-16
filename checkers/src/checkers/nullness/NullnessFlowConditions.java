package checkers.nullness;

import static checkers.nullness.NullnessFlow.hasVar;
import static checkers.nullness.NullnessFlow.isNull;
import static checkers.nullness.NullnessFlow.var;

import java.io.PrintStream;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

import checkers.flow.AbstractFlow;
import checkers.igj.quals.ReadOnly;
import checkers.nullness.quals.PolyNull;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.ElementUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;

/**
 * A utility class used by a NonNull-specific flow-sensitive qualifier inference
 * to determine nullness in conditionally-executed scopes.
 *
 * <p>
 *
 * This class is implemented as a visitor; it should only be initially invoked
 * on the conditions of e.g. if statements.
 *
 * TODO: extract any common behavior to a FlowConditions superclass.
 * TODO: can we benefit from using FlowState?
 */
public class NullnessFlowConditions extends SimpleTreeVisitor<Void, Void> {

    private final List<VariableElement> vars = new LinkedList<VariableElement>();

    private BitSet nonnull = new BitSet(0);
    private BitSet nullable = new BitSet(0);

    private boolean isNullPolyNull = false;

    private final List<String> nonnullExpressions = new LinkedList<String>();
    private final List<String> nullableExpressions = new LinkedList<String>();

    private final List<VariableElement> nonnullElements = new LinkedList<VariableElement>();
    private final List<VariableElement> nullableElements = new LinkedList<VariableElement>();

    /** Variables that should be ignored when setting annoWhenFalse. */
    private final Set<Element> excludes = new HashSet<Element>();

    private final Map<Tree, Set<AnnotationMirror>> treeResults = new IdentityHashMap<Tree, Set<AnnotationMirror>>();

    protected final NullnessAnnotatedTypeFactory typefactory;
    protected final NullnessFlow nullnessFlow;

    protected final PrintStream debug;

    public NullnessFlowConditions(NullnessAnnotatedTypeFactory tf, NullnessFlow nf, PrintStream debug) {
        this.typefactory = tf;
        this.nullnessFlow = nf;
        this.debug = debug;
    }

    /**
     * @return the elements that this analysis has determined to be NonNull when
     *         the condition being analyzed is true
     */
    public /*@ReadOnly*/ Set<VariableElement> getNonnullElements() {
        return getElements(true);
    }

    /**
     * @return the elements that this analysis has determined to be Nullable
     *         when the condition being analyzed is true
     */
    public /*@ReadOnly*/ Set<VariableElement> getNullableElements() {
        return getElements(false);
    }

    /**
     * @param isNN
     *            true to get NonNull elements, false to get Nullable elements
     * @return the elements that this analysis has determined to be NonNull (if
     *         isNN is true) or Nullable (if isNN is false) when the condition
     *         being analyzed is true
     */
    private /*@ReadOnly*/ Set<VariableElement> getElements(boolean isNN) {
        Set<VariableElement> result = new HashSet<VariableElement>();
        for (int i = 0; i < vars.size(); i++)
            if ((isNN && nonnull.get(i) && !nullable.get(i))
                    || (!isNN && nullable.get(i) && !nonnull.get(i)))
                result.add(vars.get(i));
        return Collections.unmodifiableSet(result);
    }

    public /*@ReadOnly*/ List<String> getNonnullExpressions() {
        return nonnullExpressions;
    }

    public /*@ReadOnly*/ List<String> getNullableExpressions() {
        return nullableExpressions;
    }

    public /*@ReadOnly*/ List<VariableElement> getExplicitNonnullElements() {
        return nonnullElements;
    }

    public /*@ReadOnly*/ List<VariableElement> getExplicitNullableElements() {
        return nullableElements;
    }

    public boolean isNullPolyNull() {
        return isNullPolyNull;
    }

    public /*@ReadOnly*/ Set<Element> getExcludes() {
        return excludes;
    }

    public /*@ReadOnly*/ Map<Tree, Set<AnnotationMirror>> getTreeResults() {
        return treeResults;
    }


    @Override
    public Void visitUnary(final UnaryTree node, final Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitUnary: " + node);
        }

        visit(node.getExpression(), p);

        if (node.getKind() != Tree.Kind.LOGICAL_COMPLEMENT)
            return null;

        // TODO
        // only invert if cardinal is one
        if (nonnull.cardinality() + nullable.cardinality() == 1) {
            nonnull.xor(nullable);
            nullable.xor(nonnull);
            nonnull.xor(nullable);
        } else {
            // give up
            nonnull.clear();
            nullable.clear();
        }

        isNullPolyNull = false;

        // the false branch of a logic complement of instance is nonnull!
        if (TreeUtils.skipParens(node.getExpression()).getKind() == Tree.Kind.INSTANCE_OF) {
            ExpressionTree expr = ((InstanceOfTree) TreeUtils.skipParens(node
                    .getExpression())).getExpression();
            this.excludes.remove(var(expr));
        }

        this.nonnullExpressions.addAll(nullnessFlow.shouldInferNullness(node));

        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitInstanceOf: " + node);
        }

        ExpressionTree expr = node.getExpression();
        visit(expr, p);

        if (hasVar(expr)) {
            int idx = vars.indexOf(var(expr));
            nonnull.set(idx);
            nullable.clear(idx);
            excludes.add(var(expr));
        }

        return super.visitInstanceOf(node, p);
    }

    /**
     * Splits the gen-kill sets at a branch, descends into each branch (the left
     * and right children), and merges the gen-kill when the branches rejoin in
     * one of two ways ("and" or "or").
     *
     * @param left
     *            the left branch child
     * @param right
     *            the right branch child
     * @param mergeAnd
     *            true if merging should be done using boolean "and", false if
     *            it should be done using boolean "or"
     */
    private void splitAndMerge(Tree left, Tree right, final boolean mergeAnd) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::splitAndMerge; left: " + left + " right: " + right + " mergeAnd: " + mergeAnd);
        }

        BitSet nonnullOld = (BitSet) nonnull.clone();
        BitSet nullableOld = (BitSet) nullable.clone();

        visit(left, null);

        final BitSet nonnullSplit = (BitSet) nonnull.clone();
        final BitSet nullableSplit = (BitSet) nullable.clone();

        new TreeScanner<Void, Void>() {

            private void record(Element e, Tree node) {
                int idx = vars.indexOf(e);
                if (idx >= 0) {
                    if (mergeAnd ? nullableSplit.get(idx) : nonnullSplit.get(idx)) {
                        AbstractFlow.addFlowResult(treeResults, node, typefactory.NONNULL);
                    }
                }
                if ((mergeAnd ? nullableExpressions : nonnullExpressions).contains(node.toString())) {
                    AbstractFlow.addFlowResult(treeResults, node, typefactory.NONNULL);
                }
                if ((mergeAnd ? nullableElements : nonnullElements).contains(e)) {
                    AbstractFlow.addFlowResult(treeResults, node, typefactory.NONNULL);
                }
            }

            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                Element e = TreeUtils.elementFromUse(node);
                record(e, node);
                return super.visitIdentifier(node, p);
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree node, Void p) {
                Element e = TreeUtils.elementFromUse(node);
                record(e, node);
                return super.visitMemberSelect(node, p);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                if ((mergeAnd ? nullableExpressions : nonnullExpressions).contains(node.toString())) {
                    AbstractFlow.addFlowResult(treeResults, node, typefactory.NONNULL);
                }
                return super.visitMethodInvocation(node, p);
            }
        }.scan(right, null);

        nonnull = (BitSet) nonnullOld.clone();
        nullable = (BitSet) nullableOld.clone();

        visit(right, null);

        if (mergeAnd) {
            nonnullSplit.and(nonnull);
            nullableSplit.or(nullable);
        } else {
            nonnullSplit.or(nonnull);
            nullableSplit.or(nullable);
            // nnExprs.clear();
        }

        nonnull = nonnullOld;
        nullable = nullableOld;

        nonnull.or(nonnullSplit);
        nullable.or(nullableSplit);

        // when flow leads to a contradiction: a var is both nullable
        // and nonnull, treat it as a nonnull
        nullable.andNot(nonnull);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitConditionalExpression: " + node);
        }

        // (a ? b : c) --> (a && b) || c

        BitSet nonnullOld = (BitSet) nonnull.clone();
        BitSet nullableOld = (BitSet) nullable.clone();

        splitAndMerge(node.getCondition(), node.getTrueExpression(), false);

        BitSet nonnullSplit = (BitSet) nonnull.clone();
        BitSet nullableSplit = (BitSet) nullable.clone();

        visit(node.getFalseExpression(), p);

        nonnullSplit.and(nonnull);
        nullableSplit.and(nullable);

        nonnull = nonnullOld;
        nullable = nullableOld;

        nonnull.or(nonnullSplit);
        nullable.or(nullableSplit);

        return super.visitConditionalExpression(node, p);
    }

    private void mark(Element var, boolean isNonnull) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::mark; var: " + var + " isNonnull: " + isNonnull);
        }

        if (var == null)
            return;
        int idx = vars.indexOf(var);
        if (isNonnull) {
            nonnull.set(idx);
            nullable.clear(idx);
        } else {
            nullable.set(idx);
            nonnull.clear(idx);
        }
    }

    @Override
    public Void visitBinary(final BinaryTree node, final Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitBinary: " + node);
        }

        final ExpressionTree left = node.getLeftOperand();
        final ExpressionTree right = node.getRightOperand();
        final Kind oper = node.getKind();

        if (oper == Tree.Kind.CONDITIONAL_AND) {
            splitAndMerge(left, right, false);
        } else if (oper == Tree.Kind.CONDITIONAL_OR) {
            splitAndMerge(left, right, true);
        } else if (oper == Tree.Kind.EQUAL_TO) {
            visit(left, p);
            visit(right, p);

            Element var = null;
            if (hasVar(left) && isNull(right))
                var = var(left);
            else if (isNull(left) && hasVar(right))
                var = var(right);

            mark(var, false);

            if (var != null) {
                if (typefactory.getAnnotatedType(var).hasAnnotation(typefactory.POLYNULL) ||
                        typefactory.getAnnotatedType(var).hasAnnotation(typefactory.POLYALL))
                    isNullPolyNull = true;
            } else {
                AnnotatedTypeMirror leftType = typefactory.getAnnotatedType(left);
                AnnotatedTypeMirror rightType = typefactory.getAnnotatedType(right);

                if (leftType.hasAnnotation(typefactory.NONNULL)
                        && !rightType.hasAnnotation(typefactory.NONNULL))
                    mark(var(right), true);
                if (rightType.hasAnnotation(typefactory.NONNULL)
                        && !leftType.hasAnnotation(typefactory.NONNULL))
                    mark(var(left), true);
                if ((leftType.hasAnnotation(typefactory.POLYNULL) ||
                        leftType.hasAnnotation(typefactory.POLYALL)) &&
                        isNull(right)) {
                    isNullPolyNull = true;
                } else if ((rightType.hasAnnotation(typefactory.POLYNULL) ||
                        rightType.hasAnnotation(typefactory.POLYALL)) &&
                        isNull(left)) {
                    isNullPolyNull = true;
                }
            }

            if (isNull(right) && nullnessFlow.isPure(left))
                this.nullableExpressions.add(left.toString());
            else if (isNull(left) && nullnessFlow.isPure(right))
                this.nullableExpressions.add(right.toString());

        } else if (oper == Tree.Kind.NOT_EQUAL_TO) {
            visit(left, p);
            visit(right, p);

            Element var = null;
            if (hasVar(left) && isNull(right))
                var = var(left);
            else if (isNull(left) && hasVar(right))
                var = var(right);

            mark(var, true);

            // TODO: why is there no handling of Poly and NONNULL here??

            // Handle Pure methods
            if (isNull(right) && nullnessFlow.isPure(left))
                this.nonnullExpressions.add(left.toString());
            else if (isNull(left) && nullnessFlow.isPure(right))
                this.nonnullExpressions.add(right.toString());

            if (isNull(right) && isUseOfStaticVariableElement(left))
                this.nonnullElements.add((VariableElement)TreeUtils.elementFromUse(left));
            else if (isNull(left) && isUseOfStaticVariableElement(right))
                this.nonnullElements.add((VariableElement)TreeUtils.elementFromUse(right));

        } /* else {
            System.out.println("Also looking at: " + left + " " + oper + " " + right);
            visit(left, p);
            visit(right, p);
        } */

        return null;
    }

    private boolean isUseOfStaticVariableElement(ExpressionTree tree) {
        if (!TreeUtils.isUseOfElement(tree)) {
            return false;
        }
        Element elem = TreeUtils.elementFromUse(tree);
        return elem instanceof VariableElement && ElementUtils.isStatic(elem);
    }

    @Override
    public Void visitIdentifier(final IdentifierTree node, final Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitIdentifier: " + node);
        }

        final Element e = TreeUtils.elementFromUse(node);
        assert e instanceof VariableElement;
        if (!vars.contains(e))
            vars.add((VariableElement) e);
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitMemberSelect(final MemberSelectTree node, final Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitMemberSelect: " + node);
        }

        final Element e = TreeUtils.elementFromUse(node);
        assert e instanceof VariableElement;
        if (!vars.contains(e))
            vars.add((VariableElement) e);
        if (this.nonnullExpressions.contains(node.toString()) ||
            this.nonnullElements.contains(e)) {
            AbstractFlow.addFlowResult(treeResults, node, typefactory.NONNULL);
        }
        return super.visitMemberSelect(node, p);
    }

    @Override
    public Void visitParenthesized(final ParenthesizedTree node, final Void p) {
        // Skip parens.
        return visit(node.getExpression(), p);
    }

    @Override
    public Void visitAssignment(final AssignmentTree node, final Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitAssignment: " + node);
        }

        visit(node.getVariable(), p);
        visit(node.getExpression(), p);
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlowConditions::visitMethodInvocation: " + node);
        }

        super.visitMethodInvocation(node, p);

        this.nonnullExpressions.addAll(nullnessFlow.shouldInferNullness(node));

        return null;
    }
}
