package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
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
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.MustCallInference;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.dataflow.expression.JavaExpressionParseUtil;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis.FieldInitialValue;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The annotated type factory for the Collection Ownership Checker. As the last checker in the
 * Resource Leak Checker hierarchy, its {@link #postAnalyze(ControlFlowGraph)} method checks that
 * must-call obligations are fulfilled.
 */
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

  /**
   * Map from a loop's {@linkplain DisposalLoopInfo#loopConditionalBlock() conditional block} to its
   * {@link DisposalLoopInfo}.
   */
  private final IdentityHashMap<ConditionalBlock, DisposalLoopInfo>
      conditionalBlockToDisposalLoopInfoMap = new IdentityHashMap<>();

  /**
   * Map from a {@link DisposalLoopInfo} to the methods that the loop body definitely calls on the
   * iterated element, as a Called Methods type.
   */
  private final IdentityHashMap<DisposalLoopInfo, AnnotationMirror>
      disposalLoopInfoToCalledMethodsMap = new IdentityHashMap<>();

  /**
   * Map from a {@code MethodTree} to the {@link DisposalLoopInfo}s discovered in that method's
   * body.
   */
  private final IdentityHashMap<MethodTree, Set<DisposalLoopInfo>> disposalLoopInfosByMethod =
      new IdentityHashMap<>();

  /**
   * The {@code @}{@link NotOwningCollection} annotation. Used only as an argument to {@code
   * getAnnotationInHierarchy} and {@code getPrimaryAnnotationInHierarchy}.
   */
  public final AnnotationMirror TOP;

  /** The {@code @}{@link NotOwningCollection} annotation. Equals TOP. */
  public final AnnotationMirror NOTOWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollection} annotation. */
  public final AnnotationMirror OWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollectionWithoutObligation} annotation. */
  public final AnnotationMirror OWNINGCOLLECTIONWITHOUTOBLIGATION;

  /** The {@code @}{@link OwningCollectionBottom} annotation. */
  public final AnnotationMirror BOTTOM;

  /** The {@code @}{@link PolyOwningCollection} polymorphic annotation. */
  public final AnnotationMirror POLY;

  /** The {@code value} element of the {@code @}{@link CollectionFieldDestructor} annotation. */
  private final ExecutableElement collectionFieldDestructorValueElement =
      TreeUtils.getMethod(CollectionFieldDestructor.class, "value", 0, processingEnv);

  /** The {@code element} element of the {@code @}{@link CreatesCollectionObligation} annotation. */
  private final ExecutableElement createsCollectionObligationElementElement =
      TreeUtils.getMethod(CreatesCollectionObligation.class, "element", 0, processingEnv);

  /** The {@code Iterator.hasNext()} method. */
  private final ExecutableElement iteratorHasNext =
      TreeUtils.getMethod("java.util.Iterator", "hasNext", 0, processingEnv);

  /** The {@code Collection.size()} and {@code Map.size()} methods. */
  private final List<ExecutableElement> sizeMethods =
      List.of(
          TreeUtils.getMethod("java.util.Collection", "size", 0, processingEnv),
          TreeUtils.getMethod("java.util.Map", "size", 0, processingEnv));

  /**
   * Method CFGs whose resource-leak post-analysis already ran after the first method analysis and
   * before any contained lambdas were analyzed.
   */
  private final Set<ControlFlowGraph> preLambdaPostAnalyzedMethods =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * The qualifiers of the collection ownership hierarchy. The {@code getCoType} methods return the
   * qualifier of a node in a store, of a tree, or in a set of annotations as one of these
   * constants, so that callers can {@code switch} on it or compare it with {@code ==}.
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
   * The leading space ensures this cannot coincide with an actual method name.
   */
  public static final String UNKNOWN_METHOD_NAME = " UNKNOWN";

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
   * @return the {@link DisposalLoopInfo} for condition {@code tree}, or {@code null} if {@link
   *     #postCFGConstruction} has not recorded one
   */
  public @Nullable DisposalLoopInfo getDisposalLoopInfoForConditionTree(Tree tree) {
    return conditionToDisposalLoopInfoMap.get(tree);
  }

  /**
   * Returns the {@link DisposalLoopInfo} whose {@linkplain DisposalLoopInfo#loopConditionalBlock()
   * conditional block} is {@code block}, if there is one.
   *
   * @param block a conditional block
   * @return the {@link DisposalLoopInfo} for {@code block}, or {@code null} if {@link
   *     #postCFGConstruction} has not recorded one
   */
  public @Nullable DisposalLoopInfo getDisposalLoopInfoForConditionalBlock(ConditionalBlock block) {
    return conditionalBlockToDisposalLoopInfoMap.get(block);
  }

  /**
   * Returns true if the body of {@code disposalLoopInfo} definitely calls all of {@code methods} on
   * every iterated element.
   *
   * @param disposalLoopInfo a disposal loop
   * @param methods the names of the methods to check, such as the must-call methods of the
   *     collection's elements
   * @return true if the loop body definitely calls all of {@code methods} on every iterated
   *     element; false if it does not, or if {@link #postCFGConstruction} has not recorded the loop
   */
  public boolean callsOnEveryElement(DisposalLoopInfo disposalLoopInfo, List<String> methods) {
    AnnotationMirror calledMethods = disposalLoopInfoToCalledMethodsMap.get(disposalLoopInfo);
    if (calledMethods == null) {
      return false;
    }
    RLCCalledMethodsAnnotatedTypeFactory rlAtf =
        ResourceLeakUtils.getRLCCalledMethodsAnnotatedTypeFactory(this);
    return rlAtf
        .getQualifierHierarchy()
        .isSubtypeQualifiersOnly(
            calledMethods, rlAtf.createCalledMethods(methods.toArray(new String[0])));
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
   * Records a {@link DisposalLoopInfo} and the methods its loop body is guaranteed to call on the
   * iterated element, indexed by the loop's condition tree and its conditional block.
   *
   * @param disposalLoopInfo the {@link DisposalLoopInfo}
   * @param calledMethods the methods that the loop body definitely calls on the iterated element,
   *     as a Called Methods type
   */
  private void recordDisposalLoop(
      DisposalLoopInfo disposalLoopInfo, AnnotationMirror calledMethods) {
    conditionToDisposalLoopInfoMap.put(disposalLoopInfo.loopConditionTree(), disposalLoopInfo);
    conditionalBlockToDisposalLoopInfoMap.put(
        disposalLoopInfo.loopConditionalBlock(), disposalLoopInfo);
    disposalLoopInfoToCalledMethodsMap.put(disposalLoopInfo, calledMethods);
  }

  /**
   * Returns all the disposal loops in the given method CFG.
   *
   * @param cfg the CFG to scan
   * @return the {@link DisposalLoopInfo}s discovered in {@code cfg}
   */
  private Set<DisposalLoopInfo> scanForDisposalLoopInfos(ControlFlowGraph cfg) {
    if (cfg.getUnderlyingAST().getKind() != UnderlyingAST.Kind.METHOD) {
      return Collections.emptySet();
    }
    return new DisposalLoopScanner(this, cfg)
        .scanTree(((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod());
  }

  /**
   * Discovers and stores the {@link DisposalLoopInfo}s for a method {@code cfg}. This discovery
   * must happen before {@link
   * org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsTransfer#initialStore(UnderlyingAST,
   * List)} runs, which adds an {@code IteratedCollectionElement} for the element of each loop.
   *
   * @param cfg the method CFG whose disposal loops to be discovered
   */
  public void discoverDisposalLoopInfos(ControlFlowGraph cfg) {
    MethodTree methodTree = getMethodTree(cfg.getUnderlyingAST());
    if (methodTree == null) {
      return;
    }
    disposalLoopInfosByMethod.put(methodTree, new LinkedHashSet<>(scanForDisposalLoopInfos(cfg)));
  }

  /**
   * Returns the {@link DisposalLoopInfo}s for the given underlying AST.
   *
   * @param underlyingAST the underlying AST whose disposal loops should be returned
   * @return an unmodifiable view of the disposal loops for {@code underlyingAST}
   */
  public Set<DisposalLoopInfo> getDisposalLoopInfos(UnderlyingAST underlyingAST) {
    MethodTree methodTree = getMethodTree(underlyingAST);
    if (methodTree == null) {
      return Collections.emptySet();
    }
    Set<DisposalLoopInfo> disposalLoopInfos = disposalLoopInfosByMethod.get(methodTree);
    if (disposalLoopInfos == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(disposalLoopInfos);
  }

  /**
   * Removes the {@link DisposalLoopInfo}s for the given underlying AST.
   *
   * @param underlyingAST the underlying AST whose disposal loops should be removed
   */
  private void removeDisposalLoopInfos(UnderlyingAST underlyingAST) {
    MethodTree methodTree = getMethodTree(underlyingAST);
    if (methodTree != null) {
      disposalLoopInfosByMethod.remove(methodTree);
    }
  }

  /**
   * Returns the method tree of the given underlying AST, if it is a method CFG.
   *
   * @param underlyingAST the underlying AST
   * @return the method tree, or {@code null} if {@code underlyingAST} is not a method
   */
  private @Nullable MethodTree getMethodTree(UnderlyingAST underlyingAST) {
    if (underlyingAST.getKind() != UnderlyingAST.Kind.METHOD) {
      return null;
    }
    return ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
  }

  /**
   * Computes the called methods of this method's potential disposal loops, which {@link
   * RLCCalledMethodsAnnotatedTypeFactory#postCFGConstruction} discovered earlier. For each loop,
   * {@link MustCallConsistencyAnalyzer#analyzeDisposalLoop} computes the methods that the loop body
   * is guaranteed to call on the iterated element, and this method records them.
   *
   * <p>This runs before this checker's dataflow analysis of the method. During that analysis,
   * {@link CollectionOwnershipTransfer} certifies a recorded loop if its called methods cover the
   * iterated element's must-call methods. Later, the Resource Leak Checker's {@link
   * MustCallConsistencyAnalyzer} uses the recorded loops when it checks collection obligations.
   */
  @Override
  protected void postCFGConstruction(ControlFlowGraph cfg, UnderlyingAST ast) {
    if (ast.getKind() != UnderlyingAST.Kind.METHOD) {
      return;
    }

    Set<DisposalLoopInfo> disposalLoopInfos = getDisposalLoopInfos(ast);
    removeDisposalLoopInfos(ast);
    if (disposalLoopInfos.isEmpty()) {
      return;
    }

    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(ResourceLeakUtils.getResourceLeakChecker(this), true);
    for (DisposalLoopInfo disposalLoopInfo : disposalLoopInfos) {
      AnnotationMirror calledMethods =
          mustCallConsistencyAnalyzer.analyzeDisposalLoop(cfg, disposalLoopInfo);
      if (calledMethods != null) {
        recordDisposalLoop(disposalLoopInfo, calledMethods);
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
  public void setRoot(@Nullable CompilationUnitTree root) {
    super.setRoot(root);
    // postCFGConstruction removes each method's entry for disposalLoopInfosByMethod; clear the
    // remaining fields at each compilation unit.
    conditionToDisposalLoopInfoMap.clear();
    conditionalBlockToDisposalLoopInfoMap.clear();
    disposalLoopInfoToCalledMethodsMap.clear();
    preLambdaPostAnalyzedMethods.clear();
  }

  @Override
  protected ControlFlowGraph analyze(
      Queue<ClassAndStore<CollectionOwnershipStore>> classQueue,
      Queue<LambdaAndStore<CollectionOwnershipStore>> lambdaQueue,
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
      // Run the resource-leak post-analysis now, after the method's first analysis and before any
      // lambdas in it are reanalyzed (see RLCCalledMethodsAnnotatedTypeFactory#analyze).
      // postAnalyze skips the methods recorded here.
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
    assert root != null : "@AssumeAssertion(nullness): at this point root is always nonnull";
    rlc.setRoot(root);
    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(rlc, false);
    mustCallConsistencyAnalyzer.analyze(cfg);
    RLCCalledMethodsAnnotatedTypeFactory cmAtf =
        (RLCCalledMethodsAnnotatedTypeFactory)
            ResourceLeakUtils.getRLCCalledMethodsChecker(this).getTypeFactory();
    // Inferring owning annotations for @Owning fields/parameters, @EnsuresCalledMethods for
    // finalizer methods, and @InheritableMustCall annotations for the class declarations.
    // MustCallInference writes to this factory's WPI, so check that one.
    if (getWholeProgramInference() != null) {
      if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
        MustCallInference.runMustCallInference(cmAtf, cfg, mustCallConsistencyAnalyzer);
      }
    }
  }

  /**
   * Returns true if the given type is a resource collection: a collection (see {@link
   * ResourceLeakUtils#isCollection}) with exactly one type argument, whose {@code @MustCall} type
   * is non-empty.
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
  public boolean isOwningCollectionField(@Nullable Element elt) {
    if (elt == null || !elt.getKind().isField()) {
      return false;
    }
    if (isResourceCollection(elt.asType())) {
      AnnotationMirror anno = getAnnotatedType(elt).getPrimaryAnnotationInHierarchy(TOP);
      return anno != null
          && (AnnotationUtils.areSame(anno, OWNINGCOLLECTION)
              || AnnotationUtils.areSame(anno, OWNINGCOLLECTIONWITHOUTOBLIGATION));
    }
    return false;
  }

  /**
   * Returns true if the given expression evaluates to an owning collection field, as defined by
   * {@link #isOwningCollectionField(Element)}. The field may be in parentheses or cast, or it may
   * be either branch of a conditional expression.
   *
   * @param expr an expression
   * @return true if {@code expr} evaluates to an owning collection field
   */
  public boolean isOwningCollectionField(ExpressionTree expr) {
    expr = TreeUtils.withoutParensOrCasts(expr);
    if (expr instanceof ConditionalExpressionTree conditional) {
      return isOwningCollectionField(conditional.getTrueExpression())
          || isOwningCollectionField(conditional.getFalseExpression());
    }
    return isOwningCollectionField(TreeUtils.elementFromTree(expr));
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
   * Returns true if the given element is a resource collection parameter whose type is
   * {@code @OwningCollection}, written on it or inherited from an overridden method. A parameter
   * with no collection ownership annotation is {@code @NotOwningCollection}.
   *
   * @param elt an element
   * @return true if the element is a resource collection parameter whose type is
   *     {@code @OwningCollection}
   */
  public boolean isOwningCollectionParameter(Element elt) {
    if (elt.getKind() != ElementKind.PARAMETER) {
      throw new BugInCF("Not a parameter: " + elt);
    }
    if (!isResourceCollection(elt.asType())) {
      return false;
    }
    AnnotationMirror anno = getAnnotatedType(elt).getPrimaryAnnotationInHierarchy(TOP);
    return anno != null && AnnotationUtils.areSame(anno, OWNINGCOLLECTION);
  }

  /**
   * Returns true if the given AST tree is a resource collection: a collection (see {@link
   * ResourceLeakUtils#isCollection}) with exactly one type argument, whose {@code @MustCall} type
   * is non-empty.
   *
   * @param tree the tree
   * @return true if the tree is a resource collection
   */
  public boolean isResourceCollection(Tree tree) {
    if (tree == null) {
      return false;
    }
    List<String> mcValues = getMustCallValuesOfResourceCollectionComponent(tree);
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
   * Returns true if the given tree is an invocation of {@code Iterator.hasNext()} or of a method
   * that overrides it.
   *
   * @param tree a tree
   * @return true if {@code tree} is an invocation of {@code Iterator.hasNext()}
   */
  boolean isIteratorHasNextCall(Tree tree) {
    return TreeUtils.isMethodInvocation(tree, iteratorHasNext, processingEnv);
  }

  /**
   * Returns true if the given tree is an invocation of {@code Collection.size()}, {@code
   * Map.size()}, or a method that overrides one of them.
   *
   * @param tree a tree
   * @return true if {@code tree} is an invocation of {@code Collection.size()} or {@code
   *     Map.size()}
   */
  boolean isSizeCall(Tree tree) {
    return TreeUtils.isMethodInvocation(tree, sizeMethods, processingEnv);
  }

  /**
   * Returns the argument whose obligation is transferred by a {@code @CreatesCollectionObligation}
   * call, or {@code null} if the call has no such argument. See {@link
   * CreatesCollectionObligation#element}.
   *
   * @param tree a method invocation tree
   * @return the inserted argument tree, or null if there is none
   */
  public @Nullable ExpressionTree getInsertedArgumentTree(MethodInvocationTree tree) {
    List<? extends ExpressionTree> args = tree.getArguments();
    int index = getInsertedParameterIndex(TreeUtils.elementFromUse(tree));
    return index >= 0 && index < args.size() ? args.get(index) : null;
  }

  /**
   * Returns the argument whose obligation is transferred by a {@code @CreatesCollectionObligation}
   * call, or {@code null} if the call has no such argument. See {@link
   * CreatesCollectionObligation#element}.
   *
   * @param node a method invocation node
   * @return the inserted argument node, or null if there is none
   */
  public @Nullable Node getInsertedArgumentNode(MethodInvocationNode node) {
    List<Node> args = node.getArguments();
    int index = getInsertedParameterIndex(node.getTarget().getMethod());
    return index >= 0 && index < args.size() ? args.get(index) : null;
  }

  /**
   * Returns the {@code element} of the given method's {@code @CreatesCollectionObligation}
   * annotation, which names the inserted parameter, or the empty string if it names none.
   *
   * @param method a method, which may inherit {@code @CreatesCollectionObligation}
   * @return the {@code element} of the method's {@code @CreatesCollectionObligation} annotation
   */
  public String getInsertedParameterReference(ExecutableElement method) {
    AnnotationMirror anno = getDeclAnnotation(method, CreatesCollectionObligation.class);
    return anno == null
        ? ""
        : AnnotationUtils.getElementValue(
            anno, createsCollectionObligationElementElement, String.class, "");
  }

  /**
   * Returns the index of the parameter that the given {@code @CreatesCollectionObligation} method
   * inserts: the parameter named by the annotation's {@code element}, or else the last parameter.
   *
   * @param method a method, which may inherit {@code @CreatesCollectionObligation}
   * @return the index of the inserted parameter, or -1 if the method has no parameters or {@code
   *     element} does not name one
   */
  public int getInsertedParameterIndex(ExecutableElement method) {
    int numParams = method.getParameters().size();
    String element = getInsertedParameterReference(method);
    if (element.isEmpty()) {
      return numParams - 1;
    }
    int paramIndex = JavaExpressionParseUtil.parameterIndex(element) - 1;
    return paramIndex >= 0 && paramIndex < numParams ? paramIndex : -1;
  }

  /**
   * Returns true if the argument inserted by a {@code @CreatesCollectionObligation} call is the
   * null literal, even if it is cast.
   *
   * @param node a method invocation node
   * @return true if the inserted argument is {@code null}
   */
  public boolean insertsNull(MethodInvocationNode node) {
    Node inserted = getInsertedArgumentNode(node);
    return inserted != null && NodeUtils.removeCasts(inserted) instanceof NullLiteralNode;
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
  public CollectionMutatorArgumentKind getCollectionMutatorArgumentKind(
      @Nullable Tree insertedArgumentTree) {
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
    if (insertedElement != null && rlAtf.hasNotOwning(insertedElement)) {
      return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
    }

    // If the inserted element is a parameter, and it has no explicit @Owning annotation, then it's
    // definitely not owning.
    if (insertedElement != null && insertedElement.getKind() == ElementKind.PARAMETER) {
      return !rlAtf.hasOwning(insertedElement)
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
    if (mustCallType.hasPrimaryAnnotation(NotOwning.class)) {
      return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
    }

    TypeMirror typeMirror = TreeUtils.typeOf(insertedArgumentTree);
    if (typeMirror.getKind() == TypeKind.NULL) {
      return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
    }
    // Use the argument's effective must-call type rather than the declaration of its class:  an
    // expression of type `@MustCallUnknown Object` may be owning even though `Object` has no
    // must-call obligations.  A missing @MustCall annotation means @MustCallUnknown.
    AnnotationMirror mustCallAnno = mustCallType.getPrimaryAnnotation(MustCall.class);
    if (mustCallAnno != null && rlAtf.getMustCallValues(mustCallAnno).isEmpty()) {
      return CollectionMutatorArgumentKind.DEFINITELY_NON_OWNING;
    }

    return CollectionMutatorArgumentKind.MAY_BE_OWNING;
  }

  /**
   * Returns the must-call methods of the elements of the given collection type. For example, it
   * returns {@code ["close"]} for {@code List<Socket>}. Returns null if the type is not a
   * collection, or if the must-call methods of its elements cannot be determined.
   *
   * @param atm a type, or null if the type is not available
   * @return the must-call methods of the elements of {@code atm}, or null
   */
  public @Nullable List<String> getMustCallValuesOfResourceCollectionComponent(
      @Nullable AnnotatedTypeMirror atm) {
    if (atm == null) {
      return null;
    }
    AnnotatedTypeMirror annotatedElementType =
        ResourceLeakUtils.getCollectionElementTypeArgument(atm, processingEnv);
    if (annotatedElementType != null) {
      return ResourceLeakUtils.getMcValues(annotatedElementType, mcAtf);
    }
    return getMustCallValuesOfResourceCollectionComponent(atm.getUnderlyingType());
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
  public @Nullable List<String> getMustCallValuesOfResourceCollectionComponent(Tree tree) {
    if (!(TreeUtils.isExpressionTree(tree) || tree instanceof VariableTree)) {
      return null;
    }
    return getMustCallValuesOfResourceCollectionComponent(mcAtf.getAnnotatedType(tree));
  }

  /**
   * Returns the must-call methods of the elements of the given collection type. For example, it
   * returns {@code ["close"]} for {@code List<Socket>}. Returns null if the type is not a
   * collection, or if the must-call methods of its elements cannot be determined.
   *
   * @param t a type
   * @return the must-call methods of the elements of {@code t}, or null
   */
  public @Nullable List<String> getMustCallValuesOfResourceCollectionComponent(TypeMirror t) {
    TypeMirror elementType = ResourceLeakUtils.getCollectionElementType(t, processingEnv);
    return elementType == null ? null : ResourceLeakUtils.getMcValues(elementType, mcAtf);
  }

  /**
   * Returns the flow-sensitive {@code CollectionOwnershipType} that the given node has in the given
   * store.
   *
   * @param node the node
   * @param coStore the store
   * @return the {@code CollectionOwnershipType} that the given node has in the given store, or
   *     {@code null} if no information is available
   */
  public @Nullable CollectionOwnershipType getCoType(
      Node node, @Nullable CollectionOwnershipStore coStore) {
    if (coStore == null) {
      return null;
    }
    JavaExpression jx = JavaExpression.fromNode(node);
    if (!CFAbstractStore.canInsertJavaExpression(jx)) {
      return null;
    }
    CFValue storeVal = coStore.getValue(jx);
    if (storeVal == null) {
      return null;
    }
    return getCoType(storeVal.getAnnotations());
  }

  /**
   * Returns the flow-sensitive {@code CollectionOwnershipType} of the given tree.
   *
   * @param tree the tree
   * @return the {@code CollectionOwnershipType} that the given tree has, or {@code null} if no
   *     information is available
   */
  public @Nullable CollectionOwnershipType getCoType(Tree tree) {
    JavaExpression jx;
    if (tree instanceof ExpressionTree expressionTree) {
      jx = JavaExpression.fromTree(expressionTree);
    } else if (tree instanceof VariableTree variableTree) {
      jx = JavaExpression.fromVariableTree(variableTree);
    } else {
      return null;
    }
    if (!CFAbstractStore.canInsertJavaExpression(jx)) {
      return null;
    }
    CollectionOwnershipStore coStore = getStoreBefore(tree);
    if (coStore == null) {
      return null;
    }
    CFValue storeVal = coStore.getValue(jx);
    if (storeVal == null) {
      return null;
    }
    return getCoType(storeVal.getAnnotations());
  }

  /**
   * Utility method to extract the {@code CollectionOwnershipType} from a collection of {@code
   * AnnotationMirror}s.
   *
   * @param annos the {@code AnnotationMirror} collection; null elements are ignored
   * @return the extracted {@code CollectionOwnershipType} from annos, or {@code null} if no element
   *     of annos corresponds to a {@code CollectionOwnershipType}
   */
  public @Nullable CollectionOwnershipType getCoType(
      Collection<? extends @Nullable AnnotationMirror> annos) {
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
      return Collections.emptyList();
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

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(
        super.createTypeAnnotator(), new CollectionOwnershipTypeAnnotator(this));
  }

  @Override
  protected QualifierPolymorphism createQualifierPolymorphism() {
    return new CollectionOwnershipQualifierPolymorphism(processingEnv, this);
  }

  /**
   * Resolves {@code @PolyOwningCollection} to {@code @NotOwningCollection} where the default would
   * resolve it to {@code @OwningCollection} or {@code @OwningCollectionWithoutObligation}. See
   * {@link PolyOwningCollection}.
   */
  private class CollectionOwnershipQualifierPolymorphism extends DefaultQualifierPolymorphism {

    /**
     * Creates a {@link CollectionOwnershipQualifierPolymorphism}.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public CollectionOwnershipQualifierPolymorphism(
        ProcessingEnvironment env, AnnotatedTypeFactory factory) {
      super(env, factory);
    }

    @Override
    protected void replace(
        AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirror> replacements) {
      AnnotationMirror resolved = replacements.get(POLY);
      if (resolved == null
          || !(AnnotationUtils.areSame(OWNINGCOLLECTION, resolved)
              || AnnotationUtils.areSame(OWNINGCOLLECTIONWITHOUTOBLIGATION, resolved))) {
        super.replace(type, replacements);
        return;
      }
      AnnotationMirrorMap<AnnotationMirror> notOwning = new AnnotationMirrorMap<>(replacements);
      notOwning.put(POLY, NOTOWNINGCOLLECTION);
      super.replace(type, notOwning);
    }
  }

  /**
   * The TypeAnnotator for the Collection Ownership type system. It gives the receiver, return type,
   * and parameters of a method the collection ownership qualifiers that are not written:
   *
   * <ol>
   *   <li>An overriding method takes them from the methods it overrides, so that it follows the
   *       same contract.
   *   <li>A method that returns a resource collection gives the caller its obligations, so the
   *       return type is {@code @OwningCollection}. A constructor's return type is
   *       {@code @OwningCollectionWithoutObligation}.
   *   <li>A receiver or parameter that could be a resource collection is
   *       {@code @NotOwningCollection}: the method only borrows it, so a caller can pass any
   *       collection. A method that discards elements or takes ownership has to be annotated.
   * </ol>
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
      AnnotatedTypeMirror returnType = t.getReturnType();
      List<? extends AnnotatedTypeMirror> paramTypes = t.getParameterTypes();
      boolean returnsResourceCollection = isResourceCollection(returnType.getUnderlyingType());

      // Only the receiver of a collection class inherits a qualifier. The receiver of any other
      // class, such as one that overrides toString(), keeps its default.
      boolean inheritsReceiver =
          receiverType != null
              && ResourceLeakUtils.isCollection(receiverType.getUnderlyingType(), processingEnv);
      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
          AnnotatedTypes.overriddenMethods(
              elements, CollectionOwnershipAnnotatedTypeFactory.this, t.getElement());
      for (ExecutableElement overriddenElt : overriddenMethods.values()) {
        AnnotatedExecutableType overridden =
            CollectionOwnershipAnnotatedTypeFactory.this.getAnnotatedType(overriddenElt);
        if (inheritsReceiver) {
          AnnotatedDeclaredType overriddenReceiver = overridden.getReceiverType();
          assert receiverType != null && overriddenReceiver != null
              : "@AssumeAssertion(nullness): an overriding method and the method it overrides are instance methods";
          inheritQualifier(receiverType, overriddenReceiver);
        }
        AnnotatedTypeMirror overriddenReturnType = overridden.getReturnType();
        if (inheritQualifier(returnType, overriddenReturnType)
            && returnsResourceCollection
            && overriddenReturnType.hasPrimaryAnnotation(POLY)) {
          // An override of a @PolyOwningCollection method that returns a resource collection,
          // such as an iterator() method that returns the iterator of an owning field, returns
          // @NotOwningCollection: its result never owns the elements, it's a read-only view.
          returnType.replaceAnnotation(NOTOWNINGCOLLECTION);
        }
        List<? extends AnnotatedTypeMirror> overriddenParamTypes = overridden.getParameterTypes();
        for (int i = 0; i < overriddenParamTypes.size(); i++) {
          inheritQualifier(paramTypes.get(i), overriddenParamTypes.get(i));
        }
      }

      if (returnsResourceCollection && isMissingAnnotation(returnType)) {
        returnType.replaceAnnotation(
            t.getElement().getKind() == ElementKind.CONSTRUCTOR
                ? OWNINGCOLLECTIONWITHOUTOBLIGATION
                : OWNINGCOLLECTION);
      }

      if (receiverType != null) {
        borrow(receiverType);
      }
      for (AnnotatedTypeMirror paramType : paramTypes) {
        borrow(paramType);
      }

      return super.visitExecutable(t, p);
    }

    /**
     * Gives a type in an overriding method the qualifier of the corresponding type in an overridden
     * method, if the type has no qualifier and the overridden type has one.
     *
     * @param type the receiver, return, or a parameter type of the overriding method, which this
     *     method side-effects
     * @param overriddenType the corresponding type of the overridden method
     * @return true if {@code type} inherited the qualifier of {@code overriddenType}
     */
    private boolean inheritQualifier(AnnotatedTypeMirror type, AnnotatedTypeMirror overriddenType) {
      AnnotationMirror anno = type.getAnnotationInHierarchy(TOP);
      AnnotationMirror overriddenAnno = overriddenType.getPrimaryAnnotationInHierarchy(TOP);
      if (!isMissingAnnotation(anno) || isMissingAnnotation(overriddenAnno)) {
        return false;
      }
      // At a call, this runs again after the call has resolved @PolyOwningCollection, possibly to
      // bottom. So bottom does not inherit @PolyOwningCollection; only a missing qualifier does.
      if (anno != null && AnnotationUtils.areSameByName(POLY, overriddenAnno)) {
        return false;
      }
      type.replaceAnnotation(overriddenAnno);
      return true;
    }

    /**
     * Makes the given receiver or parameter type {@code @NotOwningCollection} if it could be a
     * resource collection and has no qualifier.
     *
     * @param type a receiver or parameter type, which this method side-effects
     */
    private void borrow(AnnotatedTypeMirror type) {
      if (couldBeResourceCollection(type.getUnderlyingType()) && isMissingAnnotation(type)) {
        type.replaceAnnotation(NOTOWNINGCOLLECTION);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Also resolves {@code @PolyOwningCollection} where the framework leaves it unresolved: in a
   * method reference, and in an invocation typed during type argument inference. For example,
   * without this, inference fails for {@code lists.stream().map(List::iterator)}: the return type
   * {@code @PolyOwningCollection Iterator<String>} does not fit {@code map}'s type argument.
   */
  @Override
  protected ParameterizedExecutableType methodFromUse(
      ExpressionTree tree,
      ExecutableElement methodElt,
      AnnotatedTypeMirror receiverType,
      boolean inferTypeArgs) {
    ParameterizedExecutableType result =
        super.methodFromUse(tree, methodElt, receiverType, inferTypeArgs);
    AnnotatedExecutableType methodType = result.executableType();
    if (tree instanceof MemberReferenceTree memberReference) {
      // list::iterator resolves as list.iterator() would; List::iterator, to bottom.
      boolean boundToNonBottomReceiver =
          TreeUtils.MemberReferenceKind.getMemberReferenceKind(memberReference)
                  == TreeUtils.MemberReferenceKind.BOUND
              && receiverType != null
              && !isMissingAnnotation(receiverType);
      replacePoly(methodType, boundToNonBottomReceiver ? NOTOWNINGCOLLECTION : BOTTOM);
    } else if (tree instanceof MethodInvocationTree && !inferTypeArgs) {
      // A stand-in for inference, typed again later: accept any argument, fit any type argument.
      if (methodType.getReceiverType() != null) {
        replacePoly(methodType.getReceiverType(), NOTOWNINGCOLLECTION);
      }
      for (AnnotatedTypeMirror paramType : methodType.getParameterTypes()) {
        replacePoly(paramType, NOTOWNINGCOLLECTION);
      }
      replacePoly(methodType.getReturnType(), BOTTOM);
    }
    return result;
  }

  /**
   * Replaces each {@code @PolyOwningCollection} in the given type by the given qualifier.
   *
   * @param type a type, which this method side-effects
   * @param replacement the qualifier that replaces {@code @PolyOwningCollection}
   */
  private void replacePoly(AnnotatedTypeMirror type, AnnotationMirror replacement) {
    new SimpleAnnotatedTypeScanner<Void, Void>(
            (t, p) -> {
              if (t.hasPrimaryAnnotation(POLY)) {
                t.replaceAnnotation(replacement);
              }
              return null;
            })
        .visit(type);
  }

  /**
   * Returns true if the given type has no collection ownership qualifier, or has the default
   * qualifier, bottom.
   *
   * @param type a type
   * @return true if the type has no collection ownership qualifier other than bottom
   */
  private boolean isMissingAnnotation(AnnotatedTypeMirror type) {
    return isMissingAnnotation(type.getAnnotationInHierarchy(TOP));
  }

  /**
   * Returns true if the given collection ownership qualifier is missing or is the default
   * qualifier, bottom.
   *
   * @param anno a collection ownership qualifier, or null if there is none
   * @return true if {@code anno} is null or bottom
   */
  @EnsuresNonNullIf(expression = "#1", result = false)
  private boolean isMissingAnnotation(@Nullable AnnotationMirror anno) {
    return anno == null || AnnotationUtils.areSameByName(BOTTOM, anno);
  }

  /**
   * Returns true if a value of the given type could be a resource collection. That is the case for
   * {@code Object}, and for a collection whose element type is a type variable, a wildcard, or a
   * type whose must-call methods are non-empty or unknown. For example, it is true for {@code
   * List<T>}, {@code List<?>}, and {@code List<Socket>}, and false for {@code List<String>}.
   *
   * @param type a type
   * @return true if a value of the given type could be a resource collection
   */
  private boolean couldBeResourceCollection(TypeMirror type) {
    if (TypesUtils.isObject(type)) {
      return true;
    }
    TypeMirror elementType = ResourceLeakUtils.getCollectionElementType(type, processingEnv);
    if (elementType == null) {
      return false;
    }
    if (elementType.getKind() == TypeKind.TYPEVAR || elementType.getKind() == TypeKind.WILDCARD) {
      return true;
    }
    List<String> mcValues = ResourceLeakUtils.getMcValues(elementType, mcAtf);
    return mcValues == null || !mcValues.isEmpty();
  }

  /**
   * {@inheritDoc}
   *
   * <p>In a class that cannot hold resources, {@code this} is bottom, so that the class's methods
   * can call each other. By default, {@code this} has the qualifier of the enclosing method's
   * receiver, and in such a class the receivers differ. For example, in
   *
   * <pre>{@code
   * class StringIterator implements Iterator<String> {
   *   private boolean inRange() { ... }
   *   public boolean hasNext() { return inRange(); }
   * }
   * }</pre>
   *
   * {@code hasNext()} inherits a {@code @NotOwningCollection} receiver from {@code
   * Iterator.hasNext()}, but {@code inRange()} has a bottom receiver, so the call {@code inRange()}
   * would be rejected.
   */
  @Override
  public @Nullable AnnotatedDeclaredType getSelfType(Tree tree) {
    AnnotatedDeclaredType selfType = super.getSelfType(tree);
    if (selfType == null || couldBeResourceCollection(selfType.getUnderlyingType())) {
      return selfType;
    }
    AnnotatedDeclaredType bottomSelfType = selfType.deepCopy();
    bottomSelfType.replaceAnnotation(BOTTOM);
    return bottomSelfType;
  }

  /*
   * Defaults a use of a resource collection field to @OwningCollection, or to
   * @OwningCollectionWithoutObligation in a constructor.
   */
  @Override
  protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    super.addComputedTypeAnnotations(tree, type, iUseFlow);

    if (type.getKind() != TypeKind.DECLARED) {
      return;
    }
    Element elt = TreeUtils.elementFromTree(tree);
    if (elt != null
        && elt.getKind() == ElementKind.FIELD
        && isResourceCollection(type.getUnderlyingType())
        && isMissingAnnotation(type)) {
      TreePath currentPath = getPath(tree);
      MethodTree enclosingMethodTree =
          currentPath == null ? null : TreePathUtil.enclosingMethod(currentPath);
      boolean inConstructor =
          enclosingMethodTree != null && TreeUtils.isConstructor(enclosingMethodTree);
      type.replaceAnnotation(inConstructor ? OWNINGCOLLECTIONWITHOUTOBLIGATION : OWNINGCOLLECTION);
    }
  }

  // Defaults resource collection fields to @OwningCollection, and gives a use of a resource
  // collection parameter in its method the parameter's qualifier.
  //
  // A resource collection is an Iterable or Iterator whose elements have a non-empty must-call
  // type; see isResourceCollection(TypeMirror).
  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);

    if (elt instanceof VariableElement) {
      if (isResourceCollection(type.getUnderlyingType())) {
        if (elt.getKind() == ElementKind.FIELD) {
          if (isMissingAnnotation(type)) {
            type.replaceAnnotation(OWNINGCOLLECTION);
          }
        } else if (elt.getKind() == ElementKind.PARAMETER) {
          ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
          int index = method.getParameters().indexOf(elt);
          if (index >= 0) {
            AnnotationMirror paramAnno =
                getAnnotatedType(method)
                    .getParameterTypes()
                    .get(index)
                    .getAnnotationInHierarchy(TOP);
            if (paramAnno != null) {
              type.replaceAnnotation(paramAnno);
            }
          }
        }
      } else if (elt.getKind() == ElementKind.LOCAL_VARIABLE) {
        // Locals default to top. Where dataflow has no value for a local, such as in a catch block
        // reached only through ignored exceptions, its declared type is used, and checks that do
        // not skip unreachable code, such as the thrown type of a throw statement, would fail.
        // Collection ownership is not meaningful for a non-collection local, so keep it at bottom.
        AnnotationMirror localAnno = type.getAnnotationInHierarchy(TOP);
        if (localAnno == null || AnnotationUtils.areSameByName(TOP, localAnno)) {
          type.replaceAnnotation(BOTTOM);
        }
      }
    }
  }
}
