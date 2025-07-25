package org.checkerframework.dataflow.cfg;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlockImpl;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.cfg.visualize.StringCFGVisualizer;
import org.plumelib.util.UniqueId;
import org.plumelib.util.UnmodifiableIdentityHashMap;

/**
 * A control flow graph (CFG for short) of a single method.
 *
 * <p>The graph is represented by the successors (methods {@link SingleSuccessorBlock#getSuccessor},
 * {@link ConditionalBlock#getThenSuccessor}, {@link ConditionalBlock#getElseSuccessor}, {@link
 * ExceptionBlock#getExceptionalSuccessors}, {@link RegularBlock#getRegularSuccessor}) and
 * predecessors (method {@link Block#getPredecessors}) of the entry and exit blocks.
 */
public class ControlFlowGraph implements UniqueId {

  /** The entry block of the control flow graph. */
  protected final SpecialBlock entryBlock;

  /** The regular exit block of the control flow graph. */
  protected final SpecialBlock regularExitBlock;

  /** The exceptional exit block of the control flow graph. */
  protected final SpecialBlock exceptionalExitBlock;

  /** The AST this CFG corresponds to. */
  public final UnderlyingAST underlyingAST;

  /** The unique ID for the next-created object. */
  private static final AtomicLong nextUid = new AtomicLong(0);

  /** The unique ID of this object. */
  private final transient long uid = nextUid.getAndIncrement();

  @Override
  public long getUid(@UnknownInitialization ControlFlowGraph this) {
    return uid;
  }

  /**
   * Maps from AST {@link Tree}s to sets of {@link Node}s.
   *
   * <ul>
   *   <li>Most Trees that produce a value will have at least one corresponding Node.
   *   <li>Trees that undergo conversions, such as boxing or unboxing, can map to two distinct
   *       Nodes. The Node for the pre-conversion value is stored in {@link #treeLookup}, while the
   *       Node for the post-conversion value is stored in {@link #convertedTreeLookup}.
   * </ul>
   *
   * Some of the mapped-to nodes (in both {@link #treeLookup} and {@link #convertedTreeLookup}) do
   * not appear in {@link #getAllNodes} because their blocks are not reachable in the control flow
   * graph. Dataflow will not compute abstract values for these nodes.
   */
  protected final IdentityHashMap<Tree, Set<Node>> treeLookup;

  /** Map from AST {@link Tree}s to post-conversion sets of {@link Node}s. */
  protected final IdentityHashMap<Tree, Set<Node>> convertedTreeLookup;

  /**
   * Map from postfix increment or decrement trees that are AST {@link UnaryTree}s to the synthetic
   * tree that is {@code v + 1} or {@code v - 1}.
   */
  protected final IdentityHashMap<UnaryTree, BinaryTree> postfixNodeLookup;

  /**
   * All return nodes (if any) encountered. Only includes return statements that actually return
   * something.
   */
  protected final List<ReturnNode> returnNodes;

  /**
   * Class declarations that have been encountered when building the control-flow graph for a
   * method.
   */
  protected final List<ClassTree> declaredClasses;

  /**
   * Lambdas encountered when building the control-flow graph for a method, variable initializer, or
   * initializer.
   */
  protected final List<LambdaExpressionTree> declaredLambdas;

  public ControlFlowGraph(
      SpecialBlock entryBlock,
      SpecialBlockImpl regularExitBlock,
      SpecialBlockImpl exceptionalExitBlock,
      UnderlyingAST underlyingAST,
      IdentityHashMap<Tree, Set<Node>> treeLookup,
      IdentityHashMap<Tree, Set<Node>> convertedTreeLookup,
      IdentityHashMap<UnaryTree, BinaryTree> postfixNodeLookup,
      List<ReturnNode> returnNodes,
      List<ClassTree> declaredClasses,
      List<LambdaExpressionTree> declaredLambdas) {
    super();
    this.entryBlock = entryBlock;
    this.underlyingAST = underlyingAST;
    this.treeLookup = treeLookup;
    this.postfixNodeLookup = postfixNodeLookup;
    this.convertedTreeLookup = convertedTreeLookup;
    this.regularExitBlock = regularExitBlock;
    this.exceptionalExitBlock = exceptionalExitBlock;
    this.returnNodes = returnNodes;
    this.declaredClasses = declaredClasses;
    this.declaredLambdas = declaredLambdas;
  }

  /**
   * Verify that this is a complete and well-formed CFG, i.e. that all internal invariants hold.
   *
   * @throws IllegalStateException if some internal invariant is violated
   */
  public void checkInvariants() {
    // TODO: this is a big data structure with many more invariants...
    for (Block b : getAllBlocks()) {

      // Each node in the block should have this block as its parent.
      for (Node n : b.getNodes()) {
        if (!Objects.equals(n.getBlock(), b)) {
          throw new IllegalStateException(
              "Node "
                  + n
                  + " in block "
                  + b
                  + " incorrectly believes it belongs to "
                  + n.getBlock());
        }
      }

      // Each successor should have this block in its predecessors.
      for (Block succ : b.getSuccessors()) {
        if (!succ.getPredecessors().contains(b)) {
          throw new IllegalStateException(
              "Block "
                  + b
                  + " has successor "
                  + succ
                  + " but does not appear in that successor's predecessors");
        }
      }

      // Each predecessor should have this block in its successors.
      for (Block pred : b.getPredecessors()) {
        if (!pred.getSuccessors().contains(b)) {
          throw new IllegalStateException(
              "Block "
                  + b
                  + " has predecessor "
                  + pred
                  + " but does not appear in that predecessor's successors");
        }
      }
    }
  }

  /**
   * Returns the set of {@link Node}s to which the {@link Tree} {@code t} corresponds, or null for
   * trees that don't produce a value.
   *
   * @param t a tree
   * @return the set of {@link Node}s to which the {@link Tree} {@code t} corresponds, or null for
   *     trees that don't produce a value
   */
  public @Nullable Set<Node> getNodesCorrespondingToTree(Tree t) {
    if (convertedTreeLookup.containsKey(t)) {
      return convertedTreeLookup.get(t);
    } else {
      return treeLookup.get(t);
    }
  }

  /**
   * Returns the entry block of the control flow graph.
   *
   * @return the entry block of the control flow graph
   */
  public SpecialBlock getEntryBlock() {
    return entryBlock;
  }

  public List<ReturnNode> getReturnNodes() {
    return returnNodes;
  }

  public SpecialBlock getRegularExitBlock() {
    return regularExitBlock;
  }

  public SpecialBlock getExceptionalExitBlock() {
    return exceptionalExitBlock;
  }

  /**
   * Returns the AST this CFG corresponds to.
   *
   * @return the AST this CFG corresponds to
   */
  public UnderlyingAST getUnderlyingAST() {
    return underlyingAST;
  }

  /**
   * Returns the set of all basic blocks in this control flow graph.
   *
   * @return the set of all basic blocks in this control flow graph
   */
  public Set<Block> getAllBlocks(
      @UnknownInitialization(ControlFlowGraph.class) ControlFlowGraph this) {
    Set<Block> visited = new LinkedHashSet<>();
    // worklist is always a subset of visited; any block in worklist is also in visited.
    Queue<Block> worklist = new ArrayDeque<>();
    Block cur = entryBlock;
    visited.add(entryBlock);

    // traverse the whole control flow graph
    while (true) {
      if (cur == null) {
        break;
      }

      for (Block b : cur.getSuccessors()) {
        if (visited.add(b)) {
          worklist.add(b);
        }
      }

      cur = worklist.poll();
    }

    return visited;
  }

  /**
   * Returns all nodes in this control flow graph.
   *
   * @return all nodes in this control flow graph
   */
  public List<Node> getAllNodes(
      @UnknownInitialization(ControlFlowGraph.class) ControlFlowGraph this) {
    List<Node> result = new ArrayList<>();
    for (Block b : getAllBlocks()) {
      result.addAll(b.getNodes());
    }
    return result;
  }

  /**
   * Returns the set of all basic blocks in this control flow graph, <b>except</b> those that are
   * only reachable via an exception whose type is ignored by parameter {@code
   * shouldIgnoreException}.
   *
   * @param shouldIgnoreException returns true if it is passed a {@code TypeMirror} that should be
   *     ignored
   * @return the set of all basic blocks in this control flow graph, <b>except</b> those that are
   *     only reachable via an exception whose type is ignored by {@code shouldIgnoreException}
   */
  public Set<Block> getAllBlocks(
      @UnknownInitialization(ControlFlowGraph.class) ControlFlowGraph this,
      Function<TypeMirror, Boolean> shouldIgnoreException) {
    // This is the return value of the method.
    Set<Block> visited = new LinkedHashSet<>();
    // `worklist` is always a subset of `visited`; any block in `worklist` is also in `visited`.
    Queue<Block> worklist = new ArrayDeque<>();
    Block cur = entryBlock;
    visited.add(entryBlock);

    // Traverse the whole control flow graph.
    while (cur != null) {
      if (cur instanceof ExceptionBlock) {
        for (Map.Entry<TypeMirror, Set<Block>> entry :
            ((ExceptionBlock) cur).getExceptionalSuccessors().entrySet()) {
          if (!shouldIgnoreException.apply(entry.getKey())) {
            for (Block b : entry.getValue()) {
              if (visited.add(b)) {
                worklist.add(b);
              }
            }
          }
        }
        Block b = ((SingleSuccessorBlockImpl) cur).getSuccessor();
        if (b != null && visited.add(b)) {
          worklist.add(b);
        }

      } else {
        for (Block b : cur.getSuccessors()) {
          if (visited.add(b)) {
            worklist.add(b);
          }
        }
      }
      cur = worklist.poll();
    }

    return visited;
  }

  /**
   * Returns the list of all nodes in this control flow graph, <b>except</b> those that are only
   * reachable via an exception whose type is ignored by parameter {@code shouldIgnoreException}.
   *
   * @param shouldIgnoreException returns true if it is passed a {@code TypeMirror} that should be
   *     ignored
   * @return the list of all nodes in this control flow graph, <b>except</b> those that are only
   *     reachable via an exception whose type is ignored by {@code shouldIgnoreException}
   */
  public List<Node> getAllNodes(
      @UnknownInitialization(ControlFlowGraph.class) ControlFlowGraph this,
      Function<TypeMirror, Boolean> shouldIgnoreException) {
    List<Node> result = new ArrayList<>();
    getAllBlocks(shouldIgnoreException).forEach(b -> result.addAll(b.getNodes()));
    return result;
  }

  /**
   * Returns all basic blocks in this control flow graph, in reversed depth-first postorder. Blocks
   * may appear more than once in the sequence.
   *
   * @return the list of all basic block in this control flow graph in reversed depth-first
   *     postorder sequence
   */
  public List<Block> getDepthFirstOrderedBlocks() {
    List<Block> dfsOrderResult = new ArrayList<>();
    Set<Block> visited = new HashSet<>();
    // worklist can contain values that are not yet in visited.
    Deque<Block> worklist = new ArrayDeque<>();
    worklist.add(entryBlock);
    while (!worklist.isEmpty()) {
      Block cur = worklist.getLast();
      if (visited.contains(cur)) {
        dfsOrderResult.add(cur);
        worklist.removeLast();
      } else {
        visited.add(cur);

        for (Block b : cur.getSuccessors()) {
          if (!visited.contains(b)) {
            worklist.add(b);
          }
        }
      }
    }

    Collections.reverse(dfsOrderResult);
    return dfsOrderResult;
  }

  /**
   * Returns an unmodifiable view of the tree-lookup map. Ignores convertedTreeLookup, though {@link
   * #getNodesCorrespondingToTree} uses that field.
   *
   * @return the unmodifiable tree-lookup map
   */
  public UnmodifiableIdentityHashMap<Tree, Set<Node>> getTreeLookup() {
    return UnmodifiableIdentityHashMap.wrap(treeLookup);
  }

  /**
   * Returns an unmodifiable view of the lookup-map of the binary tree for a postfix expression.
   *
   * @return the unmodifiable lookup-map of the binary tree for a postfix expression
   */
  public UnmodifiableIdentityHashMap<UnaryTree, BinaryTree> getPostfixNodeLookup() {
    return UnmodifiableIdentityHashMap.wrap(postfixNodeLookup);
  }

  /**
   * Returns the {@link MethodTree} of the CFG if the argument {@link Tree} maps to a {@link Node}
   * in the CFG, or null otherwise.
   *
   * @param t a tree that might correspond to a node in the CFG
   * @return the method that contains {@code t}'s Node, or null
   */
  public @Nullable MethodTree getEnclosingMethod(Tree t) {
    if (treeLookup.containsKey(t) && underlyingAST.getKind() == UnderlyingAST.Kind.METHOD) {
      UnderlyingAST.CFGMethod cfgMethod = (UnderlyingAST.CFGMethod) underlyingAST;
      return cfgMethod.getMethod();
    }
    return null;
  }

  /**
   * Returns the {@link ClassTree} of the CFG if the argument {@link Tree} maps to a {@link Node} in
   * the CFG, or null otherwise.
   *
   * @param t a tree that might be within a class
   * @return the class that contains the given tree, or null
   */
  public @Nullable ClassTree getEnclosingClass(Tree t) {
    if (treeLookup.containsKey(t) && underlyingAST.getKind() == UnderlyingAST.Kind.METHOD) {
      UnderlyingAST.CFGMethod cfgMethod = (UnderlyingAST.CFGMethod) underlyingAST;
      return cfgMethod.getClassTree();
    }
    return null;
  }

  public List<ClassTree> getDeclaredClasses() {
    return declaredClasses;
  }

  public List<LambdaExpressionTree> getDeclaredLambdas() {
    return declaredLambdas;
  }

  @Override
  public String toString() {
    CFGVisualizer<?, ?, ?> viz = new StringCFGVisualizer<>();
    viz.init(Collections.singletonMap("verbose", true));
    Map<String, Object> res = viz.visualize(this, this.getEntryBlock(), null);
    viz.shutdown();
    if (res == null) {
      return "unvisualizable " + getClass().getCanonicalName();
    }
    String stringGraph = (String) res.get("stringGraph");
    return stringGraph == null ? "unvisualizable " + getClass().getCanonicalName() : stringGraph;
  }

  /**
   * Returns a verbose string representation of this, useful for debugging.
   *
   * @return a string representation of this
   */
  public String toStringDebug() {
    String className = this.getClass().getSimpleName();
    if (className.equals("ControlFlowGraph") && this.getClass() != ControlFlowGraph.class) {
      className = this.getClass().getCanonicalName();
    }

    StringJoiner result = new StringJoiner(String.format("%n  "));
    result.add(className + " #" + getUid() + " {");
    result.add("entryBlock=" + entryBlock);
    result.add("regularExitBlock=" + regularExitBlock);
    result.add("exceptionalExitBlock=" + exceptionalExitBlock);
    String astString = underlyingAST.toString().replaceAll("\\s", " ");
    if (astString.length() > 65) {
      astString = "\"" + astString.substring(0, 60) + "\"";
    }
    result.add("underlyingAST=" + underlyingAST);
    result.add("treeLookup=" + AnalysisResult.treeLookupToString(treeLookup));
    result.add("convertedTreeLookup=" + AnalysisResult.treeLookupToString(convertedTreeLookup));
    result.add("postfixLookup=" + postfixNodeLookup);
    result.add("returnNodes=" + Node.nodeCollectionToString(returnNodes));
    result.add("declaredClasses=" + declaredClasses);
    result.add("declaredLambdas=" + declaredLambdas);
    result.add("}");
    return result.toString();
  }
}
