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
import java.util.ArrayDeque;
import java.util.Arrays;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Utilities for determining tree-based heuristics.
 *
 * <p>For an example, see {@link org.checkerframework.checker.interning.InterningVisitor}.
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
     * matchParents(path, Kind.IF, Kind.METHOD)
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

    /** A convenience class for tree-matching algorithms. Skips parentheses by default. */
    public static class Matcher extends SimpleTreeVisitor<Boolean, Void> {

        @Override
        protected Boolean defaultAction(Tree node, Void p) {
            return false;
        }

        @Override
        public Boolean visitParenthesized(ParenthesizedTree node, Void p) {
            return visit(node.getExpression(), p);
        }

        public boolean match(TreePath path) {
            return visit(path.getLeaf(), null);
        }
    }

    public static class PreceededBy extends Matcher {
        private final Matcher matcher;

        public PreceededBy(Matcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean match(TreePath path) {
            StatementTree stmt = TreeUtils.enclosingOfClass(path, StatementTree.class);
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

    public static class WithIn extends Matcher {
        private final Matcher matcher;

        public WithIn(Matcher matcher) {
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

    public static class WithinTrueBranch extends Matcher {
        private final Matcher matcher;
        /** @param conditionMatcher for the condition */
        public WithinTrueBranch(Matcher conditionMatcher) {
            this.matcher = conditionMatcher;
        }

        @Override
        public boolean match(TreePath path) {
            TreePath prev = path, p = path.getParentPath();
            while (p != null) {
                if (p.getLeaf().getKind() == Tree.Kind.IF) {
                    IfTree ifTree = (IfTree) p.getLeaf();
                    ExpressionTree cond = TreeUtils.skipParens(ifTree.getCondition());
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

    public static class Matchers {
        public static Matcher preceededBy(Matcher matcher) {
            return new PreceededBy(matcher);
        }

        public static Matcher withIn(Matcher matcher) {
            return new WithIn(matcher);
        }

        public static Matcher whenTrue(Matcher conditionMatcher) {
            return new WithinTrueBranch(conditionMatcher);
        }

        public static Matcher ofKind(Tree.Kind kind, Matcher matcher) {
            return new OfKind(kind, matcher);
        }

        public static Matcher or(Matcher... matchers) {
            return new OrMatcher(matchers);
        }
    }
}
