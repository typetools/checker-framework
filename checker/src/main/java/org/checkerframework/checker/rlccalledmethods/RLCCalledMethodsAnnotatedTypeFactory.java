package org.checkerframework.checker.rlccalledmethods;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;

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
   * Set of potentially collection-obligation-fulfilling loops. Found in the MustCallVisitor, which
   * checks the header and (parts of the) body.
   */
  private static final Set<PotentiallyFulfillingLoop> potentiallyFulfillingLoops = new HashSet<>();

  /**
   * Construct a {@code PotentiallyFulfillingLoop} and add it to the static set of such loops to
   * have their loop body analyzed for possibly fulfilling collection obligations.
   *
   * @param collectionTree AST {@code Tree} for collection iterated over
   * @param collectionElementTree AST {@code Tree} for collection element iterated over
   * @param condition AST {@code Tree} for loop condition
   * @param loopBodyEntryBlock cfg {@code Block} for loop body entry
   * @param loopUpdateBlock {@code Block} containing loop update
   * @param loopConditionalBlock {@code Block} containing loop condition
   * @param collectionEltNode cfg {@code Node} for collection element iterated over
   */
  public static void addPotentiallyFulfillingLoop(
      ExpressionTree collectionTree,
      Tree collectionElementTree,
      Tree condition,
      Block loopBodyEntryBlock,
      Block loopUpdateBlock,
      ConditionalBlock loopConditionalBlock,
      Node collectionEltNode) {
    potentiallyFulfillingLoops.add(
        new PotentiallyFulfillingLoop(
            collectionTree,
            collectionElementTree,
            condition,
            loopBodyEntryBlock,
            loopUpdateBlock,
            loopConditionalBlock,
            collectionEltNode));
  }

  /**
   * Return the static set of {@code PotentiallyFulfillingLoop}s scheduled for analysis.
   *
   * @return the static set of {@code PotentiallyFulfillingLoop}s scheduled for analysis.
   */
  public static Set<PotentiallyFulfillingLoop> getPotentiallyFulfillingLoops() {
    return potentiallyFulfillingLoops;
  }

  /**
   * Creates a new RLCCalledMethodsAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
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
   * Returns whether the {@link MustCall#value} element/argument of the @MustCall annotation on the
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
   * Returns whether the {@link MustCall#value} element/argument of the @MustCall annotation on the
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
   * Gets the tree for a temporary variable
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
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation#getMustCallMethods(ResourceLeakAnnotatedTypeFactory,
   * CFStore)}.
   *
   * @param tree a tree
   * @return whether the tree has declared must-call obligations
   */
  public boolean declaredTypeHasMustCall(Tree tree) {
    assert tree.getKind() == Tree.Kind.METHOD
            || tree.getKind() == Tree.Kind.VARIABLE
            || tree.getKind() == Tree.Kind.NEW_CLASS
            || tree.getKind() == Tree.Kind.METHOD_INVOCATION
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
   * @return whether there is a NotOwning annotation on the given element
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
   * @return whether there is an Owning annotation on the given element
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
   * @return whether that method is one of the must-call methods for its enclosing class
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

  /** Wrapper class for a loop that might have an effect on the obligation of a collection/array. */
  public abstract static class CollectionObligationAlteringLoop {
    /** Loop is either assigning or fulfilling. */
    public static enum LoopKind {
      /**
       * Loop potentially assigns elements with non-empty {@code @MustCall} type to a collection.
       */
      ASSIGNING,

      /** Loop potentially calls methods on all elements of a collection. */
      FULFILLING
    }

    /** AST {@code Tree} for collection iterated over. */
    public final ExpressionTree collectionTree;

    /** AST {@code Tree} for collection element iterated over. */
    public final Tree collectionElementTree;

    /** AST {@code Tree} for loop condition. */
    public final Tree condition;

    /**
     * methods associated with this loop. For assigning loops, these are methods that are to be
     * added to the {@code MustCallOnElements} type and for fulfilling loops, methods that are to be
     * removed from the {@code MustCallOnElements} and added to the {@code CalledMethodsOnElements}
     * type.
     */
    protected final Set<String> associatedMethods;

    /**
     * Wether loop is assigning (elements with {@code MustCall} obligations to a collection) or
     * fulfilling.
     */
    public final LoopKind loopKind;

    /**
     * Constructs a new {@code CollectionObligationAlteringLoop}. Called by subclass constructor.
     *
     * @param collectionTree AST {@code Tree} for collection iterated over
     * @param collectionElementTree AST {@code Tree} for collection element iterated over
     * @param condition AST {@code Tree} for loop condition
     * @param associatedMethods set of methods associated with this loop
     * @param loopKind whether this is an assigning/fulfilling loop
     */
    protected CollectionObligationAlteringLoop(
        ExpressionTree collectionTree,
        Tree collectionElementTree,
        Tree condition,
        Set<String> associatedMethods,
        LoopKind loopKind) {
      this.collectionTree = collectionTree;
      this.collectionElementTree = collectionElementTree;
      this.condition = condition;
      this.loopKind = loopKind;
      if (associatedMethods == null) {
        associatedMethods = new HashSet<>();
      }
      this.associatedMethods = associatedMethods;
    }

    /**
     * Add methods associated with this loop. For assigning loops, these are methods that are to be
     * added to the {@code MustCallOnElements} type and for fulfilling loops, methods that are to be
     * removed from the {@code MustCallOnElements} and added to the {@code CalledMethodsOnElements}
     * type.
     *
     * @param methods the set of methods to add
     */
    public void addMethods(Set<String> methods) {
      associatedMethods.addAll(methods);
    }

    /**
     * Return methods associated with this loop. For assigning loops, these are methods that are to
     * be added to the {@code MustCallOnElements} type and for fulfilling loops, methods that are to
     * be removed from the {@code MustCallOnElements} and added to the {@code
     * CalledMethodsOnElements} type.
     *
     * @return the set of associated methdos
     */
    public Set<String> getMethods() {
      return associatedMethods;
    }
  }

  /**
   * Wrapper for a loop that potentially assigns elements with non-empty {@code MustCall}
   * obligations to an array, thus creating {@code MustCallOnElements} obligations for the array.
   */
  public static class PotentiallyAssigningLoop extends CollectionObligationAlteringLoop {
    /** The AST tree for the assignment of the resource into the array in the loop. */
    public final AssignmentTree assignment;

    /**
     * Constructs a new {@code PotentiallyAssigningLoop}
     *
     * @param collectionTree AST {@code Tree} for collection iterated over
     * @param collectionElementTree AST {@code Tree} for collection element iterated over
     * @param condition AST {@code Tree} for loop condition
     * @param assignment AST tree for the assignment of the resource into the array in the loop
     * @param methodsToCall set of methods that are to be added to the {@code MustCallOnElements}
     *     type of the array iterated over.
     */
    public PotentiallyAssigningLoop(
        ExpressionTree collectionTree,
        ExpressionTree collectionElementTree,
        Tree condition,
        AssignmentTree assignment,
        Set<String> methodsToCall) {
      super(
          collectionTree,
          collectionElementTree,
          condition,
          Set.copyOf(methodsToCall),
          CollectionObligationAlteringLoop.LoopKind.ASSIGNING);
      this.assignment = assignment;
    }
  }

  /** Wrapper for a loop that potentially calls methods on all elements of a collection/array. */
  public static class PotentiallyFulfillingLoop extends CollectionObligationAlteringLoop {

    /** cfg {@code Block} containing the loop body entry */
    public final Block loopBodyEntryBlock;

    /** cfg {@code Block} containing the loop update */
    public final Block loopUpdateBlock;

    /** cfg conditional {@link Block} following loop condition */
    public final ConditionalBlock loopConditionalBlock;

    /** cfg {@code Node} for the collection element iterated over */
    public final Node collectionElementNode;

    /**
     * Constructs a new {@code PotentiallyFulfillingLoop}
     *
     * @param collectionTree AST {@link Tree} for collection iterated over
     * @param collectionElementTree AST {@link Tree} for collection element iterated over
     * @param condition AST {@link Tree} for loop condition
     * @param loopBodyEntryBlock cfg {@link Block} for the loop body entry
     * @param loopUpdateBlock cfg {@link Block} for the loop update
     * @param loopConditionalBlock cfg conditional {@link Block} following loop condition
     * @param collectionEltNode cfg {@link Node} for the collection element iterated over
     */
    public PotentiallyFulfillingLoop(
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
          new HashSet<>(),
          CollectionObligationAlteringLoop.LoopKind.FULFILLING);
      this.loopBodyEntryBlock = loopBodyEntryBlock;
      this.loopUpdateBlock = loopUpdateBlock;
      this.loopConditionalBlock = loopConditionalBlock;
      this.collectionElementNode = collectionEltNode;
    }
  }

  /**
   * After running the called-methods analysis, call the consistency analyzer to analyze the loop
   * bodys of 'potentially-collection-obligation-fulfilling-loops', as determined by a
   * pre-pattern-match in the MustCallVisitor.
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

    // traverse the cfg to find enhanced-for-loops over collections and perform a
    // loop-body-analysis.
    // since this runs before the consistency analysis, we unfortunately have to traverse the cfg
    // twice. The first time to find these for-each loop, and the second time much later to do the
    // final consistency analysis.
    mustCallConsistencyAnalyzer.findFulfillingForEachLoops(cfg);

    // perform loop-body-analysis on normal for-loops that were pattern matched on the AST
    if (potentiallyFulfillingLoops.size() > 0) {
      Set<PotentiallyFulfillingLoop> analyzed = new HashSet<>();
      for (PotentiallyFulfillingLoop potentiallyFulfillingLoop : potentiallyFulfillingLoops) {
        Tree collectionElementTree = potentiallyFulfillingLoop.collectionElementTree;
        boolean loopContainedInThisMethod =
            cfg.getNodesCorrespondingToTree(collectionElementTree) != null;
        if (loopContainedInThisMethod) {
          mustCallConsistencyAnalyzer.analyzeObligationFulfillingLoop(
              cfg, potentiallyFulfillingLoop);
          analyzed.add(potentiallyFulfillingLoop);
        }
      }
      potentiallyFulfillingLoops.removeAll(analyzed);
    }

    super.postAnalyze(cfg);
  }
}
