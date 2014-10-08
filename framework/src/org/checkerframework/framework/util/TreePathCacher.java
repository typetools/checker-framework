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

    private static class TreePathSearcher extends TreePathScanner<TreePath,Tree> {

        private Map<Tree, TreePath> foundPaths = new HashMap<>();

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
            if (tree != null && foundPaths.get(tree) == null) {
                foundPaths.put(tree, new TreePath(getCurrentPath(), tree));
            }

            if (tree == target) {
                throw new Result(new TreePath(getCurrentPath(), target));
            }
            return super.scan(tree, target);
        }
    }
}
