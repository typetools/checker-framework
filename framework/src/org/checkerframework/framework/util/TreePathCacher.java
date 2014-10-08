package org.checkerframework.framework.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mcarthur on 10/7/14.
 */
public class TreePathCacher {

    private Map<CompilationUnitTree, SoftReference<TreePathSearcher>> compilationUnitCache = new HashMap<>();

    public TreePath getPath(CompilationUnitTree root, Tree tree) {

        SoftReference<TreePathSearcher> searcher = compilationUnitCache.get(root);
        if (searcher == null) {
            searcher = new SoftReference<>(new TreePathSearcher());
            compilationUnitCache.put(root, searcher);
        }
        return searcher.get().getPath(new TreePath(root), tree);
    }

    private static class TreePathSearcher extends TreeScanner<TreePath,Tree> {

        private Map<Tree, TreePath> foundPaths = new HashMap<>();
        private TreePath path;

        class Result extends Error {
            static final long serialVersionUID = -5942088234594905625L;

            TreePath path;
            Result(TreePath path) {
                this.path = path;
            }
        }

        public TreePath getPath(TreePath path, Tree target) {
            if (path.getLeaf() == target) {
                return path;
            }
            if (foundPaths.containsKey(target)) {
                return foundPaths.get(target);
            }

            try {
                this.scan(path, target);
            } catch (Result result) {
                return result.path;
            }
            return null;
        }

        /**
         * Scan a single node.
         * The current path is updated for the duration of the scan.
         */
        @Override
        public TreePath scan(Tree tree, Tree target) {
            TreePath prev = path;
            if (tree != null && foundPaths.get(tree) == null) {
                TreePath current = new TreePath(path, tree);
                foundPaths.put(tree, current);
                path = current;
            } else {
                this.path = foundPaths.get(tree);
            }

            if (tree == target) {
                throw new Result(path);
            }
            try {
                return super.scan(tree, target);
            } finally {
                this.path = prev;
            }
        }
    }
}
