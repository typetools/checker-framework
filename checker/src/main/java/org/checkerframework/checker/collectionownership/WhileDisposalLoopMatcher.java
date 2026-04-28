package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.TreeUtils;

/** Matches `while` {@link DisposalLoop} that iterates over a resource collection. */
final class WhileDisposalLoopMatcher {

  /** The CO type factory used for collection-ownership queries. */
  private final CollectionOwnershipAnnotatedTypeFactory coAtf;

  /** The RLCC type factory used for declaration lookup. */
  private final RLCCalledMethodsAnnotatedTypeFactory rlccAtf;

  /** The CFG of the method currently being scanned. */
  private final ControlFlowGraph cfg;

  /** Lazily-computed CFG facts for while-loop resolution. */
  private @Nullable WhileLoopResolutionCache whileLoopCache;

  /**
   * Creates a matcher for `while` disposal loops.
   *
   * @param coAtf the CO type factory
   * @param rlccAtf the RLCC type factory
   * @param cfg the CFG being scanned
   */
  WhileDisposalLoopMatcher(
      CollectionOwnershipAnnotatedTypeFactory coAtf,
      RLCCalledMethodsAnnotatedTypeFactory rlccAtf,
      ControlFlowGraph cfg) {
    this.coAtf = coAtf;
    this.rlccAtf = rlccAtf;
    this.cfg = cfg;
  }

  /** Lazily-computed CFG facts used to resolve potentially fulfilling while loops. */
  private static final class WhileLoopResolutionCache {

    /** A back edge in the CFG. */
    private static final class BlockEdge {
      /** Source block of the back edge. */
      final Block sourceBlock;

      /** Target block of the back edge. */
      final Block targetBlock;

      /**
       * Creates a CFG back edge description.
       *
       * @param sourceBlock source block of the back edge
       * @param targetBlock target block of the back edge
       */
      BlockEdge(Block sourceBlock, Block targetBlock) {
        this.sourceBlock = sourceBlock;
        this.targetBlock = targetBlock;
      }
    }

    /** Reachable CFG blocks in the current method. */
    private final Set<Block> reachableBlocks;

    /** Back edges among {@link #reachableBlocks}. */
    private final List<BlockEdge> backEdges;

    /** Natural loops for back edges, computed lazily. */
    private final IdentityHashMap<BlockEdge, Set<Block>> naturalLoopsByBackEdge =
        new IdentityHashMap<>();

    /**
     * Creates CFG facts for resolving potentially fulfilling while loops in the given CFG.
     *
     * @param cfg the enclosing method CFG
     */
    private WhileLoopResolutionCache(ControlFlowGraph cfg) {
      Block entryBlock = cfg.getEntryBlock();
      this.reachableBlocks = CollectionOwnershipUtils.reachableFrom(entryBlock);
      Map<Block, Set<Block>> dominators = computeDominators(entryBlock, reachableBlocks);
      this.backEdges = findBackEdges(reachableBlocks, dominators);
    }

    /**
     * Returns the back edges among the reachable blocks in the current CFG.
     *
     * @return the CFG back edges
     */
    private List<BlockEdge> getBackEdges() {
      return backEdges;
    }

    /**
     * Returns the natural loop induced by the given back edge, computing it lazily if needed.
     *
     * @param backEdge the back edge
     * @return the natural loop induced by the given back edge
     */
    private Set<Block> getNaturalLoopForBackEdge(BlockEdge backEdge) {
      return naturalLoopsByBackEdge.computeIfAbsent(
          backEdge,
          ignored -> naturalLoop(backEdge.sourceBlock, backEdge.targetBlock, reachableBlocks));
    }

    /**
     * Computes dominators for the reachable blocks in the current CFG.
     *
     * @param entryBlock the CFG entry block
     * @param reachableBlocks reachable blocks in the CFG
     * @return dominators for each reachable block
     */
    private static Map<Block, Set<Block>> computeDominators(
        Block entryBlock, Set<Block> reachableBlocks) {
      Map<Block, Set<Block>> dominators = new HashMap<>();

      for (Block block : reachableBlocks) {
        if (block.equals(entryBlock)) {
          dominators.put(block, new HashSet<>(Collections.singleton(entryBlock)));
        } else {
          dominators.put(block, new HashSet<>(reachableBlocks));
        }
      }

      boolean changed;
      do {
        changed = false;
        for (Block block : reachableBlocks) {
          if (block.equals(entryBlock)) {
            continue;
          }

          Set<Block> newDominators = null;
          for (Block predecessor : block.getPredecessors()) {
            if (predecessor == null || !reachableBlocks.contains(predecessor)) {
              continue;
            }
            Set<Block> predecessorDominators = dominators.get(predecessor);
            if (predecessorDominators == null) {
              continue;
            }
            if (newDominators == null) {
              newDominators = new HashSet<>(predecessorDominators);
            } else {
              newDominators.retainAll(predecessorDominators);
            }
          }

          if (newDominators == null) {
            newDominators = new HashSet<>();
          }
          newDominators.add(block);

          if (!newDominators.equals(dominators.get(block))) {
            dominators.put(block, newDominators);
            changed = true;
          }
        }
      } while (changed);

      return dominators;
    }

    /**
     * Returns the back edges among the reachable blocks in the current CFG.
     *
     * @param reachableBlocks reachable blocks in the CFG
     * @param dominators dominators for each reachable block
     * @return the CFG back edges
     */
    private static List<BlockEdge> findBackEdges(
        Set<Block> reachableBlocks, Map<Block, Set<Block>> dominators) {
      java.util.List<BlockEdge> backEdges = new java.util.ArrayList<>();
      for (Block sourceBlock : reachableBlocks) {
        for (Block targetBlock : sourceBlock.getSuccessors()) {
          if (targetBlock == null || !reachableBlocks.contains(targetBlock)) {
            continue;
          }
          Set<Block> sourceDominators = dominators.get(sourceBlock);
          if (sourceDominators != null && sourceDominators.contains(targetBlock)) {
            backEdges.add(new BlockEdge(sourceBlock, targetBlock));
          }
        }
      }
      return backEdges;
    }

    /**
     * Returns the natural loop induced by the back edge {@code sourceBlock -> targetBlock}.
     *
     * @param sourceBlock the source of the back edge
     * @param targetBlock the target of the back edge
     * @param reachableBlocks reachable blocks in the CFG
     * @return the natural loop induced by the back edge
     */
    private static Set<Block> naturalLoop(
        Block sourceBlock, Block targetBlock, Set<Block> reachableBlocks) {
      Set<Block> loopBlocks = new HashSet<>();
      ArrayDeque<Block> stack = new ArrayDeque<>();

      loopBlocks.add(targetBlock);
      if (loopBlocks.add(sourceBlock)) {
        stack.push(sourceBlock);
      }

      while (!stack.isEmpty()) {
        Block block = stack.pop();
        for (Block predecessor : block.getPredecessors()) {
          if (predecessor == null || !reachableBlocks.contains(predecessor)) {
            continue;
          }
          if (loopBlocks.add(predecessor) && !predecessor.equals(targetBlock)) {
            stack.push(predecessor);
          }
        }
      }
      return loopBlocks;
    }
  }

  /**
   * Description of a supported while disposal loop header form.
   *
   * <p>Each header form determines which extraction methods are allowed in the loop body.
   */
  private static final class WhileSpec {
    /** Methods that may extract an element when this header form is used. */
    final Set<String> extractMethods;

    /**
     * Creates a while-loop header specification.
     *
     * @param extractMethods methods that may extract an element from the looped collection
     */
    WhileSpec(Set<String> extractMethods) {
      this.extractMethods = extractMethods;
    }
  }

  /** Iterator form: {@code while (it.hasNext()) { ... it.next() ... }}. */
  private static final WhileSpec ITERATOR_SPEC = new WhileSpec(Collections.singleton("next"));

  /**
   * Non-empty collection form: {@code while (!c.isEmpty()) { ... c.poll()/pop/removeFirst/... ...
   * }}, including {@code size() > 0} and {@code 0 < size()} variants.
   */
  private static final WhileSpec NONEMPTY_SPEC =
      new WhileSpec(
          new HashSet<>(
              Arrays.asList(
                  "poll", "pollFirst", "pollLast", "remove", "removeFirst", "removeLast", "pop")));

  /**
   * AST facts recovered from a matched while-loop header.
   *
   * <p>{@link #collectionTree} is the collection whose element obligations may be discharged.
   * {@link #headerVar} is the iterator or collection variable constrained by the header. {@link
   * #collectionVarNameForBailout} names the collection variable whose writes should invalidate the
   * match when present.
   */
  private static final class WhileHeaderMatch {
    /** Collection expression whose element obligations may be discharged. */
    final ExpressionTree collectionTree;

    /** Collection variable name whose writes should invalidate the match, if one exists. */
    final @Nullable Name collectionVarNameForBailout;

    /** Iterator variable or collection variable constrained by the loop header. */
    final Name headerVar;

    /** Accepted extraction shape for the matched loop header. */
    final WhileSpec spec;

    /**
     * Creates a summary of the AST facts recovered from a matched while-loop header.
     *
     * @param collectionTree the owning collection expression to mark
     * @param collectionVarNameForBailout collection variable whose writes invalidate the match
     * @param headerVar iterator or collection variable constrained by the header
     * @param spec accepted extraction shape for the matched loop header
     */
    WhileHeaderMatch(
        ExpressionTree collectionTree,
        @Nullable Name collectionVarNameForBailout,
        Name headerVar,
        WhileSpec spec) {
      this.collectionTree = collectionTree;
      this.collectionVarNameForBailout = collectionVarNameForBailout;
      this.headerVar = headerVar;
      this.spec = spec;
    }
  }

  /**
   * One extracted element use recovered from a while-loop body.
   *
   * <p>The extraction call is the expression that removes or advances to the next element, such as
   * {@code it.next()}, {@code q.poll()}, or {@code s.pop()}.
   */
  private static final class BodyExtraction {
    /** Extraction call such as {@code it.next()}, {@code q.poll()}, or {@code s.pop()}. */
    final MethodInvocationTree extractionCall;

    /**
     * Creates a body extraction summary.
     *
     * @param extractionCall extraction call found in the loop body
     */
    BodyExtraction(MethodInvocationTree extractionCall) {
      this.extractionCall = extractionCall;
    }
  }

  /**
   * Matches a {@link DisposalLoop} that uses a while-loop and resolves its CFG-local loop facts.
   *
   * <p>Supported header shapes are iterator loops such as {@code while (it.hasNext())} and
   * non-empty collection loops such as {@code while (!q.isEmpty())}, {@code while (q.size() > 0)},
   * and {@code while (0 < q.size())}.
   *
   * @param tree the while-loop to inspect
   * @return the matched disposal loop, or {@code null} if the loop does not match
   */
  @Nullable DisposalLoop match(WhileLoopTree tree) {
    ExpressionTree condNoParens = TreeUtils.withoutParens(tree.getCondition());
    WhileHeaderMatch header = matchWhileHeader(condNoParens);
    if (header == null) {
      return null;
    }
    List<? extends StatementTree> bodyStatements =
        CollectionOwnershipUtils.asStatementList(tree.getStatement());
    if (bodyStatements == null) {
      return null;
    }
    BodyExtraction extraction =
        findSingleExtractionInWhileBody(
            bodyStatements,
            header.headerVar,
            header.collectionVarNameForBailout,
            header.spec.extractMethods);
    if (extraction == null) {
      return null;
    }
    Block condBlock = CollectionOwnershipUtils.firstBlockForTree(cfg, condNoParens);
    if (condBlock == null) {
      return null;
    }
    ConditionalBlock cblock = findConditionalSuccessor(condBlock);
    if (cblock == null) {
      Block peeled = peelExceptionBlocksToPred(condBlock);
      if (peeled != null) {
        cblock = findConditionalSuccessor(peeled);
      }
    }
    if (cblock == null) {
      return null;
    }

    Block loopBodyEntryBlock = cblock.getThenSuccessor();
    Node elementNode = CollectionOwnershipUtils.anyNodeForTree(cfg, extraction.extractionCall);
    if (elementNode == null) {
      return null;
    }

    Block loopUpdateBlock =
        chooseLoopUpdateBlockForPotentiallyFulfillingLoop(
            loopBodyEntryBlock, cblock, getOrCreateWhileLoopCache());
    if (loopUpdateBlock != null) {
      return new DisposalLoop(
          header.collectionTree,
          extraction.extractionCall,
          elementNode,
          CollectionOwnershipUtils.cfgAssociatedTreeFor(cfg, condNoParens),
          cblock,
          loopBodyEntryBlock,
          loopUpdateBlock);
    }
    return null;
  }

  /**
   * Matches supported while-loop header forms and returns the recovered loop facts.
   *
   * <p>Supported forms are: {@code while (it.hasNext())}, {@code while (!c.isEmpty())}, {@code
   * while (c.size() > 0)}, and {@code while (0 < c.size())}.
   *
   * @param cond the while-loop condition with parentheses removed
   * @return the recovered header facts, or {@code null} if the header is unsupported
   */
  private @Nullable WhileHeaderMatch matchWhileHeader(ExpressionTree cond) {
    if (cond instanceof MethodInvocationTree mit) {
      if (TreeUtils.isHasNextCall(mit)) {
        ExpressionTree recv = receiverOfInvocation(mit);
        Name itName = CollectionOwnershipUtils.getNameFromExpressionTree(recv);
        if (itName == null) {
          return null;
        }
        ExpressionTree colExpr = recoverCollectionFromIteratorReceiver(recv);
        if (colExpr == null) {
          return null;
        }
        Name colName = CollectionOwnershipUtils.getNameFromExpressionTree(colExpr);
        return new WhileHeaderMatch(colExpr, colName, itName, ITERATOR_SPEC);
      }
    }

    if (cond instanceof UnaryTree unaryTreeCond && cond.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
      ExpressionTree inner = TreeUtils.withoutParens(unaryTreeCond.getExpression());
      WhileHeaderMatch m = matchNonEmptyFromExpr(inner);
      if (m != null) {
        return m;
      }
    }

    if (cond instanceof BinaryTree binaryTreeCond) {
      WhileHeaderMatch m = matchNonEmptyFromSize(binaryTreeCond);
      if (m != null) {
        return m;
      }
    }

    return null;
  }

  /**
   * Matches a non-empty collection condition of the form {@code !c.isEmpty()}.
   *
   * @param inner the expression under the logical complement
   * @return the recovered header facts, or {@code null} if the expression does not match
   */
  private @Nullable WhileHeaderMatch matchNonEmptyFromExpr(ExpressionTree inner) {
    if (!(inner instanceof MethodInvocationTree mit)) {
      return null;
    }
    if (!isIsEmptyCall(mit)) {
      return null;
    }
    ExpressionTree recv = receiverOfInvocation(mit);
    if (recv == null) {
      return null;
    }
    Name varName = CollectionOwnershipUtils.getNameFromExpressionTree(recv);
    if (varName == null) {
      return null;
    }
    Element recvElt = TreeUtils.elementFromTree(recv);
    if (!ResourceLeakUtils.isCollection(recvElt, coAtf)) {
      return null;
    }
    ExpressionTree colTree = CollectionOwnershipUtils.baseExpression(recv);
    if (colTree == null) {
      return null;
    }
    return new WhileHeaderMatch(colTree, varName, varName, NONEMPTY_SPEC);
  }

  /**
   * Matches a non-empty collection condition of the form {@code c.size() > 0} or {@code 0 <
   * c.size()}.
   *
   * @param condition the binary condition
   * @return the recovered header facts, or {@code null} if the expression does not match
   */
  private @Nullable WhileHeaderMatch matchNonEmptyFromSize(BinaryTree condition) {
    Tree.Kind k = condition.getKind();
    if (k != Tree.Kind.GREATER_THAN && k != Tree.Kind.LESS_THAN) {
      return null;
    }

    ExpressionTree left = TreeUtils.withoutParens(condition.getLeftOperand());
    ExpressionTree right = TreeUtils.withoutParens(condition.getRightOperand());

    MethodInvocationTree sizeCall = null;
    LiteralTree zero = null;

    if (k == Tree.Kind.GREATER_THAN) {
      if (left instanceof MethodInvocationTree mitLeft && right instanceof LiteralTree ltRight) {
        sizeCall = mitLeft;
        zero = ltRight;
      }
    } else {
      if (left instanceof LiteralTree ltLeft && right instanceof MethodInvocationTree ltRight) {
        zero = ltLeft;
        sizeCall = ltRight;
      }
    }

    if (sizeCall == null
        || !(zero.getValue() instanceof Integer)
        || (Integer) zero.getValue() != 0) {
      return null;
    }
    if (!TreeUtils.isSizeAccess(sizeCall)) {
      return null;
    }

    ExpressionTree recv = receiverOfInvocation(sizeCall);
    if (recv == null) {
      return null;
    }

    Name varName = CollectionOwnershipUtils.getNameFromExpressionTree(recv);
    if (varName == null) {
      return null;
    }

    Element recvElt = TreeUtils.elementFromTree(recv);
    if (!ResourceLeakUtils.isCollection(recvElt, coAtf)) {
      return null;
    }

    ExpressionTree colTree = CollectionOwnershipUtils.baseExpression(recv);
    if (colTree == null) {
      return null;
    }

    return new WhileHeaderMatch(colTree, varName, varName, NONEMPTY_SPEC);
  }

  /**
   * Returns whether the given invocation is an {@code isEmpty()} call with no arguments.
   *
   * @param invocation a method invocation
   * @return true if {@code invocation} is an {@code isEmpty()} call with no arguments
   */
  private boolean isIsEmptyCall(MethodInvocationTree invocation) {
    ExpressionTree sel = invocation.getMethodSelect();
    if (!(sel instanceof MemberSelectTree ms)) {
      return false;
    }
    return ms.getIdentifier().contentEquals("isEmpty") && invocation.getArguments().isEmpty();
  }

  /**
   * Returns the explicit receiver of the given invocation, if present.
   *
   * @param invocation a method invocation
   * @return the explicit receiver, or {@code null} if none exists
   */
  private @Nullable ExpressionTree receiverOfInvocation(MethodInvocationTree invocation) {
    ExpressionTree sel = invocation.getMethodSelect();
    if (sel instanceof MemberSelectTree memberSelectTree) {
      return memberSelectTree.getExpression();
    }
    return null;
  }

  /**
   * Recovers the collection expression from an iterator receiver in a header such as {@code while
   * (it.hasNext())}.
   *
   * <p>This only recognizes local iterator variables initialized by {@code col.iterator()}.
   *
   * @param iteratorExpr the iterator receiver expression
   * @return the collection expression, or {@code null} if it cannot be recovered
   */
  private @Nullable ExpressionTree recoverCollectionFromIteratorReceiver(
      ExpressionTree iteratorExpr) {
    if (iteratorExpr == null) {
      return null;
    }

    Element itElt = TreeUtils.elementFromTree(iteratorExpr);
    if (!(itElt instanceof VariableElement)) {
      return null;
    }

    if (itElt.getKind() != ElementKind.LOCAL_VARIABLE) {
      return null;
    }

    Tree decl = rlccAtf.declarationFromElement(itElt);
    if (!(decl instanceof VariableTree variableTreeDecl)) {
      return null;
    }

    ExpressionTree init = variableTreeDecl.getInitializer();
    if (!(init instanceof MethodInvocationTree initCall)) {
      return null;
    }

    ExpressionTree sel = initCall.getMethodSelect();
    if (!(sel instanceof MemberSelectTree ms)) {
      return null;
    }

    if (!ms.getIdentifier().contentEquals("iterator") || !initCall.getArguments().isEmpty()) {
      return null;
    }

    ExpressionTree colExpr = ms.getExpression();
    Element colElt = TreeUtils.elementFromTree(colExpr);
    if (!ResourceLeakUtils.isCollection(colElt, coAtf)) {
      return null;
    }

    return CollectionOwnershipUtils.baseExpression(colExpr);
  }

  /**
   * Finds exactly one extraction in the loop body. If 0 or >1 extractions occur, returns {@code
   * null}.
   *
   * <p>This matcher rejects writes to the iterator/header variable and, when present, to the
   * collection variable itself, because such writes invalidate the header/body correspondence used
   * by later CFG verification.
   *
   * @param statements the loop body statements
   * @param headerVar the iterator or collection variable constrained by the header
   * @param collectionVarName the collection variable to protect from writes, if any
   * @param allowedExtractMethods the extraction methods allowed by the matched header
   * @return the unique extraction in the loop body, or {@code null} if the body is unsupported
   */
  private @Nullable BodyExtraction findSingleExtractionInWhileBody(
      List<? extends StatementTree> statements,
      Name headerVar,
      @Nullable Name collectionVarName,
      Set<String> allowedExtractMethods) {

    AtomicBoolean illegal = new AtomicBoolean(false);
    final MethodInvocationTree[] extraction = new MethodInvocationTree[] {null};
    final int[] extractionCount = new int[] {0};

    TreeScanner<Void, Void> scanner =
        new TreeScanner<Void, Void>() {

          private void markWriteIfTargetsHeaderOrCollection(ExpressionTree lhs) {
            Name assigned = CollectionOwnershipUtils.getNameFromExpressionTree(lhs);
            if (assigned != null) {
              if (assigned == headerVar) illegal.set(true);
              if (collectionVarName != null && assigned == collectionVarName) illegal.set(true);
            }
          }

          private void recordExtractionIfAny(ExpressionTree expr) {
            expr = TreeUtils.withoutParens(expr);
            if (!(expr instanceof MethodInvocationTree mit)) {
              return;
            }

            if (!isExtractionCallOnHeaderVar(mit, headerVar, allowedExtractMethods)) {
              return;
            }

            extractionCount[0]++;
            if (extractionCount[0] > 1) {
              illegal.set(true);
              return;
            }
            extraction[0] = mit;
          }

          @Override
          public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
            markWriteIfTargetsHeaderOrCollection(node.getVariable());
            return super.visitCompoundAssignment(node, p);
          }

          @Override
          public Void visitAssignment(AssignmentTree node, Void p) {
            markWriteIfTargetsHeaderOrCollection(node.getVariable());
            recordExtractionIfAny(node.getExpression());
            return super.visitAssignment(node, p);
          }

          @Override
          public Void visitVariable(VariableTree vt, Void p) {
            ExpressionTree init = vt.getInitializer();
            if (init != null) {
              recordExtractionIfAny(init);
            }
            return super.visitVariable(vt, p);
          }

          @Override
          public Void visitMethodInvocation(MethodInvocationTree mit, Void p) {
            ExpressionTree sel = mit.getMethodSelect();
            if (sel instanceof MemberSelectTree memberSelect) {
              ExpressionTree recv = memberSelect.getExpression();
              recordExtractionIfAny(recv);
            }
            return super.visitMethodInvocation(mit, p);
          }
        };

    for (StatementTree st : statements) {
      scanner.scan(st, null);
      if (illegal.get()) break;
    }

    if (illegal.get() || extraction[0] == null || extractionCount[0] != 1) {
      return null;
    }
    return new BodyExtraction(extraction[0]);
  }

  /**
   * Returns whether the given invocation is an allowed extraction call on the matched header
   * variable.
   *
   * @param invocation a method invocation
   * @param headerVar the iterator or collection variable constrained by the header
   * @param allowedExtractMethods extraction methods permitted by the matched header form
   * @return true if {@code invocation} is an allowed extraction call on {@code headerVar}
   */
  private boolean isExtractionCallOnHeaderVar(
      MethodInvocationTree invocation, Name headerVar, Set<String> allowedExtractMethods) {
    ExpressionTree sel = invocation.getMethodSelect();
    if (!(sel instanceof MemberSelectTree ms)) {
      return false;
    }
    String methodName = ms.getIdentifier().toString();
    if (!allowedExtractMethods.contains(methodName)) {
      return false;
    }
    if (!invocation.getArguments().isEmpty()) {
      return false;
    }
    Name recv = CollectionOwnershipUtils.getNameFromExpressionTree(ms.getExpression());
    return recv != null && recv == headerVar;
  }

  /**
   * Returns the conditional successor reached from the given block, if one is immediately visible.
   *
   * @param block a CFG block
   * @return the conditional successor of {@code block}, or {@code null} if none is found
   */
  private @Nullable ConditionalBlock findConditionalSuccessor(Block block) {
    for (Block succ : block.getSuccessors()) {
      if (succ instanceof ConditionalBlock conditionalBlockSucc) {
        return conditionalBlockSucc;
      }
    }
    if (block instanceof SingleSuccessorBlock singleSuccessorBlock) {
      Block succ = singleSuccessorBlock.getSuccessor();
      if (succ instanceof ConditionalBlock conditionalBlockSucc) {
        return conditionalBlockSucc;
      }
    }
    return null;
  }

  /**
   * Walks backward through exception blocks to recover the predecessor block that leads to the
   * actual loop conditional.
   *
   * <p>This is needed because loop conditions such as {@code iterator.hasNext()} may be represented
   * by exception blocks before reaching the conditional branch.
   *
   * @param block a CFG block
   * @return a predecessor block to retry from, or {@code null} if no such block is found
   */
  private @Nullable Block peelExceptionBlocksToPred(Block block) {
    Block cur = block;
    Set<Block> visitedBlocks = new HashSet<>();
    while (cur instanceof ExceptionBlock && visitedBlocks.add(cur)) {
      Set<Block> preds = cur.getPredecessors();
      if (preds.size() != 1) {
        break;
      }
      Block p = preds.iterator().next();
      if (p == null) {
        break;
      }
      cur = p;
    }
    return cur;
  }

  /**
   * Chooses the best loop update block for a potentially fulfilling while loop by matching it to
   * the tightest natural loop that contains both the body entry and the loop condition.
   *
   * @param bodyEntryBlock the loop body entry block
   * @param conditionalBlock the loop conditional block
   * @param whileLoopCache cached CFG facts for while-loop resolution
   * @return the chosen loop update block, or {@code null} if none is found
   */
  private @Nullable Block chooseLoopUpdateBlockForPotentiallyFulfillingLoop(
      Block bodyEntryBlock,
      ConditionalBlock conditionalBlock,
      WhileLoopResolutionCache whileLoopCache) {

    Block bestLoopUpdateBlock = null;
    int bestLoopSize = Integer.MAX_VALUE;

    for (WhileLoopResolutionCache.BlockEdge backEdge : whileLoopCache.getBackEdges()) {
      Set<Block> naturalLoop = whileLoopCache.getNaturalLoopForBackEdge(backEdge);

      if (!naturalLoop.contains(bodyEntryBlock)) {
        continue;
      }
      if (!naturalLoop.contains(conditionalBlock)) {
        continue;
      }

      if (naturalLoop.size() < bestLoopSize) {
        bestLoopSize = naturalLoop.size();
        bestLoopUpdateBlock = backEdge.targetBlock;
      }
    }

    return bestLoopUpdateBlock;
  }

  /**
   * Returns the cached CFG facts for while-loop resolution, creating them lazily if needed.
   *
   * @return cached CFG facts for while-loop resolution
   */
  private WhileLoopResolutionCache getOrCreateWhileLoopCache() {
    if (whileLoopCache == null) {
      whileLoopCache = new WhileLoopResolutionCache(cfg);
    }
    return whileLoopCache;
  }
}
