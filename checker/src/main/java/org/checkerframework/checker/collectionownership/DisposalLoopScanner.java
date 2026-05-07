package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;

/** Scans one method tree and discovers {@link DisposalLoopInfo}'s in it. */
public class DisposalLoopScanner extends TreeScanner<Void, Void> {

  /** The CO type factory used for collection-ownership queries. */
  private final CollectionOwnershipAnnotatedTypeFactory coAtf;

  /** The RLCC type factory used for declaration lookup. */
  private final RLCCalledMethodsAnnotatedTypeFactory rlccAtf;

  /** The CFG of the method currently being scanned. */
  private final ControlFlowGraph cfg;

  /** Disposal loops discovered while scanning the current method tree. */
  private final Set<DisposalLoopInfo> disposalLoopInfos = new LinkedHashSet<>();

  /** Matcher for indexed `for` disposal loops. */
  private final IndexedForDisposalLoopMatcher indexedForDisposalLoopMatcher;

  /** Matcher for `while` disposal loops. */
  private final WhileDisposalLoopMatcher whileDisposalLoopMatcher;

  /** Resolver for enhanced-`for` disposal loops. */
  private final EnhancedForDisposalLoopResolver enhancedForDisposalLoopResolver;

  /**
   * Creates a scanner for disposal loops in one method CFG.
   *
   * @param coAtf the CO type factory
   * @param rlccAtf the RLCC type factory
   * @param cfg the CFG to scan
   */
  public DisposalLoopScanner(
      CollectionOwnershipAnnotatedTypeFactory coAtf,
      RLCCalledMethodsAnnotatedTypeFactory rlccAtf,
      ControlFlowGraph cfg) {
    this.coAtf = coAtf;
    this.rlccAtf = rlccAtf;
    this.cfg = cfg;
    this.indexedForDisposalLoopMatcher = new IndexedForDisposalLoopMatcher(this.coAtf, this.cfg);
    this.whileDisposalLoopMatcher =
        new WhileDisposalLoopMatcher(this.coAtf, this.rlccAtf, this.cfg);
    this.enhancedForDisposalLoopResolver =
        new EnhancedForDisposalLoopResolver(this.coAtf, this.cfg);
  }

  /**
   * Scans a tree and returns the disposal loops discovered in it.
   *
   * @param tree the tree to scan
   * @return the disposal loops discovered in {@code tree}
   */
  public Set<DisposalLoopInfo> scanTree(Tree tree) {
    scan(tree, null);
    return disposalLoopInfos;
  }

  /**
   * Skips nested lambdas when scanning the enclosing method.
   *
   * @param tree the lambda expression
   * @param p the scan parameter
   * @return always {@code null}
   */
  @Override
  public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
    return null;
  }

  /**
   * Skips nested classes when scanning the enclosing method.
   *
   * @param tree the class tree
   * @param p the scan parameter
   * @return always {@code null}
   */
  @Override
  public Void visitClass(ClassTree tree, Void p) {
    return null;
  }

  /**
   * Syntactically matches indexed for-loops that iterate over all elements of a collection.
   *
   * @param tree the for-loop to inspect
   * @param p the scan parameter
   * @return always {@code null}
   */
  @Override
  public Void visitForLoop(ForLoopTree tree, Void p) {
    boolean singleLoopVariable = tree.getUpdate().size() == 1 && tree.getInitializer().size() == 1;
    if (singleLoopVariable) {
      DisposalLoopInfo disposalLoopInfo = indexedForDisposalLoopMatcher.match(tree);
      if (disposalLoopInfo != null) {
        disposalLoopInfos.add(disposalLoopInfo);
      }
    }
    return super.visitForLoop(tree, p);
  }

  /**
   * Matches a {@link DisposalLoopInfo} that uses while-loops and resolves their CFG-local loop
   * facts.
   *
   * @param tree the while-loop to inspect
   * @param p the scan parameter
   * @return always {@code null}
   */
  @Override
  public Void visitWhileLoop(WhileLoopTree tree, Void p) {
    DisposalLoopInfo disposalLoopInfo = whileDisposalLoopMatcher.match(tree);
    if (disposalLoopInfo != null) {
      disposalLoopInfos.add(disposalLoopInfo);
    }
    return super.visitWhileLoop(tree, p);
  }

  /**
   * Matches enhanced-for-loops that may fulfill collection obligations and resolves their desugared
   * iterator CFG shape.
   *
   * @param tree the enhanced-for-loop to inspect
   * @param p the scan parameter
   * @return always {@code null}
   */
  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
    DisposalLoopInfo disposalLoopInfo = enhancedForDisposalLoopResolver.match(tree);
    if (disposalLoopInfo != null) {
      disposalLoopInfos.add(disposalLoopInfo);
    }
    return super.visitEnhancedForLoop(tree, p);
  }
}
