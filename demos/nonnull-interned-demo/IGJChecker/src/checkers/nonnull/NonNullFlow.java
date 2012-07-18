package checkers.nonnull;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

import checkers.flow.Flow;
import checkers.types.AnnotatedTypeFactory;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;

/**
 * Implements NonNull-specific customizations of the flow-sensitive type
 * qualifier inference provided by {@link Flow}. In particular, if a
 * conditional null-check is performed, the checked value is treated as non-null
 * or possibly-null as appropriate in the subsequent branches. For instance, for
 * the check
 * 
 * <pre>
 * if (x != null) {
 *     foo(x);
 * } else {
 *     bar(x);
 * }
 * </pre>
 * 
 * {@code x} is treated as non-null in the argument for {@code foo} and as
 * possibly-null in the argument for {@code bar}.
 * 
 * @see Flow
 */
class NonNullFlow extends Flow {
    
    /**
     * Creates a NonNull-specific flow-sensitive inference.
     * 
     * @param env the processing environment
     * @param root the compilation unit to scan
     * @param annotation the annotation to use
     * @param factory the type factory to use
     */
    public NonNullFlow(ProcessingEnvironment env, CompilationUnitTree root,
            AnnotationMirror annotation, AnnotatedTypeFactory factory) {
        super(env, root, annotation, factory);
    }

    @Override
    protected void scanCond(Tree tree) {
        super.scanCond(tree);
        
        Conditions conds = new Conditions(factory);
        conds.visit(tree, null);

        for (VariableElement elt : conds.getNonnullElements()) {
            int idx = vars.indexOf(elt);
            if (idx >= 0) {
                annoWhenTrue.set(idx);
                annoWhenFalse.clear(idx);
            }
        }
        
        for (VariableElement elt : conds.getNullableElements()) {
            int idx = vars.indexOf(elt);
            if (idx >= 0) {
                annoWhenTrue.clear(idx);
                annoWhenFalse.set(idx);
            }
        }
    }
    
    /**
     * A utility class used by a NonNull-specific flow-sensitive qualifier
     * inference to determine nullness in conditionally-executed scopes.
     * 
     * <p>
     * 
     * This class is implemented as a visitor; it should be only be initially
     * invoked on the conditions of e.g. if statements.
     */
    static class Conditions extends SimpleTreeVisitor<Void, Void> {
         
        private BitSet nonnull = new BitSet();
        private BitSet nullable = new BitSet();
        
        private final AnnotatedTypeFactory factory;

        private final List<VariableElement> vars = new LinkedList<VariableElement>();

        /**
         * Instantiates this class using the given type factory.
         * 
         * @param factory the type factory to use
         */
        public Conditions(final AnnotatedTypeFactory factory) {
            this.factory = factory;
        }

        /**
         * @return the elements that this analysis has determined to be NonNull when
         *         the condition being analyzed is true
         */
        public Set<VariableElement> getNonnullElements() {
            return getElements(true);
        }
        
        /**
         * @return the elements that this analysis has determined to be Nullable
         *         when the condition being analyzed is true
         */
        public Set<VariableElement> getNullableElements() {
            return getElements(false);
        }
        
        /**
         * @param isNN true to get NonNull elements, false to get Nullable
         *             elements
         * @return the elements that this analysis has determined to be NonNull (if
         *         isNN is true) or Nullable (if isNN is false) when the condition
         *         being analyzed is true
         */
        private Set<VariableElement> getElements(boolean isNN) {
            Set<VariableElement> result = new HashSet<VariableElement>();
            for (int i = 0; i < vars.size(); i++)
                if ((isNN && nonnull.get(i) && !nullable.get(i))
                     || (!isNN && nullable.get(i) && !nonnull.get(i)))
                    result.add(vars.get(i));
            return Collections.unmodifiableSet(result);
        }
        
        /**
         * Convenience method: determine if the given tree is the null literal.
         * 
         * @param tree the tree to check
         * @return true if the tree is the null literal, false otherwise
         */
        private final boolean isNull(final Tree tree) {
            return tree != null && tree.getKind() == Tree.Kind.NULL_LITERAL;
        }
        
        /**
         * Convenience method: determine if the given tree might have a variable
         * element.
         * 
         * @param tree the tree to check
         * @return true if the tree may have a variable element, false otherwise
         */
        private final boolean hasVar(final Tree tree) {
            switch (tree.getKind()) {
            case IDENTIFIER:
            case MEMBER_SELECT:
                return true;
            default:
                return false;
            }
        }
        
        /**
         * Convenience method: get the variable's element for the given tree.
         * 
         * @param tree the tree to check
         * @return the element for the variable in the tree
         * @throws UnsupportedOperationException if the tree has no variable
         */
        private final Element var(final Tree tree) {
            switch (tree.getKind()) {
            case IDENTIFIER:
                return factory.elementFromUse((IdentifierTree) tree);
            case MEMBER_SELECT:
                return factory.elementFromUse((MemberSelectTree) tree);
            default:
                throw new UnsupportedOperationException("var from "
                        + tree.getKind());
            }
        }
        
        @Override
        public Void visitUnary(final UnaryTree node, final Void p) {
        
            visit(node.getExpression(), p);
            
            if (node.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
                nonnull.flip(0, nonnull.size());
                nullable.flip(0, nullable.size());
            }
            
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
            }
            
            return super.visitInstanceOf(node, p);
        }

        private void splitAndMerge(Tree left, Tree right, boolean mergeAnd) {
            
            BitSet nonnullOld = (BitSet)nonnull.clone();
            BitSet nullableOld = (BitSet)nullable.clone();
            
            visit(left, null);
            
            BitSet nonnullSplit = (BitSet)nonnull.clone();
            BitSet nullableSplit = (BitSet)nullable.clone();
            
            nonnull = nonnullOld;
            nullable = nullableOld;
            
            visit(right, null);
            
            if (mergeAnd) {
                nonnullSplit.and(nonnull);
                nullableSplit.and(nullable);
            } else {
                nonnullSplit.or(nonnull);
                nullableSplit.or(nullable);
            }
            
            nonnull = nonnullOld;
            nullable = nullableOld;
            
            nonnull.or(nonnullSplit);
            nullable.or(nullableSplit);
            
        }
        
        @Override
        public Void visitConditionalExpression(ConditionalExpressionTree node,
                Void p) {

            // (a ? b : c) --> (a && b) || c
            
            BitSet nonnullOld = (BitSet)nonnull.clone();
            BitSet nullableOld = (BitSet)nullable.clone();
            
            splitAndMerge(node.getCondition(), node.getTrueExpression(), false);

            BitSet nonnullSplit = (BitSet)nonnull.clone();
            BitSet nullableSplit = (BitSet)nullable.clone();
            
            visit(node.getFalseExpression(), p);
            
            nonnullSplit.and(nonnull);
            nullableSplit.and(nullable);
            
            nonnull = nonnullOld;
            nullable = nullableOld;
            
            nonnull.or(nonnullSplit);
            nullable.or(nullableSplit);

            return super.visitConditionalExpression(node, p);
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

                if (var != null) {
                    int idx = vars.indexOf(var);
                    nullable.set(idx);
                    nonnull.clear(idx);
                }

            } else if (oper == Tree.Kind.NOT_EQUAL_TO) {
                visit(left, p);
                visit(right, p);

                Element var = null;
                if (hasVar(left) && isNull(right))
                    var = var(left);
                else if (isNull(left) && hasVar(right))
                    var = var(right);

                if (var != null) {
                    int idx = vars.indexOf(var);
                    nonnull.set(idx);
                    nullable.clear(idx);
                }

            }
            
            return null;
        }

        @Override
        public Void visitIdentifier(final IdentifierTree node, final Void p) {
            final Element e = factory.elementFromUse(node);
            assert e instanceof VariableElement;
            if (!vars.contains(e))
                vars.add((VariableElement) e);
            return super.visitIdentifier(node, p);
        }

        @Override
        public Void visitMemberSelect(final MemberSelectTree node, final Void p) {
            final Element e = factory.elementFromUse(node);
            assert e instanceof VariableElement;
            if (!vars.contains(e))
                vars.add((VariableElement) e);
            return super.visitMemberSelect(node, p);
        }

        @Override
        public Void visitParenthesized(final ParenthesizedTree node, final Void p) {
            // Skip parens.
            return visit(node.getExpression(), p);
        }
    }   
}