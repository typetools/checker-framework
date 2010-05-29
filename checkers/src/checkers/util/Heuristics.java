package checkers.util;

import java.util.*;

import checkers.interning.InterningVisitor;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.*;

/**
 * Utilities for determining tree-based heuristics.
 *
 * @see InterningVisitor for an example
 */
public class Heuristics {

    /**
     * Determines whether a tree has a particular set of direct parents,
     * ignoring blocks and parentheses.
     *
     * <p>
     *
     * For example, to test whether an expression (specified by {@code path})
     * is immediately contained by an if statement which is immediately
     * contained in a method, one would invoke:
     *
     * <pre>
     * matchParents(path, Kind.IF, Kind.METHOD)
     * </pre>
     *
     * @param path the path to match
     * @param kinds the tree kinds to match against, in ascending order starting
     *        from the desired kind of the parent
     * @return true if the tree path matches the desired kinds, skipping blocks
     *         and parentheses, for as many kinds as specified
     */
    public static boolean matchParents(TreePath path, Tree.Kind... kinds) {

        TreePath parentPath = path.getParentPath();
        boolean result = true;

        LinkedList<Tree.Kind> queue =
            new LinkedList<Tree.Kind>(Arrays.asList(kinds));

        Tree tree;
        while ((tree = parentPath.getLeaf()) != null) {

            if (queue.isEmpty()) break;

            if (tree.getKind() == Tree.Kind.BLOCK
                    || tree.getKind() == Tree.Kind.PARENTHESIZED) {
                parentPath = parentPath.getParentPath();
                continue;
            }

            result &= queue.poll() == parentPath.getLeaf().getKind();
            parentPath = parentPath.getParentPath();
        }

        return result;
    }

    /**
     * A convenience class for tree-matching algorithms. Skips parentheses by
     * default.
     *
     * @see Heuristics#applyAt(TreePath, Tree.Kind, Matcher)
     */
    public static class Matcher extends SimpleTreeVisitor<Boolean, Void> {

        @Override
        protected Boolean defaultAction(Tree node, Void p) {
            return false;
        }

        @Override
        public Boolean visitParenthesized(ParenthesizedTree node, Void p) {
            return visit(node.getExpression(), p);
        }
    }

    /**
     * Applies a tree-matching algorithm at the first parent on a given tree
     * path with a specified kind and returns the result.
     *
     * @param path the path to search
     * @param kind the kind on which the matcher should be applied
     * @param m the matcher to run
     * @return true if a tree with {@link Kind} {@code kind} is found on the
     *         path and the matcher, when applied, returns true; false otherwise
     */
    public static boolean applyAt(
            TreePath path, Tree.Kind kind, Matcher m) {

        for (Tree tree : path)
            if (tree.getKind() == kind)
                return m.visit(tree, null) == Boolean.TRUE;

        return false;
    }

    /**
     * Applies a tree-matching algorithm at the any of the parent on a given tree
     * path with a specified kind and returns the result.
     *
     * @param path the path to search
     * @param kind the kind on which the matcher should be applied
     * @param m the matcher to run
     * @return true if a tree with {@link Kind} {@code kind} is found on the
     *         path and the matcher, when applied, returns true; false otherwise
     */
    public static boolean applyAtAny(
            TreePath path, Tree.Kind kind, Matcher m) {

        for (Tree tree : path) {
            if (tree.getKind() == kind) {
                if (m.visit(tree, null))
                    return true;
            }
        }

        return false;
    }

    public static boolean preceededBy(
            TreePath path, Tree.Kind kind, Matcher m) {

        StatementTree stmt = TreeUtils.enclosingOfClass(path, StatementTree.class);
        if (stmt == null)
            return false;
        TreePath p = path;
        while (p.getLeaf() != stmt) p = p.getParentPath();
        assert p.getLeaf() == stmt;

        while (p != null && p.getLeaf() instanceof StatementTree) {
            if (p.getParentPath().getLeaf() instanceof BlockTree) {
                BlockTree block = (BlockTree)p.getParentPath().getLeaf();
                for (StatementTree st : block.getStatements()) {
                    if (st == p.getLeaf())
                        break;

                    if (kind == st.getKind() && m.visit(st, null))
                        return true;
                }
            }
            p = p.getParentPath();
        }

        return false;
    }
}