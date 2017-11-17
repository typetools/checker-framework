package org.checkerframework.dataflow.util;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import javax.lang.model.element.Element;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.Pure.Kind;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

/**
 * A visitor that determines the purity (as defined by {@link
 * org.checkerframework.dataflow.qual.SideEffectFree}, {@link
 * org.checkerframework.dataflow.qual.Deterministic}, and {@link
 * org.checkerframework.dataflow.qual.Pure}) of a statement or expression. The entry point is method
 * {@link #checkPurity}.
 *
 * @see SideEffectFree
 * @see Deterministic
 * @see Pure
 * @author Stefan Heule
 */
public class PurityChecker {

    /**
     * Compute whether the given statement is side-effect-free, deterministic, or both. Returns a
     * result that can be queried.
     */
    public static PurityResult checkPurity(
            TreePath statement, AnnotationProvider annoProvider, boolean assumeSideEffectFree) {
        PurityCheckerHelper helper = new PurityCheckerHelper(annoProvider, assumeSideEffectFree);
        helper.scan(statement, null);
        return helper.presult;
    }

    /**
     * Result of the {@link PurityChecker}. Can be queried regarding whether a given tree was
     * side-effect-free, deterministic, or both; also gives reasons if the answer is "no".
     */
    public static class PurityResult {

        protected final List<Pair<Tree, String>> notSEFreeReasons;
        protected final List<Pair<Tree, String>> notDetReasons;
        protected final List<Pair<Tree, String>> notBothReasons;
        /**
         * Contains all the varieties of purity that the expression has. Starts out with all
         * varieties, and elements are removed from it as violations are found.
         */
        protected EnumSet<Pure.Kind> types;

        public PurityResult() {
            notSEFreeReasons = new ArrayList<>();
            notDetReasons = new ArrayList<>();
            notBothReasons = new ArrayList<>();
            types = EnumSet.allOf(Pure.Kind.class);
        }

        public EnumSet<Pure.Kind> getTypes() {
            return types;
        }

        /**
         * Is the method pure w.r.t. a given set of types?
         *
         * @param kinds the varieties of purity to check
         * @return true if the method is pure with respect to all the given kinds
         */
        public boolean isPure(Collection<Kind> kinds) {
            return types.containsAll(kinds);
        }

        /** Get the {@code reason}s why the method is not side-effect-free. */
        public List<Pair<Tree, String>> getNotSEFreeReasons() {
            return notSEFreeReasons;
        }

        /** Add {@code reason} as a reason why the method is not side-effect free. */
        public void addNotSEFreeReason(Tree t, String msgId) {
            notSEFreeReasons.add(Pair.of(t, msgId));
            types.remove(Kind.SIDE_EFFECT_FREE);
        }

        /** Get the {@code reason}s why the method is not deterministic. */
        public List<Pair<Tree, String>> getNotDetReasons() {
            return notDetReasons;
        }

        /** Add {@code reason} as a reason why the method is not deterministic. */
        public void addNotDetReason(Tree t, String msgId) {
            notDetReasons.add(Pair.of(t, msgId));
            types.remove(Kind.DETERMINISTIC);
        }

        /**
         * Get the {@code reason}s why the method is not both side-effect-free and deterministic.
         */
        public List<Pair<Tree, String>> getNotBothReasons() {
            return notBothReasons;
        }

        /**
         * Add {@code reason} as a reason why the method is not both side-effect free and
         * deterministic.
         */
        public void addNotBothReason(Tree t, String msgId) {
            notBothReasons.add(Pair.of(t, msgId));
            types.remove(Kind.DETERMINISTIC);
            types.remove(Kind.SIDE_EFFECT_FREE);
        }
    }

    // TODO: It would be possible to improve efficiency by visiting fewer nodes.  This would require
    // overriding more visit* methods.  I'm not sure whether such an optimization would be worth it.

    /** Helper class to keep {@link PurityChecker}'s interface clean. */
    protected static class PurityCheckerHelper extends TreePathScanner<Void, Void> {

        PurityResult presult = new PurityResult();

        protected final AnnotationProvider annoProvider;

        /**
         * True if all methods should be assumed to be @SideEffectFree, for the purposes of
         * org.checkerframework.dataflow analysis.
         */
        private final boolean assumeSideEffectFree;

        public PurityCheckerHelper(AnnotationProvider annoProvider, boolean assumeSideEffectFree) {
            this.annoProvider = annoProvider;
            this.assumeSideEffectFree = assumeSideEffectFree;
        }

        // I don't understand why this definition does not type-checck.
        // /** Scan a list of nodes. */
        // public void scan(Iterable<? extends Tree> nodes, Void ignore) {
        //     if (nodes != null) {
        //         for (Tree node : nodes) {
        //             scan(node, ignore);
        //         }
        //     }
        // }

        @Override
        public Void visitCatch(CatchTree node, Void ignore) {
            presult.addNotDetReason(node, "catch");
            return super.visitCatch(node, ignore);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void ignore) {
            Element elt = TreeUtils.elementFromUse(node);
            String reason = "call";
            if (!PurityUtils.hasPurityAnnotation(annoProvider, elt)) {
                presult.addNotBothReason(node, reason);
            } else {
                boolean det = PurityUtils.isDeterministic(annoProvider, elt);
                boolean seFree =
                        (assumeSideEffectFree || PurityUtils.isSideEffectFree(annoProvider, elt));
                if (!det && !seFree) {
                    presult.addNotBothReason(node, reason);
                } else if (!det) {
                    presult.addNotDetReason(node, reason);
                } else if (!seFree) {
                    presult.addNotSEFreeReason(node, reason);
                }
            }
            return super.visitMethodInvocation(node, ignore);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void ignore) {
            Element methodElement = InternalUtils.symbol(node);
            boolean sideEffectFree =
                    (assumeSideEffectFree
                            || PurityUtils.isSideEffectFree(annoProvider, methodElement));
            if (sideEffectFree) {
                presult.addNotDetReason(node, "object.creation");
            } else {
                presult.addNotBothReason(node, "object.creation");
            }
            return super.visitNewClass(node, ignore);
        }

        @Override
        public Void visitAssignment(AssignmentTree node, Void ignore) {
            ExpressionTree variable = node.getVariable();
            assignmentCheck(variable);
            return super.visitAssignment(node, ignore);
        }

        protected void assignmentCheck(ExpressionTree variable) {
            if (TreeUtils.isFieldAccess(variable)) {
                // rhs is a field access
                presult.addNotBothReason(variable, "assign.field");
            } else if (variable instanceof ArrayAccessTree) {
                // rhs is array access
                presult.addNotBothReason(variable, "assign.array");
            } else {
                // rhs is a local variable
                assert isLocalVariable(variable);
            }
        }

        protected boolean isLocalVariable(ExpressionTree variable) {
            return variable instanceof IdentifierTree && !TreeUtils.isFieldAccess(variable);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, Void ignore) {
            ExpressionTree variable = node.getVariable();
            assignmentCheck(variable);
            return super.visitCompoundAssignment(node, ignore);
        }
    }
}
