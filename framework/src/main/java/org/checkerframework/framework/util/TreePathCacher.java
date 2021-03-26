package org.checkerframework.framework.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * TreePathCacher is a TreeScanner that creates and caches a TreePath for a target Tree.
 *
 * <p>This class replicates some logic from TreePath.getPath but also adds caching to all
 * intermediate TreePaths that are generated. The intermediate TreePaths are reused when other
 * targets have overlapping paths.
 */
public class TreePathCacher extends TreeScanner<TreePath, Tree> {

  private final Map<Tree, TreePath> foundPaths = new HashMap<>(32);

  /**
   * The TreePath of the previous tree scanned. It is always set back to null after a scan has
   * completed.
   */
  private TreePath path;

  /**
   * Returns true if the tree is cached.
   *
   * @param target the tree to search for
   * @return true if the tree is cached
   */
  public boolean isCached(Tree target) {
    return foundPaths.containsKey(target);
  }

  /**
   * Adds the given key and value to the cache.
   *
   * @param target the tree to add
   * @param path the path to cache
   */
  public void addPath(Tree target, TreePath path) {
    foundPaths.put(target, path);
  }

  /**
   * Return the TreePath for a Tree.
   *
   * @param root the compilation unit to search in
   * @param target the target tree to look for
   * @return the TreePath corresponding to target, or null if target is not found in the compilation
   *     root
   */
  public @Nullable TreePath getPath(CompilationUnitTree root, @FindDistinct Tree target) {
    // This method uses try/catch and the private {@code Result} exception for control flow to
    // stop the superclass from scanning other subtrees when target is found.

    if (foundPaths.containsKey(target)) {
      return foundPaths.get(target);
    }

    TreePath path = new TreePath(root);
    if (path.getLeaf() == target) {
      return path;
    }

    try {
      this.scan(path, target);
    } catch (Result result) {
      return result.path;
    }
    // If a path wasn't found, cache null so the whole compilation unit isn't searched again.
    foundPaths.put(target, null);
    return null;
  }

  private static class Result extends Error {
    private static final long serialVersionUID = 4948452207518392627L;
    TreePath path;

    Result(TreePath path) {
      this.path = path;
    }
  }

  public void clear() {
    foundPaths.clear();
  }

  /** Scan a single node. The current path is updated for the duration of the scan. */
  @SuppressWarnings("interning:not.interned") // assertion
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
