package org.checkerframework.checker.resourceleak;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
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
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * The type factory for the Resource Leak Checker. The main difference between this and the Called
 * Methods type factory from which it is derived is that this version's {@link
 * #postAnalyze(ControlFlowGraph)} method checks that must-call obligations are fulfilled.
 */
public class ResourceLeakAnnotatedTypeFactory extends CalledMethodsAnnotatedTypeFactory
    implements CreatesMustCallForElementSupplier {

  /** The MustCall.value element/field. */
  private final ExecutableElement mustCallValueElement =
      TreeUtils.getMethod(MustCall.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.value element/field. */
  /*package-private*/ final ExecutableElement ensuresCalledMethodsValueElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.methods element/field. */
  /*package-private*/ final ExecutableElement ensuresCalledMethodsMethodsElement =
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
   * Creates a new ResourceLeakAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public ResourceLeakAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.noResourceAliases = checker.hasOption(MustCallChecker.NO_RESOURCE_ALIASES);
    this.postInit();
  }

  /**
   * Is the given element a candidate to be an owning field? A candidate owning field must have a
   * non-empty must-call obligation.
   *
   * @param element a element
   * @return true iff the given element is a field with non-empty @MustCall obligation
   */
  /*package-private*/ boolean isFieldWithNonemptyMustCallValue(Element element) {
    return element.getKind().isField() && !hasEmptyMustCallValue(element);
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
  public void postAnalyze(ControlFlowGraph cfg) {
    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(this, (ResourceLeakAnalysis) this.analysis);
    mustCallConsistencyAnalyzer.analyze(cfg);

    // Inferring owning annotations for @Owning fields/parameters, @EnsuresCalledMethods for
    // finalizer methods and @InheritableMustCall annotations for the class declarations.
    if (getWholeProgramInference() != null) {
      if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
        MustCallInference.runMustCallInference(this, cfg, mustCallConsistencyAnalyzer);
      }
    }

    super.postAnalyze(cfg);
    tempVarToTree.clear();
  }

  @Override
  protected ResourceLeakAnalysis createFlowAnalysis() {
    return new ResourceLeakAnalysis((ResourceLeakChecker) checker, this);
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
  /*package-private*/ boolean hasEmptyMustCallValue(Tree tree) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType = mustCallAnnotatedTypeFactory.getAnnotatedType(tree);
    AnnotationMirror mustCallAnnotation =
        mustCallAnnotatedType.getPrimaryAnnotation(MustCall.class);
    if (mustCallAnnotation != null) {
      return getMustCallValues(mustCallAnnotation).isEmpty();
    } else {
      // Indicates @MustCallUnknown, which should be treated (conservatively) as if it contains
      // some must call values.
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
  /*package-private*/ boolean hasEmptyMustCallValue(Element element) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType =
        mustCallAnnotatedTypeFactory.getAnnotatedType(element);
    AnnotationMirror mustCallAnnotation =
        mustCallAnnotatedType.getPrimaryAnnotation(MustCall.class);
    if (mustCallAnnotation != null) {
      return getMustCallValues(mustCallAnnotation).isEmpty();
    } else {
      // Indicates @MustCallUnknown, which should be treated (conservatively) as if it contains
      // some must call values.
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
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation#getMustCallMethods(ResourceLeakAnnotatedTypeFactory,
   * CFStore)}.
   *
   * <p>Do not call {@link List#isEmpty()} on the result of this method: prefer to call {@link
   * #hasEmptyMustCallValue(Element)}, which correctly accounts for @MustCallUnknown, instead.
   *
   * @param element an element
   * @return the strings in its must-call type
   */
  /*package-private*/ List<String> getMustCallValues(Element element) {
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
  /*package-private*/ List<String> getMustCallValues(
      @Nullable AnnotationMirror mustCallAnnotation) {
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
  /*package-private*/ @Nullable LocalVariableNode getTempVarForNode(Node node) {
    return tempVarToTree.inverse().get(node.getTree());
  }

  /**
   * Is the given node a temporary variable?
   *
   * @param node a node
   * @return true iff the given node is a temporary variable
   */
  /*package-private*/ boolean isTempVar(Node node) {
    return tempVarToTree.containsKey(node);
  }

  /**
   * Gets the tree for a temporary variable
   *
   * @param node a node for a temporary variable
   * @return the tree for {@code node}
   */
  /*package-private*/ Tree getTreeForTempVar(Node node) {
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
  /*package-private*/ void addTempVar(LocalVariableNode tmpVar, Tree tree) {
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
  /*package-private*/ boolean declaredTypeHasMustCall(Tree tree) {
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
  /*package-private*/ boolean hasMustCallAlias(Tree tree) {
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
  /*package-private*/ boolean hasMustCallAlias(Element elt) {
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
    return !checker.hasOption(MustCallChecker.NO_CREATES_MUSTCALLFOR);
  }

  @Override
  @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
  public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>> @Nullable T getTypeFactoryOfSubcheckerOrNull(Class<? extends BaseTypeChecker> subCheckerClass) {
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
    //      (see ResourceLeakVisitor.checkOwningField).
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
    TypeElement containingClass = ElementUtils.enclosingTypeElement(elt);
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotationMirror mcAnno =
        mustCallAnnotatedTypeFactory
            .getAnnotatedType(containingClass)
            .getPrimaryAnnotationInHierarchy(mustCallAnnotatedTypeFactory.TOP);
    List<String> mcValues =
        AnnotationUtils.getElementValueArray(
            mcAnno, mustCallAnnotatedTypeFactory.getMustCallValueElement(), String.class);
    String methodName = elt.getSimpleName().toString();
    return mcValues.contains(methodName);
  }
}
