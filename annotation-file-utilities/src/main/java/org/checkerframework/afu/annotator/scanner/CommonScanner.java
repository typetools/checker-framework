package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePathScanner;

/** The common base-class for all scanners that contains shared tree-traversal methods. */
public class CommonScanner extends TreePathScanner<Void, Void> {
  /** Creates a CommonScanner. */
  public CommonScanner() {}

  // Don't scan into any classes so that occurrences in nested classes
  // aren't counted.
  @Override
  public Void visitClass(ClassTree node, Void p) {
    return null;
  }
}
