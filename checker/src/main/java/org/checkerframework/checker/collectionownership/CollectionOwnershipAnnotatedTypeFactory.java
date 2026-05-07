package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.collectionownership.qual.CollectionFieldDestructor;
import org.checkerframework.checker.collectionownership.qual.CreatesCollectionObligation;
import org.checkerframework.checker.collectionownership.qual.NotOwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionBottom;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionWithoutObligation;
import org.checkerframework.checker.collectionownership.qual.PolyOwningCollection;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.MustCallInference;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.framework.flow.CFAbstractAnalysis.FieldInitialValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/** The annotated type factory for the Collection Ownership Checker. */
public class CollectionOwnershipAnnotatedTypeFactory
    extends GenericAnnotatedTypeFactory<
        CFValue,
        CollectionOwnershipStore,
        CollectionOwnershipTransfer,
        CollectionOwnershipAnalysis> {

  /**
   * The {@link MustCallAnnotatedTypeFactory} instance in the checker hierarchy. Used for getting
   * the {@code @MustCall} type of expressions.
   */
  private final MustCallAnnotatedTypeFactory mcAtf;

  /** Map from a loop-condition {@code Tree} to its corresponding {@link DisposalLoopInfo}. */
  private final IdentityHashMap<Tree, DisposalLoopInfo> conditionToDisposalLoopInfoMap =
      new IdentityHashMap<>();

  /** Map from a loop's conditional {@code Block} to its corresponding {@link DisposalLoopInfo}. */
  private final IdentityHashMap<Block, DisposalLoopInfo> conditionalBlockToDisposalLoopInfoMap =
      new IdentityHashMap<>();

  /**
   * Map from a {@link DisposalLoopInfo} to the set of called-methods on the iterated element in its
   * loop body.
   */
  private final IdentityHashMap<DisposalLoopInfo, Set<String>> disposalLoopInfoToCalledMethodsMap =
      new IdentityHashMap<>();

  /**
   * Map from a {@code MethodTree} to the {@link DisposalLoopInfo}s discovered in that method's
   * body.
   */
  private final IdentityHashMap<MethodTree, HashSet<DisposalLoopInfo>>
      preparedDisposalLoopInfosByMethod = new IdentityHashMap<>();

  /** The {@code @}{@link NotOwningCollection} annotation. */
  public final AnnotationMirror TOP;

  /** The {@code @}{@link NotOwningCollection} annotation. Equals TOP. */
  public final AnnotationMirror NOTOWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollection} annotation. */
  public final AnnotationMirror OWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollectionWithoutObligation} annotation. */
  public final AnnotationMirror OWNINGCOLLECTIONWITHOUTOBLIGATION;

  /**
   * The {@code @}{@link OwningCollectionBottom}{@code ()} annotation. It is the default in
   * unannotated code.
   */
  public final AnnotationMirror BOTTOM;

  /** The {@code @}{@link PolyOwningCollection}{@code ()} polymorphic annotation. */
  public final AnnotationMirror POLY;

  /** The value element of the {@code @}{@link CollectionFieldDestructor} annotation. */
  private final ExecutableElement collectionFieldDestructorValueElement =
      TreeUtils.getMethod(CollectionFieldDestructor.class, "value", 0, processingEnv);

  /**
   * Method CFGs whose resource-leak post-analysis already ran after the first method analysis and
   * before any contained lambdas were analyzed.
   */
  private final Set<ControlFlowGraph> preLambdaPostAnalyzedMethods =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * Enum for the types in the hierarchy. Combined with a few utility methods to get the right enum
   * value from various sources, this is a convenient interface to deal with annotations in this
   * hierarchy.
   */
  public enum CollectionOwnershipType {
    /** The @NotOwningCollection type. */
    NotOwningCollection,
    /** The @OwningCollection type. */
    OwningCollection,
    /** The @OwningCollectionWithoutObligation type. */
    OwningCollectionWithoutObligation,
    /** The @OwningCollectionBottom type. */
    OwningCollectionBottom
  };

  /** Classification of the argument inserted by a {@code @CreatesCollectionObligation} mutator. */
  public enum CollectionMutatorArgumentKind {
    /**
     * The inserted argument is a resource collection, such as in {@code addAll} or {@code putAll}.
     */
    BULK_RESOURCE_COLLECTION,
    /** The inserted argument is definitely non-owning at the call site. */
    DEFINITELY_NON_OWNING,
    /** The inserted argument may be owning or its ownership could not be proven. */
    MAY_BE_OWNING
  }

  /**
   * The method name used for CollectionObligations that represent an obligation of MustCallUnknown.
   * The digit in the first character ensures this cannot coincide with an actual method name.
   */
  public static final String UNKNOWN_METHOD_NAME = "1UNKNOWN";

  /**
   * Creates a CollectionOwnershipAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  @SuppressWarnings("this-escape")
  public CollectionOwnershipAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    NOTOWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, NotOwningCollection.class);
    TOP = NOTOWNINGCOLLECTION;
    OWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, OwningCollection.class);
    OWNINGCOLLECTIONWITHOUTOBLIGATION =
        AnnotationBuilder.fromClass(elements, OwningCollectionWithoutObligation.class);
    BOTTOM = AnnotationBuilder.fromClass(elements, OwningCollectionBottom.class);
    POLY = AnnotationBuilder.fromClass(elements, PolyOwningCollection.class);
    mcAtf = ResourceLeakUtils.getMustCallAnnotatedTypeFactory(checker);
    this.postInit();
  }

  /**
   * Returns the {@link DisposalLoopInfo} corresponding to the loop condition {@code tree}, if one
   * exists.
   *
   * @param tree the condition tree
   * @return the {@link DisposalLoopInfo} for condition {@code tree} if exists, otherwise {@code
   *     null}.
   */
  public @Nullable DisposalLoopInfo getDisposalLoopInfoForConditionTree(Tree tree) {
    return conditionToDisposalLoopInfoMap.get(tree);
  }

  /**
   * Returns the {@link DisposalLoopInfo} corresponding to the loop conditional {@code block}, if
   * one exists.
   *
   * @param block the loop-condition block
   * @return the {@link DisposalLoopInfo} for conditional {@code block} if exists, otherwise {@code
   *     null}.
   */
  public @Nullable DisposalLoopInfo getDisposalLoopInfoForConditionBlock(Block block) {
    return conditionalBlockToDisposalLoopInfoMap.get(block);
  }

  /**
   * Returns the called-methods on the iterated element for a {@link DisposalLoopInfo}.
   *
   * @param disposalLoopInfo the {@link DisposalLoopInfo}
   * @return the called-methods for {@link DisposalLoopInfo}, or {@code null} if no information
   *     exits
   */
  public @Nullable Set<String> getCalledMethods(DisposalLoopInfo disposalLoopInfo) {
    return disposalLoopInfoToCalledMethodsMap.get(disposalLoopInfo);
  }

  /**
   * Returns true if the checker should ignore exceptional control flow due to the given exception
   * type.
   *
   * @param exceptionType exception type
   * @return true if {@code exceptionType} is ignored by collection-ownership flow
   */
  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return analysis.isIgnoredExceptionType(exceptionType);
  }

  /**
   * Registers a {@link DisposalLoopInfo} together with the called-methods on the iterated element
   * over that loop's body.
   *
   * @param disposalLoopInfo the {@link DisposalLoopInfo}
   * @param calledMethods the called-methods on the iterated element over {@link DisposalLoopInfo}'s
   *     body
   */
  private void registerCalledMethods(DisposalLoopInfo disposalLoopInfo, Set<String> calledMethods) {
    conditionToDisposalLoopInfoMap.put(disposalLoopInfo.loopConditionTree(), disposalLoopInfo);
    conditionalBlockToDisposalLoopInfoMap.put(
        disposalLoopInfo.loopConditionalBlock(), disposalLoopInfo);
    disposalLoopInfoToCalledMethodsMap.put(
        disposalLoopInfo, Collections.unmodifiableSet(new LinkedHashSet<>(calledMethods)));
  }

  /**
   * Scans a method CFG for {@link DisposalLoopInfo}'s and returns the discovered loops.
   *
   * @param cfg the CFG to scan
   * @return the {@link DisposalLoopInfo}'s discovered in {@code cfg}
   */
  private Set<DisposalLoopInfo> scanForDisposalLoopInfos(ControlFlowGraph cfg) {
    if (cfg.getUnderlyingAST().getKind() != UnderlyingAST.Kind.METHOD) {
      return Collections.emptySet();
    }
    return new DisposalLoopScanner(
            this, ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(this), cfg)
        .scanTree(((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod());
  }

  /**
   * Discovers and stores the {@link DisposalLoopInfo}'s for a method {@code cfg}. This discovery
   * must happen before {@link
   * org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsTransfer#initialStore(UnderlyingAST,
   * List)} runs, so that the called-methods initial stores are populated for the loop's iterated
   * temp elements, e.g., col.get(i), col.pop().
   *
   * @param cfg the method CFG whose disposal loops to be discovered
   */
  public void discoverDisposalLoopInfos(ControlFlowGraph cfg) {
    MethodTree methodTree = getEnclosingMethodTree(cfg.getUnderlyingAST());
    if (methodTree == null) {
      return;
    }
    preparedDisposalLoopInfosByMethod.put(
        methodTree, new LinkedHashSet<>(scanForDisposalLoopInfos(cfg)));
  }

  /**
   * Returns the {@link DisposalLoopInfo}'s for the given underlying AST.
   *
   * @param underlyingAST the underlying AST whose disposal loops should be returned
   * @return the set of disposal loops for {@code underlyingAST}
   */
  public Set<DisposalLoopInfo> getDisposalLoopInfos(UnderlyingAST underlyingAST) {
    MethodTree methodTree = getEnclosingMethodTree(underlyingAST);
    if (methodTree == null) {
      return Collections.emptySet();
    }
    Set<DisposalLoopInfo> preparedDisposalLoopInfos =
        preparedDisposalLoopInfosByMethod.get(methodTree);
    if (preparedDisposalLoopInfos == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(new LinkedHashSet<>(preparedDisposalLoopInfos));
  }

  /**
   * Removes and returns the {@link DisposalLoopInfo}'s for the given underlying AST.
   *
   * @param underlyingAST the underlying AST whose disposal loops should be removed
   * @return the removed disposal loops for {@code underlyingAST}
   */
  private Set<DisposalLoopInfo> removePreparedDisposalLoopInfos(UnderlyingAST underlyingAST) {
    MethodTree methodTree = getEnclosingMethodTree(underlyingAST);
    if (methodTree == null) {
      return Collections.emptySet();
    }
    Set<DisposalLoopInfo> preparedDisposalLoopInfos =
        preparedDisposalLoopInfosByMethod.remove(methodTree);
    if (preparedDisposalLoopInfos == null) {
      return Collections.emptySet();
    }
    return preparedDisposalLoopInfos;
  }

  /**
   * Returns the enclosing method tree for the given underlying AST, if it is a method CFG.
   *
   * @param underlyingAST the underlying AST
   * @return the enclosing method tree, or {@code null} if {@code underlyingAST} is not a method
   */
  private @Nullable MethodTree getEnclosingMethodTree(UnderlyingAST underlyingAST) {
    if (underlyingAST.getKind() != UnderlyingAST.Kind.METHOD) {
      return null;
    }
    return ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
  }

  @Override
  protected void postCFGConstruction(ControlFlowGraph cfg, UnderlyingAST ast) {
    // Discovers disposal loops in method's CFG, and for each disposal loop store the called-methods
    // on the iterated element by the  loop's body using MustCallConsistencyAnalyzer.
    if (ast.getKind() != UnderlyingAST.Kind.METHOD) {
      return;
    }

    Set<DisposalLoopInfo> preparedDisposalLoopInfos = removePreparedDisposalLoopInfos(ast);
    if (preparedDisposalLoopInfos.isEmpty()) {
      return;
    }

    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(ResourceLeakUtils.getResourceLeakChecker(this), true);
    for (DisposalLoopInfo disposalLoopInfo : preparedDisposalLoopInfos) {
      Set<String> calledMethods =
          mustCallConsistencyAnalyzer.analyzeDisposalLoop(cfg, disposalLoopInfo);
      if (calledMethods != null) {
        registerCalledMethods(disposalLoopInfo, calledMethods);
      }
    }
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            PolyOwningCollection.class,
            NotOwningCollection.class,
            OwningCollection.class,
            OwningCollectionWithoutObligation.class,
            OwningCollectionBottom.class));
  }

  @Override
  protected ControlFlowGraph analyze(
      Queue<IPair<ClassTree, @Nullable CollectionOwnershipStore>> classQueue,
      Queue<IPair<LambdaExpressionTree, @Nullable CollectionOwnershipStore>> lambdaQueue,
      UnderlyingAST ast,
      List<FieldInitialValue<CFValue>> fieldValues,
      @Nullable ControlFlowGraph cfg,
      boolean isInitializationCode,
      boolean updateInitializationStore,
      boolean isStatic,
      @Nullable CollectionOwnershipStore capturedStore) {
    ControlFlowGraph result =
        super.analyze(
            classQueue,
            lambdaQueue,
            ast,
            fieldValues,
            cfg,
            isInitializationCode,
            updateInitializationStore,
            isStatic,
            capturedStore);
    if (cfg == null && ast.getKind() == UnderlyingAST.Kind.METHOD) {
      // This uses the same workaround pattern for lambdas that originally lived in
      // RLCCalledMethodsAnnotatedTypeFactory#analyze:
      // run the resource-leak post-analysis immediately after the first method analysis, before
      // containing lambdas are reanalyzed to fixpoint.
      runResourceLeakPostAnalyze(result);
      preLambdaPostAnalyzedMethods.add(result);
    }
    return result;
  }

  @Override
  public void postAnalyze(ControlFlowGraph cfg) {
    if (!preLambdaPostAnalyzedMethods.remove(cfg)) {
      runResourceLeakPostAnalyze(cfg);
    }
    super.postAnalyze(cfg);
  }

  /**
   * Runs the resource-leak post-analysis that must happen in the last checker in the Resource Leak
   * Checker hierarchy.
   *
   * @param cfg the CFG to analyze
   */
  private void runResourceLeakPostAnalyze(ControlFlowGraph cfg) {
    ResourceLeakChecker rlc = ResourceLeakUtils.getResourceLeakChecker(this);
    rlc.setRoot(root);
    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(rlc, false);
    mustCallConsistencyAnalyzer.analyze(cfg);
    RLCCalledMethodsAnnotatedTypeFactory cmAtf =
        (RLCCalledMethodsAnnotatedTypeFactory)
            ResourceLeakUtils.getRLCCalledMethodsChecker(this).getTypeFactory();
    // Inferring owning annotations for @Owning fields/parameters, @EnsuresCalledMethods for
    // finalizer methods, and @InheritableMustCall annotations for the class declarations.
    if (cmAtf.getWholeProgramInference() != null) {
      if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
        MustCallInference.runMustCallInference(cmAtf, cfg, mustCallConsistencyAnalyzer);
      }
    }
  }

  /**
   * Returns true if the given type is a resource collection: a type assignable from {@code
   * Collection} whose single type var has non-empty MustCall type.
   *
   * <p>This overload should be used only before computation of AnnotatedTypeMirrors is completed,
   * in particular in addComputedTypeAnnotations(AnnotatedTypeMirror).
   *
   * @param t the AnnotatedTypeMirror
   * @return true if t is a resource collection
   */
  public boolean isResourceCollection(TypeMirror t) {
    List<String> mcValues = getMustCallValuesOfResourceCollectionComponent(t);
    return mcValues != null && !mcValues.isEmpty();
  }

  /**
   * Returns true if the given element is a resource collection field that is declared as
   * {@code @OwningCollection}. Since that is the default, this method also returns true if the
   * field has no collection ownership annotation.
   *
   * @param elt a field that might be a resource collection
   * @return true if the element is a resource collection field that is {@code @OwningCollection} by
   *     declaration
   */
  public boolean isOwningCollectionField(Element elt) {
    if (elt == null) {
      return false;
    }
    if (elt.getKind().isField()) {
      if (isResourceCollection(elt.asType())) {
        AnnotatedTypeMirror atm = getAnnotatedType(elt);
        CollectionOwnershipType fieldType =
            getCoType(Collections.singletonList(atm.getPrimaryAnnotationInHierarchy(TOP)));
        if (fieldType == null) {
          return false;
        }
        return switch (fieldType) {
          case OwningCollection, OwningCollectionWithoutObligation -> true;
          default -> false;
        };
      }
    }
    return false;
  }

  /**
   * Returns true if the given element is a resource collection field.
   *
   * @param elt an element
   * @return true if the element is a resource collection field
   */
  public boolean isResourceCollectionField(Element elt) {
    if (elt == null) {
      return false;
    }
    if (elt.getKind().isField()) {
      return isResourceCollection(elt.asType());
    }
    return false;
  }

  /**
   * Returns true if the given element is a resource collection parameter that is declared as
   * {@code @OwningCollection}. Since that is the default, this method also returns true if the
   * parameter has no collection ownership annotation.
   *
   * @param elt an element
   * @return true if the element is a resource collection parameter that is
   *     {@code @OwningCollection} by declaration
   */
  public boolean isOwningCollectionParameter(Element elt) {
    if (isResourceCollection(elt.asType())) {
      if (elt.getKind() == ElementKind.PARAMETER) {
        AnnotatedTypeMirror atm = getAnnotatedType(elt);
        CollectionOwnershipType paramType =
            getCoType(Collections.singletonList(atm.getPrimaryAnnotationInHierarchy(TOP)));
        if (paramType == null) {
          return false;
        }
        return paramType == CollectionOwnershipType.OwningCollection;
      }
    }
    return false;
  }

  /**
   * Returns true if the given AST tree is a resource collection.
   *
   * <p>That is, whether the given tree is of a type assignable from java.util.Collection, whose
   * only type var has non-empty MustCall type.
   *
   * @param tree the tree
   * @return true if the tree is a resource collection
   */
  public boolean isResourceCollection(Tree tree) {
    if (tree == null) {
      return false;
    }
    AnnotatedTypeMirror treeMcType;
    try {
      treeMcType = mcAtf.getAnnotatedType(tree);
    } catch (BugInCF e) {
      // this happens if the tree is not of a supported format, thrown by
      // AnnotatedTypeFactory#getAnnotatedType
      treeMcType = null;
    }
    List<String> mcValues = getMustCallValuesOfResourceCollectionComponent(treeMcType);
    return mcValues != null && !mcValues.isEmpty();
  }

  /**
   * Returns true if the given method is annotated {@code @CreatesCollectionObligation}.
   *
   * @param methodElement the method
   * @return true if the method is annotated {@code @CreatesCollectionObligation}
   */
  public boolean isCreatesCollectionObligationMethod(ExecutableElement methodElement) {
    return getDeclAnnotation(methodElement, CreatesCollectionObligation.class) != null;
  }

  /**
   * Returns the argument whose obligation is transferred by a {@code @CreatesCollectionObligation}
   * call, or {@code null} if the call has no such argument.
   *
   * <p>The current heuristic is that the inserted element is the last argument at the call site.
   * TODO: Make @CreatesCollectionObligation accept an index for the inserted elements position.
   *
   * @param tree a method invocation tree
   * @return the inserted argument tree, or null if there is none
   */
  public @Nullable ExpressionTree getInsertedArgumentTree(MethodInvocationTree tree) {
    if (tree.getArguments().isEmpty()) {
      return null;
    }
    return tree.getArguments().get(tree.getArguments().size() - 1);
  }

  /**
   * Returns the argument whose obligation is transferred by a {@code @CreatesCollectionObligation}
   * call, or {@code null} if the call has no such argument.
   *
   * @param node a method invocation node
   * @return the inserted argument node, or null if there is none
   */
  public @Nullable Node getInsertedArgumentNode(MethodInvocationNode node) {
    List<Node> args = node.getArguments();
    if (args.isEmpty()) {
      return null;
    }
    return args.get(args.size() - 1);
  }

  /**
   * Classifies the argument inserted by a {@code @CreatesCollectionObligation} mutator call.
   *
   * <p>For resource collections, this returns {@link
   * CollectionMutatorArgumentKind#BULK_RESOURCE_COLLECTION}. For non-collection values, this
   * returns {@link CollectionMutatorArgumentKind#DEFINITELY_NON_OWNING} only if ownership is
   * definitely absent at the call site.
   *
   * @param insertedArgumentTree the inserted argument
   * @return the inserted argument's ownership classification
   */
  public CollectionMutatorArgumentKind getCollectionMutatorArgumentKind(Tree insertedArgumentTree) {
    if (insertedArgumentTree == null) {
      return CollectionMutatorArgumentKind.MAY_BE_OWNING;
    }

    if (isResourceCollection(insertedArgumentTree)) {
      return CollectionMutatorArgumentKind.BULK_RESOURCE_COLLECTION;
    }

    RLCCalledMethodsAnnotatedTypeFactory rlAtf =
        ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(this);

    // If the inserted element annotated as @NotOwning, then it's definitely not owning
    Element insertedElement = TreeUtils.elementFromTree(insertedArgumentTree);
    if (insertedElement != null && insertedElement.getAnnotation(NotOwning.class) != null) {
      return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
    }

    // If the inserted element is a parameter, and it has no explicit @Owning annotation, then it's
    // definitely not owning.
    if (insertedElement != null && insertedElement.getKind() == ElementKind.PARAMETER) {
      return insertedElement.getAnnotation(Owning.class) == null
          ? CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING
          : CollectionMutatorArgumentKind.MAY_BE_OWNING;
    }

    // If the inserted element is a method call, and the callee has no @Owning annotation, then it's
    // definitely not owning.
    if (insertedArgumentTree instanceof MethodInvocationTree mit) {
      ExecutableElement callee = TreeUtils.elementFromUse(mit);
      if (rlAtf.hasNotOwning(callee)) {
        return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
      }
    }

    AnnotatedTypeMirror mustCallType = mcAtf.getAnnotatedType(insertedArgumentTree);
    if (mustCallType != null && mustCallType.hasPrimaryAnnotation(NotOwning.class)) {
      return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
    }

    TypeMirror typeMirror = TreeUtils.typeOf(insertedArgumentTree);
    TypeElement typeElement = TypesUtils.getTypeElement(typeMirror);
    if (typeElement != null && rlAtf.hasEmptyMustCallValue(typeElement)) {
      return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
    }

    return CollectionMutatorArgumentKind.MAY_BE_OWNING;
  }

  /**
   * If the given type is a collection, this method returns the MustCall values of its elements or
   * null if there are none or if the given type is not a collection.
   *
   * <p>That is, if the given type is a Java.util.Collection implementation, this method returns the
   * MustCall values of its type variable upper bound if there are any or else null.
   *
   * @param atm the AnnotatedTypeMirror
   * @return if the given type is a collection, returns the MustCall values of its elements or null
   *     if there are none or if the given type is not a collection
   */
  public List<String> getMustCallValuesOfResourceCollectionComponent(AnnotatedTypeMirror atm) {
    if (atm == null) {
      return null;
    }
    boolean isCollectionType = ResourceLeakUtils.isCollection(atm.getUnderlyingType());

    AnnotatedTypeMirror componentType = null;
    if (isCollectionType) {
      List<? extends AnnotatedTypeMirror> typeArgs =
          ((AnnotatedDeclaredType) atm).getTypeArguments();
      if (typeArgs.size() == 1) {
        componentType = typeArgs.get(0);
      }
    }

    if (componentType != null) {
      List<String> list = ResourceLeakUtils.getMcValues(componentType, mcAtf);
      return list;
    } else {
      return null;
    }
  }

  /**
   * If the given tree represents a collection, this method returns the MustCall values of its
   * elements. It returns null if there are none or if the given type is not a collection.
   *
   * <p>That is, if the given tree is of a Java.util.Collection implementation, this method returns
   * the MustCall values of its type variable upper bound if there are any or else null.
   *
   * @param tree the AST tree
   * @return if the given tree represents a collection, returns the MustCall values of its elements
   *     or null if there are none or if the given type is not a collection
   */
  public List<String> getMustCallValuesOfResourceCollectionComponent(Tree tree) {
    return getMustCallValuesOfResourceCollectionComponent(mcAtf.getAnnotatedType(tree));
  }

  /**
   * If the given type is a collection, this method returns the MustCall values of its elements. It
   * returns null if there are none or if the given type is not a collection.
   *
   * <p>That is, if the given type is a Java.util.Collection implementation, this method returns the
   * MustCall values of its type variable upper bound if there are any or else null.
   *
   * @param t the TypeMirror
   * @return if the given type is a collection, returns the MustCall values of its elements or null
   *     if there are none or if the given type is not a collection
   */
  public List<String> getMustCallValuesOfResourceCollectionComponent(TypeMirror t) {
    boolean isCollectionType = ResourceLeakUtils.isCollection(t);

    TypeMirror componentType = null;
    if (isCollectionType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) t).getTypeArguments();
      if (typeArgs.size() == 1) {
        componentType = typeArgs.get(0);
      }
    }

    if (componentType != null) {
      List<String> list = ResourceLeakUtils.getMcValues(componentType, mcAtf);
      return list;
    } else {
      return null;
    }
  }

  /**
   * Returns the flow-sensitive {@code CollectionOwnershipType} that the given node has in the given
   * store.
   *
   * @param node the node
   * @param coStore the store
   * @return the {@code CollectionOwnershipType} that the given node has in the given store
   */
  public CollectionOwnershipType getCoType(Node node, @Nullable CollectionOwnershipStore coStore) {
    if (coStore == null) {
      return null;
    }
    JavaExpression jx = JavaExpression.fromNode(node);
    CFValue storeVal;
    try {
      storeVal = coStore.getValue(jx);
    } catch (BugInCF e) {
      storeVal = null;
    }
    if (storeVal == null) {
      return null;
    }
    return getCoType(storeVal.getAnnotations());
  }

  /**
   * Returns the flow-sensitive {@code CollectionOwnershipType} of the given tree.
   *
   * @param tree the tree
   * @return the {@code CollectionOwnershipType} that the given tree has
   */
  public CollectionOwnershipType getCoType(Tree tree) {
    JavaExpression jx = null;
    if (tree instanceof ExpressionTree expressionTree) {
      jx = JavaExpression.fromTree(expressionTree);
    } else if (tree instanceof VariableTree variableTree) {
      jx = JavaExpression.fromVariableTree(variableTree);
    }
    try {
      CollectionOwnershipStore coStore = getStoreBefore(tree);
      CFValue storeVal = coStore.getValue(jx);
      return getCoType(storeVal.getAnnotations());
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Utility method to extract the {@code CollectionOwnershipType} from a collection of {@code
   * AnnotationMirror}s.
   *
   * @param annos the {@code AnnotationMirror} collection
   * @return the extracted {@code CollectionOwnershipType} from annos
   */
  public CollectionOwnershipType getCoType(Collection<? extends AnnotationMirror> annos) {
    for (AnnotationMirror anm : annos) {
      if (anm == null) {
        continue;
      }
      if (AnnotationUtils.areSame(anm, NOTOWNINGCOLLECTION)) {
        return CollectionOwnershipType.NotOwningCollection;
      } else if (AnnotationUtils.areSame(anm, OWNINGCOLLECTION)) {
        return CollectionOwnershipType.OwningCollection;
      } else if (AnnotationUtils.areSame(anm, OWNINGCOLLECTIONWITHOUTOBLIGATION)) {
        return CollectionOwnershipType.OwningCollectionWithoutObligation;
      } else if (AnnotationUtils.areSame(anm, BOTTOM)) {
        return CollectionOwnershipType.OwningCollectionBottom;
      }
    }
    return null;
  }

  /**
   * Returns the field names in the {@code @CollectionFieldDestructor} annotation that the given
   * method has or an empty list if there is no such annotation.
   *
   * @param method the method
   * @return the field names in the method's {@code @CollectionFieldDestructor} annotation, or an
   *     empty list if there is no such annotation
   */
  public List<String> getCollectionFieldDestructorAnnoFields(ExecutableElement method) {
    AnnotationMirror collectionFieldDestructorAnno =
        getDeclAnnotation(method, CollectionFieldDestructor.class);
    if (collectionFieldDestructorAnno == null) {
      return new ArrayList<String>();
    }
    return AnnotationUtils.getElementValueArray(
        collectionFieldDestructorAnno, collectionFieldDestructorValueElement, String.class);
  }

  /**
   * Returns true if the given expression {@code e} refers to {@code this.field}.
   *
   * @param e the expression
   * @param field the field
   * @return true if {@code e} refers to {@code this.field}
   */
  public boolean expressionIsFieldAccess(String e, VariableElement field) {
    try {
      JavaExpression je = StringToJavaExpression.atFieldDecl(e, field, this.checker);
      return je instanceof FieldAccess fieldAccess && fieldAccess.getField().equals(field);
    } catch (JavaExpressionParseException ex) {
      // The parsing error will be reported elsewhere, assuming e was derived from an
      // annotation.
      return false;
    }
  }

  /**
   * Returns a JavaExpression for the given String. Returns null if string is not parsable as a Java
   * expression.
   *
   * @param s the string
   * @param method the method with the annotation
   * @return a JavaExpression for the given String, or null if the string is not parsable as a Java
   *     expression
   */
  public JavaExpression stringToJavaExpression(String s, ExecutableElement method) {
    Tree methodTree = declarationFromElement(method);
    if (methodTree instanceof MethodTree mit) {
      try {
        return StringToJavaExpression.atMethodBody(s, mit, checker);
      } catch (JavaExpressionParseException ex) {
        return null;
      }
    }
    return null;
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(
        super.createTypeAnnotator(), new CollectionOwnershipTypeAnnotator(this));
  }

  /**
   * The TypeAnnotator for the Collection Ownership type system.
   *
   * <p>This TypeAnnotator defaults resource collection return types to OwningCollection, and
   * resource collection parameters to NotOwningCollection.
   */
  private class CollectionOwnershipTypeAnnotator extends TypeAnnotator {

    /**
     * Creates a CollectionOwnershipTypeAnnotator.
     *
     * @param atypeFactory the type factory
     */
    public CollectionOwnershipTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedExecutableType t, Void p) {

      AnnotatedDeclaredType receiverType = t.getReceiverType();
      AnnotationMirror receiverAnno =
          receiverType == null ? null : receiverType.getAnnotationInHierarchy(TOP);
      boolean receiverHasExplicitAnno =
          receiverAnno != null && !AnnotationUtils.areSameByName(BOTTOM, receiverAnno);

      AnnotatedTypeMirror returnType = t.getReturnType();
      AnnotationMirror returnAnno = returnType.getAnnotationInHierarchy(TOP);
      boolean returnHasExplicitAnno =
          returnAnno != null && !AnnotationUtils.areSameByName(BOTTOM, returnAnno);

      // inherit supertype annotations

      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
          AnnotatedTypes.overriddenMethods(
              elements, CollectionOwnershipAnnotatedTypeFactory.this, t.getElement());

      if (overriddenMethods != null) {
        for (ExecutableElement superElt : overriddenMethods.values()) {
          AnnotatedExecutableType annotatedSuperMethod =
              CollectionOwnershipAnnotatedTypeFactory.this.getAnnotatedType(superElt);

          if (!receiverHasExplicitAnno) {
            AnnotatedDeclaredType superReceiver = annotatedSuperMethod.getReceiverType();
            AnnotationMirror superReceiverAnno = superReceiver.getPrimaryAnnotationInHierarchy(TOP);
            boolean superReceiverHasExplicitAnno =
                superReceiverAnno != null
                    && !AnnotationUtils.areSameByName(BOTTOM, superReceiverAnno)
                    && !AnnotationUtils.areSameByName(POLY, superReceiverAnno);
            if (superReceiverHasExplicitAnno) {
              receiverType.replaceAnnotation(superReceiverAnno);
            }
          }

          if (!returnHasExplicitAnno) {
            AnnotatedTypeMirror superReturnType = annotatedSuperMethod.getReturnType();
            AnnotationMirror superReturnAnno = superReturnType.getPrimaryAnnotationInHierarchy(TOP);
            boolean superReturnHasExplicitAnno =
                superReturnAnno != null
                    && !AnnotationUtils.areSameByName(BOTTOM, superReturnAnno)
                    && !AnnotationUtils.areSameByName(POLY, superReturnAnno);
            if (superReturnHasExplicitAnno) {
              returnType.replaceAnnotation(superReturnAnno);
            }
          }

          List<? extends AnnotatedTypeMirror> paramTypes = t.getParameterTypes();
          List<? extends AnnotatedTypeMirror> superParamTypes =
              annotatedSuperMethod.getParameterTypes();
          for (int i = 0; i < superParamTypes.size(); i++) {
            AnnotationMirror paramAnno = paramTypes.get(i).getAnnotationInHierarchy(TOP);
            boolean paramHasExplicitAnno =
                paramAnno != null && !AnnotationUtils.areSameByName(BOTTOM, paramAnno);
            if (!paramHasExplicitAnno) {
              AnnotationMirror superParamAnno =
                  superParamTypes.get(i).getPrimaryAnnotationInHierarchy(TOP);
              boolean superParamHasExplicitAnno =
                  superParamAnno != null
                      && !AnnotationUtils.areSameByName(BOTTOM, superParamAnno)
                      && !AnnotationUtils.areSameByName(POLY, superParamAnno);
              if (superParamHasExplicitAnno) {
                paramTypes.get(i).replaceAnnotation(superParamAnno);
              }
            }
          }
        }
      } // end "if (overriddenMethods != null)"

      if (isResourceCollection(returnType.getUnderlyingType())) {
        AnnotationMirror manualAnno = returnType.getAnnotationInHierarchy(TOP);
        if (manualAnno == null || AnnotationUtils.areSameByName(BOTTOM, manualAnno)) {
          boolean isConstructor = t.getElement().getKind() == ElementKind.CONSTRUCTOR;
          if (isConstructor) {
            returnType.replaceAnnotation(OWNINGCOLLECTIONWITHOUTOBLIGATION);
          } else {
            returnType.replaceAnnotation(OWNINGCOLLECTION);
          }
        }
      }

      for (AnnotatedTypeMirror paramType : t.getParameterTypes()) {
        if (isResourceCollection(paramType.getUnderlyingType())) {
          AnnotationMirror manualAnno = paramType.getAnnotationInHierarchy(TOP);
          if (manualAnno == null || AnnotationUtils.areSameByName(BOTTOM, manualAnno)) {
            paramType.replaceAnnotation(NOTOWNINGCOLLECTION);
          }
        }
      }

      return super.visitExecutable(t, p);
    }
  }

  /*
   * Defaults resource collection field uses within member methods to @OwningCollection.
   */
  @Override
  protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    super.addComputedTypeAnnotations(tree, type, iUseFlow);

    if (type.getKind() == TypeKind.DECLARED) {
      Element elt = TreeUtils.elementFromTree(tree);
      if (elt != null) {
        boolean isField = elt.getKind() == ElementKind.FIELD;
        if (isField && isResourceCollection(type.getUnderlyingType())) {
          AnnotationMirror fieldAnno = type.getAnnotationInHierarchy(TOP);
          if (fieldAnno == null || AnnotationUtils.areSameByName(BOTTOM, fieldAnno)) {
            TreePath currentPath = getPath(tree);
            MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(currentPath);
            if (enclosingMethodTree != null && TreeUtils.isConstructor(enclosingMethodTree)) {
              type.replaceAnnotation(OWNINGCOLLECTIONWITHOUTOBLIGATION);
            } else {
              type.replaceAnnotation(OWNINGCOLLECTION);
            }
          }
        }
      }
    }
  }

  // Default resource collection fields to @OwningCollection and resource collection parameters
  // to @NotOwningCollection (inside the method).
  //
  // A resource collection is a `Iterable` or `Iterator`, whose component has non-empty
  // @MustCall type, as defined by the predicate isResourceCollection(AnnotatedTypeMirror).
  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);

    if (elt instanceof VariableElement) {
      if (isResourceCollection(type.getUnderlyingType())) {
        if (elt.getKind() == ElementKind.FIELD) {
          AnnotationMirror fieldAnno = type.getAnnotationInHierarchy(TOP);
          if (fieldAnno == null || AnnotationUtils.areSameByName(BOTTOM, fieldAnno)) {
            type.replaceAnnotation(OWNINGCOLLECTION);
          }
        } else if (elt.getKind() == ElementKind.PARAMETER) {
          // Propagate the parameter annotation to the use site.
          ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
          AnnotatedExecutableType annotatedMethod =
              CollectionOwnershipAnnotatedTypeFactory.this.getAnnotatedType(method);
          List<? extends VariableElement> params = method.getParameters();
          List<? extends AnnotatedTypeMirror> paramTypes = annotatedMethod.getParameterTypes();
          for (int i = 0; i < params.size(); i++) {
            if (params.get(i).getSimpleName() == elt.getSimpleName()) {
              type.replaceAnnotation(paramTypes.get(i).getAnnotationInHierarchy(TOP));
              break;
            }
          }
        }
      } else if (elt.getKind() == ElementKind.LOCAL_VARIABLE) {
        // Non-resource locals such as "Socket sock = null" can be reconstructed
        // as @NotOwningCollection at identifier use sites because of the defaulting path. For
        // plain non-collection locals, collection-ownership qualifiers are not meaningful, so
        // override that fallback here and keep them at bottom.
        AnnotationMirror localAnno = type.getAnnotationInHierarchy(TOP);
        if (localAnno == null || AnnotationUtils.areSameByName(TOP, localAnno)) {
          type.replaceAnnotation(BOTTOM);
        }
      }
    }
  }
}
