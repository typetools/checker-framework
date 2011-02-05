package checkers.nullness;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

import checkers.igj.quals.ReadOnly;
import checkers.nullness.quals.PolyNull;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.TreeUtils;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;

import static checkers.nullness.NullnessFlow.*;

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

    private BitSet nonnull = new BitSet(0);
    private BitSet nullable = new BitSet(0);
    private boolean isNullPolyNull = false;
    private List<String> nonnullExpressions = new LinkedList<String>();
    private List<String> nullableExpressions = new LinkedList<String>();

    private final List<VariableElement> vars = new LinkedList<VariableElement>();

    /** Variables that should be ignored when setting annoWhenFalse. */
    private final Set<Element> excludes = new HashSet<Element>();

    /**
     * People, don't modify the enclosing flowResults directly! Instead, mark
     * the results here and then let the outside query for it.
     */
    private Map<Tree, AnnotationMirror> treeResults = new IdentityHashMap<Tree, AnnotationMirror>();

    private final NullnessAnnotatedTypeFactory typefactory;

    public NullnessFlowConditions(NullnessAnnotatedTypeFactory tf) {
        this.typefactory = tf;
    }

    /**
     * @return the elements that this analysis has determined to be NonNull when
     *         the condition being analyzed is true
     */
    public @ReadOnly Set<VariableElement> getNonnullElements() {
        return getElements(true);
    }

    /**
     * @return the elements that this analysis has determined to be Nullable
     *         when the condition being analyzed is true
     */
    public @ReadOnly Set<VariableElement> getNullableElements() {
        return getElements(false);
    }

    /**
     * @param isNN
     *            true to get NonNull elements, false to get Nullable elements
     * @return the elements that this analysis has determined to be NonNull (if
     *         isNN is true) or Nullable (if isNN is false) when the condition
     *         being analyzed is true
     */
    private @ReadOnly Set<VariableElement> getElements(boolean isNN) {
        Set<VariableElement> result = new HashSet<VariableElement>();
        for (int i = 0; i < vars.size(); i++)
            if ((isNN && nonnull.get(i) && !nullable.get(i))
                    || (!isNN && nullable.get(i) && !nonnull.get(i)))
                result.add(vars.get(i));
        return Collections.unmodifiableSet(result);
    }

    public @ReadOnly List<String> getNonnullExpressions() {
        return nonnullExpressions;
    }

    public @ReadOnly List<String> getNullableExpressions() {
        return nullableExpressions;
    }

    public boolean isNullPolyNull() {
        return isNullPolyNull;
    }

    public @ReadOnly Set<Element> getExcludes() {
        return excludes;
    }

    public @ReadOnly Map<Tree, AnnotationMirror> getTreeResults() {
        return treeResults;
    }

    @Override
    public Void visitUnary(final UnaryTree node, final Void p) {

        visit(node.getExpression(), p);

        if (node.getKind() != Tree.Kind.LOGICAL_COMPLEMENT)
            return null;

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

        this.nonnullExpressions.addAll(shouldInferNullness(node));

        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {

        Tree expr = node.getExpression();
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

        BitSet nonnullOld = (BitSet) nonnull.clone();
        BitSet nullableOld = (BitSet) nullable.clone();

        visit(left, null);

        final BitSet nonnullSplit = (BitSet) nonnull.clone();
        final BitSet nullableSplit = (BitSet) nullable.clone();

        new TreeScanner<Void, Void>() {

            private void record(Element e, Tree node) {
                int idx = vars.indexOf(e);
                if (idx >= 0) {
                    if (mergeAnd ? nullableSplit.get(idx) : nonnullSplit
                            .get(idx)) {
                        treeResults.put(node, typefactory.NONNULL);
                    }
                }
                if ((mergeAnd ? nullableExpressions : nonnullExpressions)
                        .contains(node.toString())) {
                    treeResults.put(node, typefactory.NONNULL);
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
                if ((mergeAnd ? nullableExpressions : nonnullExpressions)
                        .contains(node.toString())) {
                    treeResults.put(node, typefactory.NONNULL);
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
    public Void visitConditionalExpression(ConditionalExpressionTree node,
            Void p) {

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

        final Tree left = node.getLeftOperand();
        final Tree right = node.getRightOperand();
        final Kind oper = node.getKind();

        if (oper == Tree.Kind.CONDITIONAL_AND)
            splitAndMerge(left, right, false);
        else if (oper == Tree.Kind.CONDITIONAL_OR)
            splitAndMerge(left, right, true);
        else if (oper == Tree.Kind.EQUAL_TO) {
            visit(left, p);
            visit(right, p);

            Element var = null;
            if (hasVar(left) && isNull(right))
                var = var(left);
            else if (isNull(left) && hasVar(right))
                var = var(right);

            mark(var, false);

            if (var != null) {
                if (typefactory.getAnnotatedType(var).getAnnotation(
                        PolyNull.class.getName()) != null)
                    isNullPolyNull = true;
            } else {
                AnnotatedTypeMirror leftType = typefactory
                        .getAnnotatedType(left);
                AnnotatedTypeMirror rightType = typefactory
                        .getAnnotatedType(right);
                if (leftType.hasAnnotation(typefactory.NONNULL)
                        && !rightType.hasAnnotation(typefactory.NONNULL))
                    mark(var(right), true);
                if (rightType.hasAnnotation(typefactory.NONNULL)
                        && !leftType.hasAnnotation(typefactory.NONNULL))
                    mark(var(left), true);
            }

            if (isNull(right) && isPure(left))
                this.nullableExpressions.add(left.toString());
            else if (isNull(left) && isPure(right))
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

            // Handle Pure methods
            if (isNull(right) && isPure(left))
                this.nonnullExpressions.add(left.toString());
            else if (isNull(left) && isPure(right))
                this.nonnullExpressions.add(right.toString());
        }

        return null;
    }

    @Override
    public Void visitIdentifier(final IdentifierTree node, final Void p) {
        final Element e = TreeUtils.elementFromUse(node);
        assert e instanceof VariableElement;
        if (!vars.contains(e))
            vars.add((VariableElement) e);
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitMemberSelect(final MemberSelectTree node, final Void p) {
        final Element e = TreeUtils.elementFromUse(node);
        assert e instanceof VariableElement;
        if (!vars.contains(e))
            vars.add((VariableElement) e);
        if (this.nonnullExpressions.contains(node.toString())) {
            treeResults.put(node, typefactory.NONNULL);
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
        visit(node.getVariable(), p);
        visit(node.getExpression(), p);
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        super.visitMethodInvocation(node, p);

        this.nonnullExpressions.addAll(shouldInferNullness(node));

        return null;
    }
}
