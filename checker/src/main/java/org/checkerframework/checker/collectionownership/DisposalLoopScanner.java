package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;

/** Scans one method tree and discovers disposal loops in its CFG. */
public class DisposalLoopScanner extends TreeScanner<Void, Void> {

  /** The CO type factory used for collection-ownership queries. */
  private final CollectionOwnershipAnnotatedTypeFactory atypeFactory;

  /** The RLCC type factory used for declaration lookup and ignored-exception checks. */
  private final RLCCalledMethodsAnnotatedTypeFactory rlccAtf;

  /** The CFG of the method currently being scanned. */
  private final ControlFlowGraph cfg;

  /** The method currently being scanned, or {@code null} if the CFG is not for a method. */
  private final @Nullable MethodTree methodTree;

  /** Disposal loops discovered while scanning the current method tree. */
  private final Set<DisposalLoop> disposalLoops = new LinkedHashSet<>();

  /** Matcher for indexed `for` disposal loops. */
  private final IndexedForDisposalLoopMatcher indexedForDisposalLoopMatcher;

  /** Matcher for `while` disposal loops. */
  private final WhileDisposalLoopMatcher whileDisposalLoopMatcher;

  /** Resolver for enhanced-`for` disposal loops. */
  private final EnhancedForDisposalLoopResolver enhancedForDisposalLoopResolver;

  /**
   * Creates a scanner for disposal loops in one method CFG.
   *
   * @param atypeFactory the CO type factory
   * @param rlccAtf the RLCC type factory
   * @param cfg the CFG to scan
   */
  public DisposalLoopScanner(
      CollectionOwnershipAnnotatedTypeFactory atypeFactory,
      RLCCalledMethodsAnnotatedTypeFactory rlccAtf,
      ControlFlowGraph cfg) {
    this.atypeFactory = atypeFactory;
    this.rlccAtf = rlccAtf;
    this.cfg = cfg;
    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST.getKind() == UnderlyingAST.Kind.METHOD) {
      this.methodTree = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
    } else {
      this.methodTree = null;
    }
    this.indexedForDisposalLoopMatcher =
        new IndexedForDisposalLoopMatcher(this.atypeFactory, this.cfg, this.methodTree);
    this.whileDisposalLoopMatcher =
        new WhileDisposalLoopMatcher(this.atypeFactory, this.rlccAtf, this.cfg, this.methodTree);
    this.enhancedForDisposalLoopResolver =
        new EnhancedForDisposalLoopResolver(this.atypeFactory, this.cfg, this.methodTree);
  }

  /**
   * Scans a tree and returns the disposal loops discovered in it.
   *
   * @param tree the tree to scan
   * @return the disposal loops discovered in {@code tree}
   */
  public Set<DisposalLoop> scanTree(Tree tree) {
    disposalLoops.clear();
    scan(tree, null);
    return new LinkedHashSet<>(disposalLoops);
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
      DisposalLoop disposalLoop = indexedForDisposalLoopMatcher.match(tree);
      if (disposalLoop != null) {
        disposalLoops.add(disposalLoop);
      }
    }
    return super.visitForLoop(tree, p);
  }

  /**
   * Matches while-loops that may fulfill collection obligations and resolves their CFG-local loop
   * facts.
   *
   * @param tree the while-loop to inspect
   * @param p the scan parameter
   * @return always {@code null}
   */
  @Override
  public Void visitWhileLoop(WhileLoopTree tree, Void p) {
    DisposalLoop disposalLoop = whileDisposalLoopMatcher.match(tree);
    if (disposalLoop != null) {
      disposalLoops.add(disposalLoop);
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
    DisposalLoop disposalLoop = enhancedForDisposalLoopResolver.match(tree);
    if (disposalLoop != null) {
      disposalLoops.add(disposalLoop);
    }
    return super.visitEnhancedForLoop(tree, p);
  }
}
