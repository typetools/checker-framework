package org.checkerframework.checker.rlccalledmethods;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.calledmethods.EnsuresCalledMethodOnExceptionContract;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsBottom;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.CreatesMustCallForElementSupplier;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis.FieldInitialValue;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.plumelib.util.IPair;

/**
 * The type factory for the RLCCalledMethodsChecker. The main difference between this and the Called
 * Methods type factory from which it is derived is that this version's {@link
 * #postAnalyze(ControlFlowGraph)} method checks that must-call obligations are fulfilled.
 */
public class RLCCalledMethodsAnnotatedTypeFactory extends CalledMethodsAnnotatedTypeFactory
    implements CreatesMustCallForElementSupplier {

  /** The RLC parent checker. */
  private ResourceLeakChecker rlc;

  /** The MustCall.value element/field. */
  private final ExecutableElement mustCallValueElement =
      TreeUtils.getMethod(MustCall.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.value element/field. */
  public final ExecutableElement ensuresCalledMethodsValueElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.methods element/field. */
  public final ExecutableElement ensuresCalledMethodsMethodsElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "methods", 0, processingEnv);

  /** The EnsuresCalledMethods.List.value element/field. */
  private final ExecutableElement ensuresCalledMethodsListValueElement =
      TreeUtils.getMethod(EnsuresCalledMethods.List.class, "value", 0, processingEnv);

  /** The CreatesMustCallFor.List.value element/field. */
  private final ExecutableElement createsMustCallForListValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.List.class, "value", 0, processingEnv);

  /** The CreatesMustCallFor.value element/field. */
  private final ExecutableElement createsMustCallForValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.class, "value", 0, processingEnv);

  /** True if -AnoResourceAliases was passed on the command line. */
  private final boolean noResourceAliases;

  /**
   * Bidirectional map to store temporary variables created for expressions with non-empty @MustCall
   * obligations and the corresponding trees. Keys are the artificial local variable nodes created
   * as temporary variables; values are the corresponding trees.
   *
   * <p>Note that in an ideal world, this would be an {@code IdentityBiMap}: that is, a BiMap using
   * {@link java.util.IdentityHashMap} as both of the backing maps. However, Guava doesn't have such
   * a map AND their implementation is incompatible with IdentityHashMap as a backing map, because
   * even their {@code AbstractBiMap} class uses {@code equals} calls in its implementation (and its
   * documentation calls out that it and all its derived BiMaps are incompatible with
   * IdentityHashMap as a backing map for this reason). Therefore, we use a regular BiMap. Doing so
   * is safe iff 1) the LocalVariableNode keys all have different names, and 2) a standard Tree
   * implementation that uses reference equality for equality (e.g., JCTree in javac) is used.
   */
  private final BiMap<LocalVariableNode, Tree> tempVarToTree = HashBiMap.create();

  /**
   * Per-method loop state for collection-obligation loops that have been matched syntactically by
   * the MustCall visitor.
   */
  private static final class MethodCollectionLoopState {

    /** Enhanced-for-loops that have been matched syntactically but still need CFG resolution. */
    final Set<EnhancedForLoopTree> potentiallyFulfillingEnhancedForLoops = new LinkedHashSet<>();

    /**
     * Collection-obligation loops that have been matched syntactically but still need CFG-local
     * resolution before the consistency analyzer can verify them.
     */
    final Set<PotentiallyFulfillingCollectionLoop> potentiallyFulfillingCollectionLoops =
        new LinkedHashSet<>();

    /**
     * Potentially fulfilling collection loops that have all CFG information required by the
     * consistency analyzer.
     */
    final Set<ResolvedPotentiallyFulfillingCollectionLoop>
        resolvedPotentiallyFulfillingCollectionLoops = new LinkedHashSet<>();

    /** Lazily-computed CFG facts used to resolve potentially fulfilling while loops. */
    private @Nullable WhileLoopResolutionCache whileLoopCache;

    /**
     * Returns CFG facts for resolving potentially fulfilling while loops in this method, creating
     * them lazily if needed.
     *
     * @param cfg the enclosing method CFG
     * @return the CFG facts for resolving potentially fulfilling while loops in this method
     */
    private WhileLoopResolutionCache getOrCreateWhileLoopCache(ControlFlowGraph cfg) {
      if (whileLoopCache == null) {
        whileLoopCache = new WhileLoopResolutionCache(cfg);
      }
      return whileLoopCache;
    }
  }

  /** Lazily-computed CFG facts used to resolve potentially fulfilling while loops. */
  private static final class WhileLoopResolutionCache {

    /** A back edge in the CFG. */
    private static final class BlockEdge {
      final Block sourceBlock;
      final Block targetBlock;

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
      this.reachableBlocks = reachableFrom(entryBlock);
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
     * Returns blocks reachable from {@code entryBlock}.
     *
     * @param entryBlock the CFG entry block
     * @return the reachable blocks
     */
    private static Set<Block> reachableFrom(Block entryBlock) {
      Set<Block> seen = new HashSet<>();
      ArrayDeque<Block> queue = new ArrayDeque<>();
      queue.add(entryBlock);
      seen.add(entryBlock);

      while (!queue.isEmpty()) {
        Block block = queue.remove();
        for (Block successor : block.getSuccessors()) {
          if (successor != null && seen.add(successor)) {
            queue.add(successor);
          }
        }
      }
      return seen;
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
        if (block == entryBlock) {
          dominators.put(block, new HashSet<>(Collections.singleton(entryBlock)));
        } else {
          dominators.put(block, new HashSet<>(reachableBlocks)); // TOP
        }
      }

      boolean changed;
      do {
        changed = false;
        for (Block block : reachableBlocks) {
          if (block == entryBlock) {
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
      List<BlockEdge> backEdges = new ArrayList<>();
      for (Block sourceBlock : reachableBlocks) {
        for (Block targetBlock : sourceBlock.getSuccessors()) {
          if (targetBlock == null || !reachableBlocks.contains(targetBlock)) {
            continue;
          }
          Set<Block> sourceDominators = dominators.get(sourceBlock);
          if (sourceDominators != null && sourceDominators.contains(targetBlock)) {
            // targetBlock dominates sourceBlock, so sourceBlock -> targetBlock is a back edge.
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
          if (loopBlocks.add(predecessor) && predecessor != targetBlock) {
            stack.push(predecessor);
          }
        }
      }
      return loopBlocks;
    }
  }

  /** Per-method collection-loop state accumulated during MustCall visitor matching. */
  private final IdentityHashMap<MethodTree, MethodCollectionLoopState>
      collectionLoopStateByEnclosingMethod = new IdentityHashMap<>();

  /**
   * Returns the loop state for the given method, creating it if needed.
   *
   * @param enclosingMethodTree the enclosing method
   * @return the loop state for the given method
   */
  private MethodCollectionLoopState getOrCreateMethodCollectionLoopState(
      MethodTree enclosingMethodTree) {
    return collectionLoopStateByEnclosingMethod.computeIfAbsent(
        enclosingMethodTree, ignored -> new MethodCollectionLoopState());
  }

  /**
   * Returns the loop state for the given underlying AST, or {@code null} if there is none.
   *
   * @param underlyingAST the current underlying AST
   * @return the loop state for the given underlying AST, or {@code null}
   */
  private @Nullable MethodCollectionLoopState getMethodCollectionLoopState(
      UnderlyingAST underlyingAST) {
    MethodTree enclosingMethodTree = getEnclosingMethodTree(underlyingAST);
    if (enclosingMethodTree == null) {
      return null;
    }
    return collectionLoopStateByEnclosingMethod.get(enclosingMethodTree);
  }

  /**
   * Returns the potentially fulfilling collection loops for the method represented by the given
   * underlying AST.
   *
   * @param underlyingAST the current underlying AST
   * @return the potentially fulfilling collection loops for the method represented by the given
   *     underlying AST
   */
  Set<PotentiallyFulfillingCollectionLoop> getPotentiallyFulfillingCollectionLoops(
      UnderlyingAST underlyingAST) {
    MethodCollectionLoopState loopState = getMethodCollectionLoopState(underlyingAST);
    if (loopState == null) {
      return Collections.emptySet();
    }
    return loopState.potentiallyFulfillingCollectionLoops;
  }

  /**
   * Returns the resolved potentially fulfilling collection loops for the method represented by the
   * given underlying AST.
   *
   * @param underlyingAST the current underlying AST
   * @return the resolved potentially fulfilling collection loops for the method represented by the
   *     given underlying AST
   */
  Set<ResolvedPotentiallyFulfillingCollectionLoop> getResolvedPotentiallyFulfillingCollectionLoops(
      UnderlyingAST underlyingAST) {
    MethodCollectionLoopState loopState = getMethodCollectionLoopState(underlyingAST);
    if (loopState == null) {
      return Collections.emptySet();
    }
    return loopState.resolvedPotentiallyFulfillingCollectionLoops;
  }

  /**
   * Removes the loop state associated with the given underlying AST.
   *
   * @param underlyingAST the current underlying AST
   */
  private void removeMethodCollectionLoopState(UnderlyingAST underlyingAST) {
    MethodTree enclosingMethodTree = getEnclosingMethodTree(underlyingAST);
    if (enclosingMethodTree != null) {
      collectionLoopStateByEnclosingMethod.remove(enclosingMethodTree);
    }
  }

  /**
   * Returns the enclosing method for the given underlying AST, or {@code null} if the underlying
   * AST is not a method.
   *
   * @param underlyingAST the current underlying AST
   * @return the enclosing method for the given underlying AST, or {@code null}
   */
  private @Nullable MethodTree getEnclosingMethodTree(UnderlyingAST underlyingAST) {
    if (underlyingAST.getKind() != UnderlyingAST.Kind.METHOD) {
      return null;
    }
    return ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
  }

  /**
   * Creates a new RLCCalledMethodsAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  @SuppressWarnings("this-escape")
  public RLCCalledMethodsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.rlc = ResourceLeakUtils.getResourceLeakChecker(checker);
    this.noResourceAliases = rlc.hasOption(MustCallChecker.NO_RESOURCE_ALIASES);
    if (this.getClass() == RLCCalledMethodsAnnotatedTypeFactory.class) {
      this.postInit();
    }
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return getBundledTypeQualifiers(
        // These annotations are in the Called Methods Checker, not the Resource Leak Checker.
        CalledMethods.class, CalledMethodsBottom.class, CalledMethodsPredicate.class);
  }

  /**
   * Creates a @CalledMethods annotation whose values are the given strings.
   *
   * @param val the methods that have been called
   * @return an annotation indicating that the given methods have been called
   */
  public AnnotationMirror createCalledMethods(String... val) {
    return createAccumulatorAnnotation(Arrays.asList(val));
  }

  @Override
  protected ControlFlowGraph analyze(
      Queue<IPair<ClassTree, @Nullable AccumulationStore>> classQueue,
      Queue<IPair<LambdaExpressionTree, @Nullable AccumulationStore>> lambdaQueue,
      UnderlyingAST ast,
      List<FieldInitialValue<AccumulationValue>> fieldValues,
      @Nullable ControlFlowGraph cfg,
      boolean isInitializationCode,
      boolean updateInitializationStore,
      boolean isStatic,
      @Nullable AccumulationStore capturedStore) {
    if (cfg != null && ast.getKind() == UnderlyingAST.Kind.METHOD) {
      // The old RLCC workaround for RLLambda.java (#7316) used to run in analyze(). It was moved
      // to CollectionOwnershipAnnotatedTypeFactory.postAnalyzeAfterFirstMethodAnalysis(...) so it
      // executes once at the correct lifecycle point: after the first method analysis and before
      // lambda fixpoint.
      //
      // At this point cfg is the preserved first method analysis result. Keep that result, but
      // re-enqueue nested classes and lambdas so later fixpoint iterations still analyze them.
      // Returning cfg without re-enqueuing would incorrectly stop lambda processing; recomputing
      // the method analysis would discard the preserved first-pass result that the early
      // resource-leak post-analysis depends on.
      for (ClassTree cls : cfg.getDeclaredClasses()) {
        classQueue.add(IPair.of(cls, getStoreBefore(cls)));
      }
      for (LambdaExpressionTree lambda : cfg.getDeclaredLambdas()) {
        lambdaQueue.add(IPair.of(lambda, getStoreBefore(lambda)));
      }
      return cfg;
    }
    return super.analyze(
        classQueue,
        lambdaQueue,
        ast,
        fieldValues,
        cfg,
        isInitializationCode,
        updateInitializationStore,
        isStatic,
        capturedStore);
  }

  public void recordPotentiallyFulfillingCollectionLoop(
      MethodTree enclosingMethodTree,
      ExpressionTree collectionTree,
      Tree collectionElementTree,
      Tree conditionTree,
      Block loopBodyEntryBlock,
      ConditionalBlock loopConditionalBlock,
      Node collectionElementNode) {
    getOrCreateMethodCollectionLoopState(enclosingMethodTree)
        .potentiallyFulfillingCollectionLoops
        .add(
            new PotentiallyFulfillingCollectionLoop(
                collectionTree,
                collectionElementTree,
                conditionTree,
                loopBodyEntryBlock,
                loopConditionalBlock,
                collectionElementNode));
  }

  public void recordPotentiallyFulfillingEnhancedForLoop(
      MethodTree enclosingMethodTree, EnhancedForLoopTree enhancedForLoopTree) {
    getOrCreateMethodCollectionLoopState(enclosingMethodTree)
        .potentiallyFulfillingEnhancedForLoops
        .add(enhancedForLoopTree);
  }

  public void recordResolvedPotentiallyFulfillingCollectionLoop(
      MethodTree enclosingMethodTree,
      ExpressionTree collectionTree,
      Tree collectionElementTree,
      Tree conditionTree,
      Block loopBodyEntryBlock,
      Block loopUpdateBlock,
      ConditionalBlock loopConditionalBlock,
      Node collectionElementNode) {
    getOrCreateMethodCollectionLoopState(enclosingMethodTree)
        .resolvedPotentiallyFulfillingCollectionLoops
        .add(
            new ResolvedPotentiallyFulfillingCollectionLoop(
                collectionTree,
                collectionElementTree,
                conditionTree,
                loopBodyEntryBlock,
                loopUpdateBlock,
                loopConditionalBlock,
                collectionElementNode));
  }

  @Override
  protected RLCCalledMethodsAnalysis createFlowAnalysis() {
    return new RLCCalledMethodsAnalysis((RLCCalledMethodsChecker) checker, this);
  }

  /**
   * Retrieves the {@code @MustCall} annotation for the given object, which can be either an {@link
   * Element} or a {@link Tree}. This method delegates to the {@code MustCallAnnotatedTypeFactory}
   * to get the annotated type of the input object and then extracts the primary {@code @MustCall}
   * annotation from it.
   *
   * @param obj the object for which to retrieve the {@code @MustCall} annotation. Must be either an
   *     instance of {@link Element} or {@link Tree}.
   * @return the {@code @MustCall} annotation if present, null otherwise
   * @throws IllegalArgumentException if the input object type is not supported
   */
  public AnnotationMirror getMustCallAnnotation(Object obj) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType;
    if (obj instanceof Element) {
      mustCallAnnotatedType = mustCallAnnotatedTypeFactory.getAnnotatedType((Element) obj);
    } else if (obj instanceof Tree) {
      mustCallAnnotatedType = mustCallAnnotatedTypeFactory.getAnnotatedType((Tree) obj);
    } else {
      throw new IllegalArgumentException("Unsupported type: " + obj.getClass().getName());
    }
    return mustCallAnnotatedType.getPrimaryAnnotation(MustCall.class);
  }

  /**
   * Returns true if the {@link MustCall#value} element/argument of the @MustCall annotation on the
   * type of {@code tree} is definitely empty.
   *
   * <p>This method only considers the declared type: it does not consider flow-sensitive
   * refinement.
   *
   * @param tree a tree
   * @return true if the Must Call type is non-empty or top
   */
  public boolean hasEmptyMustCallValue(Tree tree) {
    AnnotationMirror mustCallAnnotation = getMustCallAnnotation(tree);
    if (mustCallAnnotation != null) {
      return getMustCallValues(mustCallAnnotation).isEmpty();
    } else {
      // Indicates @MustCallUnknown, which should be treated (conservatively) as if it
      // contains some must call values.
      return false;
    }
  }

  /**
   * Returns true if the {@link MustCall#value} element/argument of the @MustCall annotation on the
   * type of {@code element} is definitely empty.
   *
   * <p>This method only considers the declared type: it does not consider flow-sensitive
   * refinement.
   *
   * @param element an element
   * @return true if the Must Call type is non-empty or top
   */
  public boolean hasEmptyMustCallValue(Element element) {
    AnnotationMirror mustCallAnnotation = getMustCallAnnotation(element);
    if (mustCallAnnotation != null) {
      return getMustCallValues(mustCallAnnotation).isEmpty();
    } else {
      // Indicates @MustCallUnknown, which should be treated (conservatively) as if it
      // contains some must call values.
      return false;
    }
  }

  /**
   * Returns the {@link MustCall#value} element/argument of the @MustCall annotation on the class
   * type of {@code element}. If there is no such annotation, returns the empty list.
   *
   * <p>Do not use this method to get the MustCall values of an {@link
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation}. Instead, use
   * {@link
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation#getMustCallMethods(RLCCalledMethodsAnnotatedTypeFactory,
   * CFStore)}.
   *
   * <p>Do not call {@link List#isEmpty()} on the result of this method: prefer to call {@link
   * #hasEmptyMustCallValue(Element)}, which correctly accounts for @MustCallUnknown, instead.
   *
   * @param element an element
   * @return the strings in its must-call type
   */
  public List<String> getMustCallValues(Element element) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType =
        mustCallAnnotatedTypeFactory.getAnnotatedType(element);
    AnnotationMirror mustCallAnnotation =
        mustCallAnnotatedType.getPrimaryAnnotation(MustCall.class);
    return getMustCallValues(mustCallAnnotation);
  }

  /**
   * Helper method for getting the must-call values from a must-call annotation.
   *
   * @param mustCallAnnotation a {@link MustCall} annotation, or null
   * @return the strings in mustCallAnnotation's value element, or the empty list if
   *     mustCallAnnotation is null
   */
  public List<String> getMustCallValues(@Nullable AnnotationMirror mustCallAnnotation) {
    if (mustCallAnnotation == null) {
      return Collections.emptyList();
    }
    return AnnotationUtils.getElementValueArray(
        mustCallAnnotation, mustCallValueElement, String.class, Collections.emptyList());
  }

  /**
   * Helper method to get the temporary variable that represents the given node, if one exists.
   *
   * @param node a node
   * @return the tempvar for node's expression, or null if one does not exist
   */
  public @Nullable LocalVariableNode getTempVarForNode(Node node) {
    return tempVarToTree.inverse().get(node.getTree());
  }

  /**
   * Is the given node a temporary variable?
   *
   * @param node a node
   * @return true iff the given node is a temporary variable
   */
  public boolean isTempVar(Node node) {
    return tempVarToTree.containsKey(node);
  }

  /**
   * Gets the tree for a temporary variable.
   *
   * @param node a node for a temporary variable
   * @return the tree for {@code node}
   */
  public Tree getTreeForTempVar(Node node) {
    if (!tempVarToTree.containsKey(node)) {
      throw new TypeSystemError(node + " must be a temporary variable");
    }
    return tempVarToTree.get(node);
  }

  /**
   * Registers a temporary variable by adding it to this type factory's tempvar map.
   *
   * @param tmpVar a temporary variable
   * @param tree the tree of the expression the tempvar represents
   */
  public void addTempVar(LocalVariableNode tmpVar, Tree tree) {
    if (!tempVarToTree.containsValue(tree)) {
      tempVarToTree.put(tmpVar, tree);
    }
  }

  /**
   * Returns true if the type of the tree includes a must-call annotation. Note that this method may
   * not consider dataflow, and is only safe to use when you need the declared, rather than
   * inferred, type of the tree.
   *
   * <p>Do not use this method if you are trying to get the must-call obligations of the resource
   * aliases of an {@link
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation}. Instead, use
   * {@link
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation#getMustCallMethods(RLCCalledMethodsAnnotatedTypeFactory,
   * CFStore)}.
   *
   * @param tree a tree
   * @return true if the tree has declared must-call obligations
   */
  public boolean declaredTypeHasMustCall(Tree tree) {
    assert tree instanceof MethodTree
            || tree instanceof VariableTree
            || tree instanceof NewClassTree
            || tree instanceof MethodInvocationTree
        : "unexpected declaration tree kind: " + tree.getKind();
    return !hasEmptyMustCallValue(tree);
  }

  /**
   * Returns true if the given tree has an {@link MustCallAlias} annotation and resource-alias
   * tracking is not disabled.
   *
   * @param tree a tree
   * @return true if the given tree has an {@link MustCallAlias} annotation
   */
  public boolean hasMustCallAlias(Tree tree) {
    Element elt = TreeUtils.elementFromTree(tree);
    return hasMustCallAlias(elt);
  }

  /**
   * Returns true if the given element has an {@link MustCallAlias} annotation and resource-alias
   * tracking is not disabled.
   *
   * @param elt an element
   * @return true if the given element has an {@link MustCallAlias} annotation
   */
  public boolean hasMustCallAlias(Element elt) {
    if (noResourceAliases) {
      return false;
    }
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    return mustCallAnnotatedTypeFactory.getDeclAnnotationNoAliases(elt, MustCallAlias.class)
        != null;
  }

  /**
   * Returns true if the declaration of the method being invoked has one or more {@link
   * CreatesMustCallFor} annotations.
   *
   * @param node a method invocation node
   * @return true iff there is one or more @CreatesMustCallFor annotations on the declaration of the
   *     invoked method
   */
  public boolean hasCreatesMustCallFor(MethodInvocationNode node) {
    ExecutableElement decl = TreeUtils.elementFromUse(node.getTree());
    return getDeclAnnotation(decl, CreatesMustCallFor.class) != null
        || getDeclAnnotation(decl, CreatesMustCallFor.List.class) != null;
  }

  /**
   * Does this type factory support {@link CreatesMustCallFor}?
   *
   * @return true iff the -AnoCreatesMustCallFor command-line argument was not supplied to the
   *     checker
   */
  public boolean canCreateObligations() {
    // Precomputing this call to `hasOption` causes a NullPointerException, so leave it as is.
    return !rlc.hasOption(MustCallChecker.NO_CREATES_MUSTCALLFOR);
  }

  /**
   * Fetches the store from the results of dataflow for {@code block}. The store after {@code block}
   * is returned.
   *
   * @param block a block
   * @return the appropriate CFStore, populated with CalledMethods annotations, from the results of
   *     running dataflow
   */
  public AccumulationStore getStoreAfterBlock(Block block) {
    return flowResult.getStoreAfter(block);
  }

  /**
   * Returns the then or else store after {@code block} depending on the value of {@code then} is
   * returned.
   *
   * @param block a conditional block
   * @param then wether the then store should be returned
   * @return the then or else store after {@code block} depending on the value of {@code then} is
   *     returned
   */
  public AccumulationStore getStoreAfterConditionalBlock(ConditionalBlock block, boolean then) {
    TransferInput<AccumulationValue, AccumulationStore> transferInput = flowResult.getInput(block);
    assert transferInput != null : "@AssumeAssertion(nullness): transferInput should be non-null";
    if (then) {
      return transferInput.getThenStore();
    }
    return transferInput.getElseStore();
  }

  @Override
  @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
  public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>>
      @Nullable T getTypeFactoryOfSubcheckerOrNull(Class<? extends SourceChecker> subCheckerClass) {
    if (subCheckerClass == MustCallChecker.class) {
      if (!canCreateObligations()) {
        return super.getTypeFactoryOfSubcheckerOrNull(MustCallNoCreatesMustCallForChecker.class);
      }
    }
    return super.getTypeFactoryOfSubcheckerOrNull(subCheckerClass);
  }

  /**
   * Returns the {@link EnsuresCalledMethods.List#value} element.
   *
   * @return the {@link EnsuresCalledMethods.List#value} element
   */
  public ExecutableElement getEnsuresCalledMethodsListValueElement() {
    return ensuresCalledMethodsListValueElement;
  }

  /**
   * Returns the {@link CreatesMustCallFor#value} element.
   *
   * @return the {@link CreatesMustCallFor#value} element
   */
  @Override
  public ExecutableElement getCreatesMustCallForValueElement() {
    return createsMustCallForValueElement;
  }

  /**
   * Returns the {@link org.checkerframework.checker.mustcall.qual.CreatesMustCallFor.List#value}
   * element.
   *
   * @return the {@link org.checkerframework.checker.mustcall.qual.CreatesMustCallFor.List#value}
   *     element
   */
  @Override
  public ExecutableElement getCreatesMustCallForListValueElement() {
    return createsMustCallForListValueElement;
  }

  /**
   * Does the given element have an {@code @NotOwning} annotation (including in stub files)?
   *
   * <p>Prefer this method to calling {@link #getDeclAnnotation(Element, Class)} on the type factory
   * directly, which won't find this annotation in stub files (it only considers stub files loaded
   * by this checker, not subcheckers).
   *
   * @param elt an element
   * @return true if there is a NotOwning annotation on the given element
   */
  public boolean hasNotOwning(Element elt) {
    MustCallAnnotatedTypeFactory mcatf = getTypeFactoryOfSubchecker(MustCallChecker.class);
    return mcatf.getDeclAnnotation(elt, NotOwning.class) != null;
  }

  /**
   * Does the given element have an {@code @Owning} annotation (including in stub files)?
   *
   * <p>Prefer this method to calling {@link #getDeclAnnotation(Element, Class)} on the type factory
   * directly, which won't find this annotation in stub files (it only considers stub files loaded
   * by this checker, not subcheckers).
   *
   * @param elt an element
   * @return true if there is an Owning annotation on the given element
   */
  public boolean hasOwning(Element elt) {
    MustCallAnnotatedTypeFactory mcatf = getTypeFactoryOfSubchecker(MustCallChecker.class);
    return mcatf.getDeclAnnotation(elt, Owning.class) != null;
  }

  @Override
  public Set<EnsuresCalledMethodOnExceptionContract> getExceptionalPostconditions(
      ExecutableElement methodOrConstructor) {
    Set<EnsuresCalledMethodOnExceptionContract> result =
        super.getExceptionalPostconditions(methodOrConstructor);

    // This override is a sneaky way to satisfy a few subtle design constraints:
    //   1. The RLC requires destructors to close the class's @Owning fields even on exception
    //      (see RLCCalledMethodsVisitor.checkOwningField).
    //   2. In versions 3.39.0 and earlier, the RLC did not have the annotation
    //      @EnsuresCalledMethodsOnException, meaning that for destructors it had to treat
    //      a simple @EnsuresCalledMethods annotation as serving both purposes.
    //
    // As a result, there is a lot of code that is missing the "correct"
    // @EnsuresCalledMethodsOnException annotations on its destructors.
    //
    // This override treats the @EnsuresCalledMethods annotations on destructors as if they
    // were also @EnsuresCalledMethodsOnException for backwards compatibility.  By overriding
    // this method we get both directions of checking: destructor implementations have to
    // satisfy these implicit contracts, and destructor callers get to benefit from them.
    //
    // It should be possible to remove this override entirely without sacrificing any soundness.
    // However, that is undesirable at this point because it would be a breaking change.
    //
    // TODO: gradually remove this override.
    //   1. When this override adds an implicit annotation, the Checker Framework should issue
    //      a warning along with a suggestion to add the right annotations.
    //   2. After a few months we should remove this override and require proper annotations on
    //      all destructors.

    if (isMustCallMethod(methodOrConstructor)) {
      Set<Contract.Postcondition> normalPostconditions =
          getContractsFromMethod().getPostconditions(methodOrConstructor);
      for (Contract.Postcondition normalPostcondition : normalPostconditions) {
        for (String method : getCalledMethods(normalPostcondition.annotation)) {
          result.add(
              new EnsuresCalledMethodOnExceptionContract(
                  normalPostcondition.expressionString, method));
        }
      }
    }

    return result;
  }

  /**
   * Returns true iff the {@code MustCall} annotation of the class that encloses the methodTree
   * names this method.
   *
   * @param elt a method
   * @return true if that method is one of the must-call methods for its enclosing class
   */
  private boolean isMustCallMethod(ExecutableElement elt) {
    TypeElement enclosingClass = ElementUtils.enclosingTypeElement(elt);
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotationMirror mcAnno =
        mustCallAnnotatedTypeFactory
            .getAnnotatedType(enclosingClass)
            .getPrimaryAnnotationInHierarchy(mustCallAnnotatedTypeFactory.TOP);
    List<String> mcValues =
        AnnotationUtils.getElementValueArray(
            mcAnno,
            mustCallAnnotatedTypeFactory.getMustCallValueElement(),
            String.class,
            Collections.emptyList());
    String methodName = elt.getSimpleName().toString();
    return mcValues.contains(methodName);
  }

  /**
   * Returns true if the checker should ignore exceptional control flow due to the given exception
   * type.
   *
   * @param exceptionType exception type
   * @return {@code true} if {@code exceptionType} is a member of {@link
   *     RLCCalledMethodsAnalysis#ignoredExceptionTypes}, {@code false} otherwise
   */
  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ((RLCCalledMethodsAnalysis) analysis).isIgnoredExceptionType(exceptionType);
  }

  /**
   * Fetches the transfer input for the given block, either from the flowResult, if the analysis is
   * still running, or else from the analysis itself.
   *
   * @param block a block
   * @return the appropriate TransferInput from the results of running dataflow
   */
  public TransferInput<AccumulationValue, AccumulationStore> getInput(Block block) {
    if (!analysis.isRunning()) {
      return flowResult.getInput(block);
    } else {
      return analysis.getInput(block);
    }
  }

  /** Wrapper for a loop that potentially calls methods on all elements of a collection/array. */
  public static class PotentiallyFulfillingCollectionLoop {

    /** AST {@code Tree} for collection iterated over. */
    public final ExpressionTree collectionTree;

    /** AST {@code Tree} for collection element iterated over. */
    public final Tree collectionElementTree;

    /** AST {@code Tree} for loop condition. */
    public final Tree condition;

    /** cfg {@code Block} containing the loop body entry. */
    public final Block loopBodyEntryBlock;

    /** cfg conditional {@link Block} following loop condition. */
    public final ConditionalBlock loopConditionalBlock;

    /** cfg {@code Node} for the collection element iterated over. */
    public final Node collectionElementNode;

    /**
     * Constructs a new {@code PotentiallyFulfillingCollectionLoop}.
     *
     * @param collectionTree AST {@link Tree} for collection iterated over
     * @param collectionElementTree AST {@link Tree} for collection element iterated over
     * @param condition AST {@link Tree} for loop condition
     * @param loopBodyEntryBlock cfg {@link Block} for the loop body entry
     * @param loopConditionalBlock cfg conditional {@link Block} following loop condition
     * @param collectionEltNode cfg {@link Node} for the collection element iterated over
     */
    public PotentiallyFulfillingCollectionLoop(
        ExpressionTree collectionTree,
        Tree collectionElementTree,
        Tree condition,
        Block loopBodyEntryBlock,
        ConditionalBlock loopConditionalBlock,
        Node collectionEltNode) {
      this.collectionTree = collectionTree;
      this.collectionElementTree = collectionElementTree;
      this.condition = condition;
      this.loopBodyEntryBlock = loopBodyEntryBlock;
      this.loopConditionalBlock = loopConditionalBlock;
      this.collectionElementNode = collectionEltNode;
    }
  }

  /**
   * A potentially fulfilling collection loop whose CFG-local information is complete enough for
   * consistency analysis.
   */
  public static class ResolvedPotentiallyFulfillingCollectionLoop
      extends PotentiallyFulfillingCollectionLoop {

    /**
     * The methods that the loop definitely calls on all elements of the collection it iterates
     * over.
     */
    protected final Set<String> calledMethods;

    /** cfg {@code Block} containing the loop update. */
    public final Block loopUpdateBlock;

    /**
     * Constructs a new {@code ResolvedPotentiallyFulfillingCollectionLoop}.
     *
     * @param collectionTree AST {@link Tree} for collection iterated over
     * @param collectionElementTree AST {@link Tree} for collection element iterated over
     * @param condition AST {@link Tree} for loop condition
     * @param loopBodyEntryBlock cfg {@link Block} for the loop body entry
     * @param loopUpdateBlock cfg {@link Block} for the loop update
     * @param loopConditionalBlock cfg conditional {@link Block} following loop condition
     * @param collectionEltNode cfg {@link Node} for the collection element iterated over
     */
    public ResolvedPotentiallyFulfillingCollectionLoop(
        ExpressionTree collectionTree,
        Tree collectionElementTree,
        Tree condition,
        Block loopBodyEntryBlock,
        Block loopUpdateBlock,
        ConditionalBlock loopConditionalBlock,
        Node collectionEltNode) {
      super(
          collectionTree,
          collectionElementTree,
          condition,
          loopBodyEntryBlock,
          loopConditionalBlock,
          collectionEltNode);
      this.calledMethods = new HashSet<>();
      this.loopUpdateBlock = loopUpdateBlock;
    }

    /**
     * Add methods that are guaranteed to be invoked on every element of the collection the loop
     * iterates over.
     *
     * @param methods the set of methods to add
     */
    public void addCalledMethods(Set<String> methods) {
      calledMethods.addAll(methods);
    }

    /**
     * Returns methods that are guaranteed to be invoked on every element of the collection the loop
     * iterates over.
     *
     * @return the set of methods the loop calls on all elements of the iterated collection
     */
    public Set<String> getCalledMethods() {
      return calledMethods;
    }
  }

  /**
   * After running the called-methods analysis, call the consistency analyzer to analyze
   * CFG-resolved potentially fulfilling collection loops, as determined by a pre-pattern-match in
   * the MustCallVisitor.
   *
   * <p>The analysis uses the CalledMethods type of the collection element iterated over to
   * determine the methods the loop calls on the collection elements.
   *
   * @param cfg the cfg of the enclosing method
   */
  @Override
  public void postAnalyze(ControlFlowGraph cfg) {
    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(ResourceLeakUtils.getResourceLeakChecker(this), true);
    MethodCollectionLoopState loopState = getMethodCollectionLoopState(cfg.getUnderlyingAST());

    if (loopState != null) {
      if (!loopState.potentiallyFulfillingEnhancedForLoops.isEmpty()) {
        new EnhancedForLoopResolver(cfg, loopState).resolveEnhancedForLoops();
      }
      new WhileLoopResolver(cfg).resolveWhileLoops(loopState);
      analyzeResolvedPotentiallyFulfillingCollectionLoops(
          cfg, loopState, mustCallConsistencyAnalyzer);
    }

    super.postAnalyze(cfg);
    removeMethodCollectionLoopState(cfg.getUnderlyingAST());
  }

  /**
   * Returns blocks reachable from {@code entryBlock}.
   *
   * <p>This remains a utility on the outer type because the resource leak consistency analyzer also
   * uses it.
   *
   * @param entryBlock the CFG entry block
   * @return the reachable blocks
   */
  public static Set<Block> reachableFrom(Block entryBlock) {
    return WhileLoopResolutionCache.reachableFrom(entryBlock);
  }

  /**
   * Analyzes CFG-resolved potentially fulfilling collection loops for the current method and
   * removes the ones that were analyzed.
   *
   * @param cfg the CFG of the current method
   * @param loopState per-method collection-loop state
   * @param mustCallConsistencyAnalyzer the consistency analyzer
   */
  private void analyzeResolvedPotentiallyFulfillingCollectionLoops(
      ControlFlowGraph cfg,
      MethodCollectionLoopState loopState,
      MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer) {
    if (loopState.resolvedPotentiallyFulfillingCollectionLoops.isEmpty()) {
      return;
    }

    Iterator<ResolvedPotentiallyFulfillingCollectionLoop> resolvedLoopIterator =
        loopState.resolvedPotentiallyFulfillingCollectionLoops.iterator();
    while (resolvedLoopIterator.hasNext()) {
      ResolvedPotentiallyFulfillingCollectionLoop resolvedLoop = resolvedLoopIterator.next();
      Tree collectionElementTree = resolvedLoop.collectionElementTree;
      boolean loopContainedInThisMethod =
          cfg.getNodesCorrespondingToTree(collectionElementTree) != null;
      if (loopContainedInThisMethod) {
        mustCallConsistencyAnalyzer.analyzeResolvedPotentiallyFulfillingCollectionLoop(
            cfg, resolvedLoop);
        resolvedLoopIterator.remove();
      }
    }
  }

  /** Resolves enhanced-for-loop candidates into CFG-resolved loops for consistency analysis. */
  private final class EnhancedForLoopResolver {
    /** The CFG of the current method. */
    private final ControlFlowGraph cfg;

    /** Per-method collection-loop state. */
    private final MethodCollectionLoopState loopState;

    /** Blocks that have already been visited while traversing the CFG. */
    private final Set<Block> visitedBlocks = new HashSet<>();

    /** Worklist for CFG traversal. */
    private final Deque<Block> worklist = new ArrayDeque<>();

    /**
     * Creates a resolver for enhanced-for-loops in the given CFG.
     *
     * @param cfg the CFG of the current method
     * @param loopState per-method collection-loop state
     */
    private EnhancedForLoopResolver(ControlFlowGraph cfg, MethodCollectionLoopState loopState) {
      this.cfg = cfg;
      this.loopState = loopState;
    }

    /** Traverses the CFG and records resolved enhanced-for-loops for the pending candidates. */
    private void resolveEnhancedForLoops() {
      Block entryBlock = cfg.getEntryBlock();
      worklist.add(entryBlock);
      visitedBlocks.add(entryBlock);

      while (!worklist.isEmpty() && !loopState.potentiallyFulfillingEnhancedForLoops.isEmpty()) {
        Block currentBlock = worklist.removeFirst();

        for (Node node : currentBlock.getNodes()) {
          if (node instanceof MethodInvocationNode) {
            resolveEnhancedForLoop((MethodInvocationNode) node);
          }
        }

        for (IPair<Block, @Nullable TypeMirror> successorAndExceptionType :
            getSuccessorsExceptIgnoredExceptions(currentBlock)) {
          Block successorBlock = successorAndExceptionType.first;
          if (successorBlock != null && visitedBlocks.add(successorBlock)) {
            worklist.addLast(successorBlock);
          }
        }
      }
    }

    /**
     * Returns all successor blocks for some block, except for those corresponding to ignored
     * exception types. See {@link RLCCalledMethodsAnalysis#isIgnoredExceptionType(TypeMirror)}.
     *
     * @param block input block
     * @return set of pairs (b, t), where b is a successor block, and t is the type of exception for
     *     the CFG edge from block to b, or {@code null} if b is a non-exceptional successor
     */
    private Set<IPair<Block, @Nullable TypeMirror>> getSuccessorsExceptIgnoredExceptions(
        Block block) {
      if (block.getType() == Block.BlockType.EXCEPTION_BLOCK) {
        ExceptionBlock exceptionBlock = (ExceptionBlock) block;
        Set<IPair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
        Block regularSuccessor = exceptionBlock.getSuccessor();
        if (regularSuccessor != null) {
          result.add(IPair.of(regularSuccessor, null));
        }
        Map<TypeMirror, Set<Block>> exceptionalSuccessors =
            exceptionBlock.getExceptionalSuccessors();
        for (Map.Entry<TypeMirror, Set<Block>> entry : exceptionalSuccessors.entrySet()) {
          TypeMirror exceptionType = entry.getKey();
          if (!isIgnoredExceptionType(exceptionType)) {
            for (Block exceptionalSuccessor : entry.getValue()) {
              result.add(IPair.of(exceptionalSuccessor, exceptionType));
            }
          }
        }
        return result;
      } else {
        Set<IPair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
        for (Block successorBlock : block.getSuccessors()) {
          result.add(IPair.of(successorBlock, null));
        }
        return result;
      }
    }

    /**
     * Records a resolved collection loop if the given node is desugared from an enhanced-for-loop
     * over a collection.
     *
     * @param methodInvocationNode the node to check
     */
    private void resolveEnhancedForLoop(MethodInvocationNode methodInvocationNode) {
      if (methodInvocationNode.getIterableExpression() == null) {
        return;
      }

      EnhancedForLoopTree loop = methodInvocationNode.getEnhancedForLoop();
      if (loop == null) {
        throw new BugInCF(
            "MethodInvocationNode.iterableExpression should be non-null iff"
                + " MethodInvocationNode.enhancedForLoop is non-null");
      }
      if (!loopState.potentiallyFulfillingEnhancedForLoops.contains(loop)) {
        return;
      }

      VariableTree loopVariable = loop.getVariable();

      // Find the first block of the loop body by traversing the desugared iterator.next() path
      // until the assignment of the loop variable is found.
      SingleSuccessorBlock singleSuccessorBlock =
          (SingleSuccessorBlock) methodInvocationNode.getBlock();
      Iterator<Node> nodeIterator = singleSuccessorBlock.getNodes().iterator();
      Node loopVariableNode = null;
      Node node;
      boolean isAssignmentOfLoopVariable;
      do {
        while (!nodeIterator.hasNext()) {
          singleSuccessorBlock = (SingleSuccessorBlock) singleSuccessorBlock.getSuccessor();
          nodeIterator = singleSuccessorBlock.getNodes().iterator();
        }
        node = nodeIterator.next();
        isAssignmentOfLoopVariable = false;
        if ((node instanceof AssignmentNode) && (node.getTree() instanceof VariableTree)) {
          loopVariableNode = ((AssignmentNode) node).getTarget();
          VariableTree iteratorVariableDeclaration = (VariableTree) node.getTree();
          isAssignmentOfLoopVariable =
              iteratorVariableDeclaration.getName() == loopVariable.getName();
        }
      } while (!isAssignmentOfLoopVariable);
      Block loopBodyEntryBlock = singleSuccessorBlock.getSuccessor();

      // Find the desugared loop condition by traversing the CFG backwards until iterator.hasNext()
      // is found.
      Block loopUpdateBlock = methodInvocationNode.getBlock();
      nodeIterator = loopUpdateBlock.getNodes().iterator();
      boolean isLoopCondition;
      do {
        while (!nodeIterator.hasNext()) {
          Set<Block> predecessorBlocks = loopUpdateBlock.getPredecessors();
          if (predecessorBlocks.size() == 1) {
            loopUpdateBlock = predecessorBlocks.iterator().next();
            nodeIterator = loopUpdateBlock.getNodes().iterator();
          } else {
            // There is no trivial resolution here. Best we can do is skip this loop.
            return;
          }
        }
        node = nodeIterator.next();
        isLoopCondition = false;
        if (node instanceof MethodInvocationNode) {
          MethodInvocationTree methodInvocationTree = ((MethodInvocationNode) node).getTree();
          isLoopCondition = TreeUtils.isHasNextCall(methodInvocationTree);
        }
      } while (!isLoopCondition);

      Block blockContainingLoopCondition = node.getBlock();
      if (blockContainingLoopCondition.getSuccessors().size() != 1) {
        throw new BugInCF(
            "loop condition has: "
                + blockContainingLoopCondition.getSuccessors().size()
                + " successors instead of 1.");
      }
      Block conditionalBlock = blockContainingLoopCondition.getSuccessors().iterator().next();
      if (!(conditionalBlock instanceof ConditionalBlock)) {
        throw new BugInCF(
            "loop condition successor is not ConditionalBlock, but: "
                + conditionalBlock.getClass());
      }

      loopState.resolvedPotentiallyFulfillingCollectionLoops.add(
          new ResolvedPotentiallyFulfillingCollectionLoop(
              loop.getExpression(),
              loopVariableNode.getTree(),
              node.getTree(),
              loopBodyEntryBlock,
              loopUpdateBlock,
              (ConditionalBlock) conditionalBlock,
              loopVariableNode));
      loopState.potentiallyFulfillingEnhancedForLoops.remove(loop);
    }
  }

  /** Resolves while-loop candidates into CFG-resolved loops for consistency analysis. */
  private static final class WhileLoopResolver {
    /** The CFG of the current method. */
    private final ControlFlowGraph cfg;

    /**
     * Creates a resolver for potentially fulfilling while loops in the given CFG.
     *
     * @param cfg the enclosing method CFG
     */
    private WhileLoopResolver(ControlFlowGraph cfg) {
      this.cfg = cfg;
    }

    /**
     * Resolves all potentially fulfilling while loops in the given method state that can be tied to
     * a loop update block in the current CFG.
     *
     * @param loopState per-method collection-loop state
     */
    private void resolveWhileLoops(MethodCollectionLoopState loopState) {
      if (loopState.potentiallyFulfillingCollectionLoops.isEmpty()) {
        return;
      }

      WhileLoopResolutionCache whileLoopCache = loopState.getOrCreateWhileLoopCache(cfg);

      Iterator<PotentiallyFulfillingCollectionLoop> potentialLoopIterator =
          loopState.potentiallyFulfillingCollectionLoops.iterator();
      while (potentialLoopIterator.hasNext()) {
        PotentiallyFulfillingCollectionLoop potentialLoop = potentialLoopIterator.next();
        ResolvedPotentiallyFulfillingCollectionLoop resolvedLoop =
            resolveWhileLoop(potentialLoop, whileLoopCache);
        if (resolvedLoop != null) {
          loopState.resolvedPotentiallyFulfillingCollectionLoops.add(resolvedLoop);
          potentialLoopIterator.remove();
        }
      }
    }

    /**
     * Resolves one potentially fulfilling while loop if a suitable loop update block can be found.
     *
     * @param potentialLoop a potentially fulfilling while loop
     * @param whileLoopCache cached CFG facts for while-loop resolution
     * @return the CFG-resolved loop, or {@code null} if no loop update block is found
     */
    private @Nullable ResolvedPotentiallyFulfillingCollectionLoop resolveWhileLoop(
        PotentiallyFulfillingCollectionLoop potentialLoop,
        WhileLoopResolutionCache whileLoopCache) {
      Block loopUpdateBlock =
          chooseLoopUpdateBlockForPotentiallyFulfillingLoop(potentialLoop, whileLoopCache);
      if (loopUpdateBlock == null) {
        return null;
      }
      return new ResolvedPotentiallyFulfillingCollectionLoop(
          potentialLoop.collectionTree,
          potentialLoop.collectionElementTree,
          potentialLoop.condition,
          potentialLoop.loopBodyEntryBlock,
          loopUpdateBlock,
          potentialLoop.loopConditionalBlock,
          potentialLoop.collectionElementNode);
    }

    /**
     * Chooses the best loop update block for a potentially fulfilling while loop by matching it to
     * the tightest natural loop that contains both the body entry and the loop condition.
     *
     * @param potentialLoop a potentially fulfilling while loop
     * @param whileLoopCache cached CFG facts for while-loop resolution
     * @return the chosen loop update block, or {@code null} if none is found
     */
    private @Nullable Block chooseLoopUpdateBlockForPotentiallyFulfillingLoop(
        PotentiallyFulfillingCollectionLoop potentialLoop,
        WhileLoopResolutionCache whileLoopCache) {

      Block bodyEntryBlock = potentialLoop.loopBodyEntryBlock;
      Block conditionalBlock = potentialLoop.loopConditionalBlock;

      Block bestLoopUpdateBlock = null;
      int bestLoopSize = Integer.MAX_VALUE;

      for (WhileLoopResolutionCache.BlockEdge backEdge : whileLoopCache.getBackEdges()) {
        // backEdge.targetBlock is the candidate block that the loop body flows back to.
        Set<Block> naturalLoop = whileLoopCache.getNaturalLoopForBackEdge(backEdge);

        // Must contain this while-loop's body entry and conditional block.
        if (!naturalLoop.contains(bodyEntryBlock)) {
          continue;
        }
        if (!naturalLoop.contains(conditionalBlock)) {
          continue;
        }

        // Prefer the tightest loop. This helps nested-loop disambiguation.
        if (naturalLoop.size() < bestLoopSize) {
          bestLoopSize = naturalLoop.size();
          bestLoopUpdateBlock = backEdge.targetBlock;
        }
      }

      return bestLoopUpdateBlock;
    }
  }
}
