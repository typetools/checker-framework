package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Matches {@code while} loops that iterate over elements of a resource collection and produce
 * {@link DisposalLoopInfo}.
 *
 * <p>Supported forms include iterator loops such as {@code while (it.hasNext())} and non-empty
 * collection loops such as {@code while (!queue.isEmpty())}, {@code while (queue.size() > 0)}, and
 * {@code while (0 < queue.size())}. A match requires exactly one extraction call in the loop body,
 * such as {@code it.next()}, {@code queue.poll()}, or {@code queue.removeFirst()}.
 */
final class WhileDisposalLoopMatcher {

  /**
   * Methods that may extract an element in an iterator-header loop like {@code while (it.hasNext())
   * { ... it.next() ... }}.
   */
  private static final Set<String> ITERATOR_EXTRACT_METHODS = Set.of("next");

  /**
   * Methods that may extract an element in a non-empty-collection loop such as {@code while
   * (!c.isEmpty()) { ... c.poll()/pop/removeFirst/... ...}}, including {@code size() > 0} and
   * {@code 0 < size()} variants.
   */
  private static final Set<String> NONEMPTY_EXTRACT_METHODS =
      Set.of("poll", "pollFirst", "pollLast", "remove", "removeFirst", "removeLast", "pop");

  /** The CO type factory used for collection-ownership queries. */
  private final CollectionOwnershipAnnotatedTypeFactory coAtf;

  /** The RLCC type factory used for declaration lookup. */
  private final RLCCalledMethodsAnnotatedTypeFactory rlccAtf;

  /** The CFG of the method currently being scanned. */
  private final ControlFlowGraph cfg;

  /** Lazily-computed CFG facts for while-loop resolution. */
  private @MonotonicNonNull WhileLoopResolutionCache whileLoopCache;

  /**
   * Creates a matcher for {@code while} disposal loops.
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

  /**
   * Returns the {@link DisposalLoopInfo} if the {@code while} loop {@code tree} matches one of the
   * supported header forms and has exactly one compatible extraction in its body.
   *
   * <p>For example:
   *
   * <pre>{@code
   * while (it.hasNext()) {
   *   Resource resource = it.next();
   *   resource.close();
   * }
   * }</pre>
   *
   * and:
   *
   * <pre>{@code
   * while (!queue.isEmpty()) {
   *   queue.removeFirst().close();
   * }
   * }</pre>
   *
   * @param tree the while-loop to inspect
   * @return the matched disposal loop info, or {@code null} if the loop does not match
   */
  @Nullable DisposalLoopInfo match(WhileLoopTree tree) {
    ExpressionTree conditionWithoutParens = TreeUtils.withoutParens(tree.getCondition());
    WhileHeaderInfo headerMatch = matchWhileHeader(conditionWithoutParens);
    if (headerMatch == null) {
      return null;
    }

    // Validate the loop body and recover the unique extraction call that represents the iterated
    // element for this loop.
    MethodInvocationTree extractionCall =
        new WhileBodyScanner(
                headerMatch.headerVarName,
                headerMatch.collectionVarNameForInvalidation,
                headerMatch.allowedExtractMethods)
            .scanLoopBody(tree.getStatement());
    if (extractionCall == null) {
      return null;
    }

    // After the tree match succeeds, recover the CFG blocks corresponding to the loop condition,
    // body entry, and loop back edge.
    Block conditionBlock = CollectionOwnershipUtils.firstBlockForTree(cfg, conditionWithoutParens);
    if (conditionBlock == null) {
      return null;
    }

    ConditionalBlock conditionalBlock = findImmediateConditionalSuccessor(conditionBlock);
    if (conditionalBlock == null) {
      // Some while-loop conditions are represented through exception blocks before reaching the
      // actual conditional branch. Retry from the peeled predecessor.
      Block peeledPredecessor = peelExceptionBlocksToPredecessor(conditionBlock);
      if (peeledPredecessor != null) {
        conditionalBlock = findImmediateConditionalSuccessor(peeledPredecessor);
      }
    }
    if (conditionalBlock == null) {
      return null;
    }

    Block loopBodyEntryBlock = conditionalBlock.getThenSuccessor();
    Node iteratedElementNode = CollectionOwnershipUtils.anyNodeForTree(cfg, extractionCall);
    if (iteratedElementNode == null) {
      return null;
    }

    Block loopUpdateBlock =
        chooseLoopUpdateBlock(loopBodyEntryBlock, conditionalBlock, getOrCreateWhileLoopCache());
    if (loopUpdateBlock == null) {
      return null;
    }

    return new DisposalLoopInfo(
        headerMatch.collectionTree,
        extractionCall,
        iteratedElementNode,
        CollectionOwnershipUtils.cfgAssociatedTreeFor(cfg, conditionWithoutParens),
        conditionalBlock,
        loopBodyEntryBlock,
        loopUpdateBlock);
  }

  /**
   * Matches supported {@code while} header forms and returns the recovered {@link WhileHeaderInfo}.
   *
   * <p>Supported header shapes are:
   *
   * <ul>
   *   <li>{@code it.hasNext()}
   *   <li>{@code !collection.isEmpty()}
   *   <li>{@code collection.size() > 0}
   *   <li>{@code 0 < collection.size()}
   * </ul>
   *
   * @param condition the while-loop condition with parentheses removed
   * @return the recovered header facts, or {@code null} if the header is unsupported
   */
  private @Nullable WhileHeaderInfo matchWhileHeader(ExpressionTree condition) {
    if (condition instanceof MethodInvocationTree methodInvocationTree) {
      return matchIteratorHeaderInfo(methodInvocationTree);
    }

    if (condition instanceof UnaryTree unaryTree
        && condition.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
      ExpressionTree inner = TreeUtils.withoutParens(unaryTree.getExpression());
      WhileHeaderInfo headerMatch = matchNonEmptyFromExpr(inner);
      if (headerMatch != null) {
        return headerMatch;
      }
    }

    if (condition instanceof BinaryTree binaryTree) {
      return matchNonEmptyFromSize(binaryTree);
    }

    return null;
  }

  /**
   * Matches an iterator header of the form {@code while (it.hasNext())} and extracts corresponding
   * {@link WhileHeaderInfo}.
   *
   * @param invocation the candidate header invocation
   * @return the recovered header facts, or {@code null} if the header does not match
   */
  private @Nullable WhileHeaderInfo matchIteratorHeaderInfo(MethodInvocationTree invocation) {
    if (!TreeUtils.isHasNextCall(invocation)) {
      return null;
    }
    ExpressionTree receiver = receiverOfInvocation(invocation);
    if (receiver == null) {
      return null;
    }

    Name iteratorVarName = CollectionOwnershipUtils.getNameFromExpressionTree(receiver);
    if (iteratorVarName == null) {
      return null;
    }

    ExpressionTree collectionTree = recoverCollectionFromIteratorReceiver(receiver);
    if (collectionTree == null) {
      return null;
    }

    Name collectionVarName = CollectionOwnershipUtils.getNameFromExpressionTree(collectionTree);
    return new WhileHeaderInfo(
        collectionTree, collectionVarName, iteratorVarName, ITERATOR_EXTRACT_METHODS);
  }

  /**
   * Matches a non-empty {@code while} condition of the form {@code !collection.isEmpty()} and
   * extracts corresponding {@link WhileHeaderInfo}.
   *
   * @param expression the expression under the logical complement
   * @return the recovered header facts, or {@code null} if the expression does not match
   */
  private @Nullable WhileHeaderInfo matchNonEmptyFromExpr(ExpressionTree expression) {
    if (!(expression instanceof MethodInvocationTree methodInvocationTree)) {
      return null;
    }
    if (!isIsEmptyCall(methodInvocationTree)) {
      return null;
    }

    ExpressionTree receiver = receiverOfInvocation(methodInvocationTree);
    if (receiver == null) {
      return null;
    }
    return nonEmptyHeaderMatch(receiver);
  }

  /**
   * Matches a non-empty collection condition of the form {@code collection.size() > 0} or {@code 0
   * < collection.size()} and returns corresponding {@link WhileHeaderInfo}.
   *
   * @param condition the binary condition
   * @return the recovered header facts, or {@code null} if the expression does not match
   */
  private @Nullable WhileHeaderInfo matchNonEmptyFromSize(BinaryTree condition) {
    Tree.Kind kind = condition.getKind();
    if (kind != Tree.Kind.GREATER_THAN && kind != Tree.Kind.LESS_THAN) {
      return null;
    }

    ExpressionTree left = TreeUtils.withoutParens(condition.getLeftOperand());
    ExpressionTree right = TreeUtils.withoutParens(condition.getRightOperand());

    @Nullable MethodInvocationTree sizeCall = null;
    @Nullable LiteralTree zeroLiteral = null;

    if (kind == Tree.Kind.GREATER_THAN) {
      if (left instanceof MethodInvocationTree leftInvocation
          && right instanceof LiteralTree rightLiteral) {
        sizeCall = leftInvocation;
        zeroLiteral = rightLiteral;
      }
    } else {
      if (left instanceof LiteralTree leftLiteral
          && right instanceof MethodInvocationTree rightInvocation) {
        sizeCall = rightInvocation;
        zeroLiteral = leftLiteral;
      }
    }

    if (sizeCall == null) {
      return null;
    }

    Object zeroValue = zeroLiteral.getValue();
    if (!(zeroValue instanceof Integer intZeroVal) || intZeroVal != 0) {
      return null;
    }
    if (!TreeUtils.isSizeAccess(sizeCall)) {
      return null;
    }

    ExpressionTree receiver = receiverOfInvocation(sizeCall);
    if (receiver == null) {
      return null;
    }
    return nonEmptyHeaderMatch(receiver);
  }

  /**
   * Builds a {@link WhileHeaderInfo} once {@link #matchNonEmptyFromExpr(ExpressionTree)} or {@link
   * #matchNonEmptyFromSize(BinaryTree)} has already matched a non-empty collection header condition
   * such as {@code !c.isEmpty()}, {@code c.size() > 0}, or {@code 0 < c.size()}.
   *
   * @param receiver the receiver checked by the non-empty header
   * @return the recovered header facts, or {@code null} if the receiver is not a resource
   *     collection
   */
  private @Nullable WhileHeaderInfo nonEmptyHeaderMatch(ExpressionTree receiver) {
    Name collectionVarName = CollectionOwnershipUtils.getNameFromExpressionTree(receiver);
    if (collectionVarName == null) {
      return null;
    }
    if (!coAtf.isResourceCollection(receiver)) {
      return null;
    }

    ExpressionTree collectionTree = CollectionOwnershipUtils.referenceExpression(receiver);
    if (collectionTree == null) {
      return null;
    }

    return new WhileHeaderInfo(
        collectionTree, collectionVarName, collectionVarName, NONEMPTY_EXTRACT_METHODS);
  }

  /**
   * Returns whether the given invocation is an {@code isEmpty()} call with no arguments.
   *
   * @param invocation a method invocation
   * @return true if {@code invocation} is an {@code isEmpty()} call with no arguments
   */
  private boolean isIsEmptyCall(MethodInvocationTree invocation) {
    ExpressionTree methodSelect = invocation.getMethodSelect();
    if (!(methodSelect instanceof MemberSelectTree memberSelectTree)) {
      return false;
    }
    return memberSelectTree.getIdentifier().contentEquals("isEmpty")
        && invocation.getArguments().isEmpty();
  }

  /**
   * Returns the explicit receiver of the given invocation, if present.
   *
   * @param invocation a method invocation
   * @return the explicit receiver, or {@code null} if none exists
   */
  private @Nullable ExpressionTree receiverOfInvocation(MethodInvocationTree invocation) {
    ExpressionTree methodSelect = invocation.getMethodSelect();
    if (methodSelect instanceof MemberSelectTree memberSelectTree) {
      return memberSelectTree.getExpression();
    }
    return null;
  }

  /**
   * Recovers the collection expression from an iterator receiver in a header such as {@code while
   * (it.hasNext())}.
   *
   * <p>This only recognizes local iterator variables initialized directly by {@code
   * collection.iterator()}.
   *
   * @param iteratorReceiver the iterator receiver expression
   * @return the collection expression, or {@code null} if it cannot be recovered
   */
  private @Nullable ExpressionTree recoverCollectionFromIteratorReceiver(
      ExpressionTree iteratorReceiver) {
    Element iteratorElement = TreeUtils.elementFromTree(iteratorReceiver);
    if (!(iteratorElement instanceof VariableElement)) {
      return null;
    }
    if (iteratorElement.getKind() != ElementKind.LOCAL_VARIABLE) {
      return null;
    }

    Tree declaration = rlccAtf.declarationFromElement(iteratorElement);
    if (!(declaration instanceof VariableTree variableTreeDeclaration)) {
      return null;
    }

    ExpressionTree initializer = variableTreeDeclaration.getInitializer();
    if (!(initializer instanceof MethodInvocationTree initializerCall)) {
      return null;
    }

    ExpressionTree methodSelect = initializerCall.getMethodSelect();
    if (!(methodSelect instanceof MemberSelectTree memberSelectTree)) {
      return null;
    }
    if (!memberSelectTree.getIdentifier().contentEquals("iterator")
        || !initializerCall.getArguments().isEmpty()) {
      return null;
    }

    ExpressionTree collectionExpression = memberSelectTree.getExpression();
    if (!coAtf.isResourceCollection(collectionExpression)) {
      return null;
    }
    return CollectionOwnershipUtils.referenceExpression(collectionExpression);
  }

  /**
   * Returns whether the given invocation is an allowed extraction call on the matched header
   * variable.
   *
   * @param invocation a method invocation
   * @param headerVarName the iterator or collection variable constrained by the header
   * @param allowedExtractMethods extraction methods permitted by the matched header form
   * @return true if {@code invocation} is an allowed extraction call on {@code headerVarName}
   */
  private boolean isExtractionCallOnHeaderVar(
      MethodInvocationTree invocation, Name headerVarName, Set<String> allowedExtractMethods) {
    ExpressionTree methodSelect = invocation.getMethodSelect();
    if (!(methodSelect instanceof MemberSelectTree memberSelectTree)) {
      return false;
    }

    String methodName = memberSelectTree.getIdentifier().toString();
    if (!allowedExtractMethods.contains(methodName)) {
      return false;
    }
    if (!invocation.getArguments().isEmpty()) {
      return false;
    }

    Name receiverName =
        CollectionOwnershipUtils.getNameFromExpressionTree(memberSelectTree.getExpression());
    return receiverName != null && receiverName == headerVarName;
  }

  /**
   * Scans the body of a matched {@code while} loop and checks whether the body is consistent with
   * the loop header.
   *
   * <p>The body is accepted only if it:
   *
   * <ul>
   *   <li>does not overwrite the header variable or the matched collection variable
   *   <li>contains exactly one allowed extraction call on the header/collection variable
   * </ul>
   */
  private final class WhileBodyScanner extends TreeScanner<Void, Void> {

    /** Iterator or collection variable constrained by the loop header. */
    private final Name headerVarName;

    /** Collection variable whose writes should invalidate the match, if one exists. */
    private final @Nullable Name collectionVarNameForInvalidation;

    /** Extraction methods allowed by the matched header form. */
    private final Set<String> allowedExtractMethods;

    /**
     * Whether the loop body has been rejected. The loop is rejected if it overwrites the
     * header/collection variable, or does zero/more than one extraction call.
     */
    private boolean illegal = false;

    /** Number of extraction calls found so far. */
    private int extractionCount = 0;

    /** The unique extraction call, if one has been found so far. */
    private @Nullable MethodInvocationTree extractionCall = null;

    /**
     * Creates a {@link WhileBodyScanner}.
     *
     * @param headerVarName iterator or collection variable constrained by the header
     * @param collectionVarNameForInvalidation collection variable whose writes invalidate the match
     * @param allowedExtractMethods extraction methods allowed by the matched header form
     */
    private WhileBodyScanner(
        Name headerVarName,
        @Nullable Name collectionVarNameForInvalidation,
        Set<String> allowedExtractMethods) {
      this.headerVarName = headerVarName;
      this.collectionVarNameForInvalidation = collectionVarNameForInvalidation;
      this.allowedExtractMethods = allowedExtractMethods;
    }

    /**
     * Scans the loop body once and returns the unique extraction call as the iterated element.
     *
     * @param loopBody the loop body to scan
     * @return the unique allowed extraction call, or {@code null} if the body writes to the header
     *     variable or matched collection variable, contains no extraction, or contains more than
     *     one extraction
     */
    private @Nullable MethodInvocationTree scanLoopBody(StatementTree loopBody) {
      super.scan(loopBody, null);
      if (illegal || extractionCount != 1) {
        return null;
      }
      return extractionCall;
    }

    /**
     * Rejects a loop body that writes to the variable constrained by the header/collection
     * variable.
     *
     * @param lhs the assignment target
     */
    private void markWriteIfTargetsHeaderOrCollection(ExpressionTree lhs) {
      Name assignedVariable = CollectionOwnershipUtils.getNameFromExpressionTree(lhs);
      if (assignedVariable == null) {
        return;
      }
      if (assignedVariable == headerVarName) {
        illegal = true;
      }
      if (collectionVarNameForInvalidation != null
          && assignedVariable == collectionVarNameForInvalidation) {
        illegal = true;
      }
    }

    /**
     * Records one allowed extraction call found in the given expression.
     *
     * @param expression the candidate extraction expression
     */
    private void recordExtractionIfAny(ExpressionTree expression) {
      ExpressionTree expressionWithoutParens = TreeUtils.withoutParens(expression);
      if (!(expressionWithoutParens instanceof MethodInvocationTree methodInvocationTree)) {
        return;
      }
      if (!isExtractionCallOnHeaderVar(
          methodInvocationTree, headerVarName, allowedExtractMethods)) {
        return;
      }

      extractionCount++;
      if (extractionCount > 1) {
        // More than one extraction means this iteration can advance through more than one element,
        // so the loop no longer corresponds to a single iterated element.
        illegal = true;
        return;
      }
      extractionCall = methodInvocationTree;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
      markWriteIfTargetsHeaderOrCollection(tree.getVariable());
      if (illegal) {
        return null;
      }
      return super.visitCompoundAssignment(tree, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void p) {
      // Reassigning the header/collection variable breaks the link between the loop header and the
      // extraction expected in the body.
      markWriteIfTargetsHeaderOrCollection(tree.getVariable());
      if (illegal) {
        return null;
      }
      recordExtractionIfAny(tree.getExpression());
      return super.visitAssignment(tree, p);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void p) {
      ExpressionTree initializer = tree.getInitializer();
      if (initializer != null) {
        recordExtractionIfAny(initializer);
      }
      return super.visitVariable(tree, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
      ExpressionTree receiver = receiverOfInvocation(tree);
      if (receiver != null) {
        // For chained calls such as `it.next().close()` or `queue.poll().close()`, the extraction
        // call appears as the receiver of an outer invocation.
        recordExtractionIfAny(receiver);
      }
      return super.visitMethodInvocation(tree, p);
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void p) {
      switch (tree.getKind()) {
        case PREFIX_DECREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, POSTFIX_INCREMENT -> {
          Name mutatedVariable =
              CollectionOwnershipUtils.getNameFromExpressionTree(tree.getExpression());
          if (mutatedVariable == headerVarName) {
            illegal = true;
            return null;
          }
        }
        default -> {}
      }
      return super.visitUnary(tree, p);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
      // A lambda body is not executed as part of the enclosing loop body.
      return null;
    }

    @Override
    public Void visitClass(ClassTree tree, Void p) {
      // Skip local and anonymous class bodies for the same reason as lambdas.
      return null;
    }

    @Override
    public @Nullable Void scan(@Nullable Tree tree, Void p) {
      // Short-circuit if the body has been rejected.
      if (illegal || tree == null) {
        return null;
      }
      return super.scan(tree, p);
    }

    @Override
    public @Nullable Void scan(@Nullable Iterable<? extends Tree> trees, Void p) {
      if (illegal || trees == null) {
        return null;
      }
      return super.scan(trees, p);
    }
  }

  /**
   * Returns the conditional successor reached immediately from the given block, if one is visible.
   *
   * @param block a CFG block
   * @return the immediate conditional successor of {@code block}, or {@code null} if none is found
   */
  private @Nullable ConditionalBlock findImmediateConditionalSuccessor(Block block) {
    for (Block successor : block.getSuccessors()) {
      if (successor instanceof ConditionalBlock conditionalBlock) {
        return conditionalBlock;
      }
    }
    return null;
  }

  /**
   * Walks backward through exception blocks amd returns the predecessor block that is not an {@link
   * ExceptionBlock}.
   *
   * @param block a CFG block
   * @return a predecessor block to retry from, or {@code null} if no such block is found
   */
  private @Nullable Block peelExceptionBlocksToPredecessor(Block block) {
    Block currentBlock = block;
    Set<Block> visitedBlocks = new HashSet<>();
    while (currentBlock instanceof ExceptionBlock && visitedBlocks.add(currentBlock)) {
      Set<Block> predecessors = currentBlock.getPredecessors();
      if (predecessors.size() != 1) {
        break;
      }
      Block predecessor = predecessors.iterator().next();
      if (predecessor == null) {
        break;
      }
      currentBlock = predecessor;
    }
    return currentBlock;
  }

  /**
   * Chooses the loop-update block for a matched while loop by selecting the smallest natural loop
   * that contains both the body entry and the loop conditional.
   *
   * @param bodyEntryBlock the loop body entry block
   * @param conditionalBlock the loop conditional block
   * @param whileLoopCache cached CFG facts for while-loop resolution
   * @return the chosen loop-update block, or {@code null} if none is found
   */
  private @Nullable Block chooseLoopUpdateBlock(
      Block bodyEntryBlock,
      ConditionalBlock conditionalBlock,
      WhileLoopResolutionCache whileLoopCache) {
    Block bestLoopUpdateBlock = null;
    int bestLoopSize = Integer.MAX_VALUE;

    for (WhileLoopResolutionCache.BackEdge backEdge : whileLoopCache.getBackEdges()) {
      Set<Block> naturalLoop = whileLoopCache.getNaturalLoopForBackEdge(backEdge);

      if (!naturalLoop.contains(bodyEntryBlock)) {
        continue;
      }
      if (!naturalLoop.contains(conditionalBlock)) {
        continue;
      }

      // Prefer the smallest natural loop containing both the body entry and the conditional block,
      // which most closely matches the while loop being resolved.
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

  /**
   * Facts recovered from a matched {@code while} loop header.
   *
   * @param collectionTree collection expression whose element obligations may be discharged.
   * @param collectionVarNameForInvalidation collection variable whose writes should invalidate the
   *     match, if one exists.
   * @param headerVarName iterator variable or collection variable constrained by the loop header.
   * @param allowedExtractMethods extraction methods accepted for this matched header.
   */
  private record WhileHeaderInfo(
      ExpressionTree collectionTree,
      @Nullable Name collectionVarNameForInvalidation,
      Name headerVarName,
      Set<String> allowedExtractMethods) {}

  /** Lazily-computed CFG facts used to resolve matched while loops. */
  private static final class WhileLoopResolutionCache {

    /**
     * A back edge from {@code sourceBlock} to {@code targetBlock}.
     *
     * @param sourceBlock Source block of the back edge.
     * @param targetBlock Target block of the back edge.
     */
    private record BackEdge(Block sourceBlock, Block targetBlock) {}

    /** Reachable CFG blocks in the current method. */
    private final Set<Block> reachableBlocks;

    /** Back edges among {@link #reachableBlocks}. */
    private final List<BackEdge> backEdges;

    /** Natural loops for back edges, computed lazily. */
    private final IdentityHashMap<BackEdge, Set<Block>> naturalLoopsByBackEdge =
        new IdentityHashMap<>();

    /**
     * Creates CFG facts for resolving matched while loops in the given CFG.
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
    private List<BackEdge> getBackEdges() {
      return backEdges;
    }

    /**
     * Returns the natural loop induced by the given back edge, computing it lazily if needed.
     *
     * @param backEdge the back edge
     * @return the natural loop induced by the given back edge
     */
    private Set<Block> getNaturalLoopForBackEdge(BackEdge backEdge) {
      return naturalLoopsByBackEdge.computeIfAbsent(
          backEdge,
          ignored -> naturalLoop(backEdge.sourceBlock, backEdge.targetBlock, reachableBlocks));
    }

    /**
     * Computes dominators for the reachable blocks in the current CFG using the standard iterative
     * dataflow equations:
     *
     * <pre>
     *  Dom(entry) = {entry}
     *  Dom(n) = {n} ∪ ⋂ Dom(p)    for each other reachable block n
     * </pre>
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
          dominators.put(block, new HashSet<>(Set.of(entryBlock)));
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
     * Returns the back edges among the reachable blocks in the current CFG. The edge A -> B is a
     * back edge if B dominates A.
     *
     * @param reachableBlocks reachable blocks in the CFG
     * @param dominators dominators for each reachable block
     * @return the CFG back edges
     */
    private static List<BackEdge> findBackEdges(
        Set<Block> reachableBlocks, Map<Block, Set<Block>> dominators) {
      List<BackEdge> backEdges = new ArrayList<>();
      for (Block sourceBlock : reachableBlocks) {
        for (Block targetBlock : sourceBlock.getSuccessors()) {
          if (targetBlock == null || !reachableBlocks.contains(targetBlock)) {
            continue;
          }
          Set<Block> sourceDominators = dominators.get(sourceBlock);
          if (sourceDominators != null && sourceDominators.contains(targetBlock)) {
            backEdges.add(new BackEdge(sourceBlock, targetBlock));
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
}
