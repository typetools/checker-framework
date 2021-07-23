package org.checkerframework.framework.util;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;

import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Utilities for determining tree-based heuristics.
 *
 * <p>For an example, see {@code org.checkerframework.checker.interning.InterningVisitor}.
 */
public class Heuristics {

    /**
     * Determines whether a tree has a particular set of direct parents, ignoring blocks and
     * parentheses.
     *
     * <p>For example, to test whether an expression (specified by {@code path}) is immediately
     * contained by an if statement which is immediately contained in a method, one would invoke:
     *
     * <pre>
     * matchParents(path, Kind.IF, Tree.Kind.METHOD)
     * </pre>
     *
     * @param path the path to match
     * @param kinds the tree kinds to match against, in ascending order starting from the desired
     *     kind of the parent
     * @return true if the tree path matches the desired kinds, skipping blocks and parentheses, for
     *     as many kinds as specified
     */
    public static boolean matchParents(TreePath path, Tree.Kind... kinds) {

        TreePath parentPath = path.getParentPath();
        boolean result = true;

        ArrayDeque<Tree.Kind> queue = new ArrayDeque<>(Arrays.asList(kinds));

        Tree tree;
        while ((tree = parentPath.getLeaf()) != null) {

            if (queue.isEmpty()) {
                break;
            }

            if (tree.getKind() == Tree.Kind.BLOCK || tree.getKind() == Tree.Kind.PARENTHESIZED) {
                parentPath = parentPath.getParentPath();
                continue;
            }

            result &= queue.removeFirst() == parentPath.getLeaf().getKind();
            parentPath = parentPath.getParentPath();
        }

        return result;
    }

    /** A base class for tree-matching algorithms. Skips parentheses by default. */
    public static class Matcher extends SimpleTreeVisitor<Boolean, Void> {

        @Override
        protected Boolean defaultAction(Tree node, Void p) {
            return false;
        }

        @Override
        public Boolean visitParenthesized(ParenthesizedTree node, Void p) {
            return visit(node.getExpression(), p);
        }

        /**
         * Returns true if the given path matches this Matcher.
         *
         * @param path the path to test
         * @return true if the given path matches this Matcher
         */
        public boolean match(TreePath path) {
            return visit(path.getLeaf(), null);
        }
    }

    public static class PreceededBy extends Matcher {
        private final Matcher matcher;

        public PreceededBy(Matcher matcher) {
            this.matcher = matcher;
        }

        @SuppressWarnings("interning:not.interned")
        @Override
        public boolean match(TreePath path) {
            StatementTree stmt = TreePathUtil.enclosingOfClass(path, StatementTree.class);
            if (stmt == null) {
                return false;
            }
            TreePath p = path;
            while (p.getLeaf() != stmt) {
                p = p.getParentPath();
            }
            assert p.getLeaf() == stmt;

            while (p != null && p.getLeaf() instanceof StatementTree) {
                if (p.getParentPath().getLeaf() instanceof BlockTree) {
                    BlockTree block = (BlockTree) p.getParentPath().getLeaf();
                    for (StatementTree st : block.getStatements()) {
                        if (st == p.getLeaf()) {
                            break;
                        }

                        if (matcher.match(new TreePath(p, st))) {
                            return true;
                        }
                    }
                }
                p = p.getParentPath();
            }

            return false;
        }
    }

    /**
     * {@code match()} returns true if called on a path, any element of which matches the given
     * matcher (supplied at object initialization). That matcher is usually one that matches only
     * the leaf of a path, ignoring all other parts of it.
     */
    public static class Within extends Matcher {
        /**
         * The matcher that {@code Within.match} will try, on every parent of the path it is given.
         */
        private final Matcher matcher;

        /**
         * Create a new Within matcher.
         *
         * @param matcher the matcher that {@code Within.match} will try, on every parent of the
         *     path it is given
         */
        public Within(Matcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean match(TreePath path) {
            TreePath p = path;
            while (p != null) {
                if (matcher.match(p)) {
                    return true;
                }
                p = p.getParentPath();
            }

            return false;
        }
    }

    /**
     * {@code match()} returns true if called on a path whose leaf is within the "then" clause of an
     * if whose conditon matches the matcher (supplied at object initialization). Also returns true
     * if the leaf is within the "else" of a negated condition that matches the supplied matcher.
     */
    public static class WithinTrueBranch extends Matcher {
        private final Matcher matcher;
        /** @param conditionMatcher for the condition */
        public WithinTrueBranch(Matcher conditionMatcher) {
            this.matcher = conditionMatcher;
        }

        @SuppressWarnings("interning:not.interned")
        @Override
        public boolean match(TreePath path) {
            TreePath prev = path, p = path.getParentPath();
            while (p != null) {
                if (p.getLeaf().getKind() == Tree.Kind.IF) {
                    IfTree ifTree = (IfTree) p.getLeaf();
                    ExpressionTree cond = TreeUtils.withoutParens(ifTree.getCondition());
                    if (ifTree.getThenStatement() == prev.getLeaf()
                            && matcher.match(new TreePath(p, cond))) {
                        return true;
                    }
                    if (cond.getKind() == Tree.Kind.LOGICAL_COMPLEMENT
                            && matcher.match(new TreePath(p, ((UnaryTree) cond).getExpression()))) {
                        return true;
                    }
                }
                prev = p;
                p = p.getParentPath();
            }

            return false;
        }
    }

    /**
     * {@code match()} returns true if called on a path whose leaf has the given kind (supplied at
     * object initialization).
     */
    public static class OfKind extends Matcher {
        private final Tree.Kind kind;
        private final Matcher matcher;

        public OfKind(Tree.Kind kind, Matcher matcher) {
            this.kind = kind;
            this.matcher = matcher;
        }

        @Override
        public boolean match(TreePath path) {
            if (path.getLeaf().getKind() == kind) {
                return matcher.match(path);
            }
            return false;
        }
    }

    /** {@code match()} returns true if any of the given matchers returns true. */
    public static class OrMatcher extends Matcher {
        private final Matcher[] matchers;

        public OrMatcher(Matcher... matchers) {
            this.matchers = matchers;
        }

        @Override
        public boolean match(TreePath path) {
            for (Matcher matcher : matchers) {
                if (matcher.match(path)) {
                    return true;
                }
            }
            return false;
        }
    }
}
