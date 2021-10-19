package org.checkerframework.checker.resourceleak;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
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
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The type factory for the Resource Leak Checker. The main difference between this and the Called
 * Methods type factory from which it is derived is that this version's {@link
 * #postAnalyze(ControlFlowGraph)} method checks that must-call obligations are fulfilled.
 */
public class ResourceLeakAnnotatedTypeFactory extends CalledMethodsAnnotatedTypeFactory
    implements CreatesMustCallForElementSupplier {

  /** The MustCall.value element/field. */
  final ExecutableElement mustCallValueElement =
      TreeUtils.getMethod(MustCall.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.value element/field. */
  final ExecutableElement ensuresCalledMethodsValueElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.methods element/field. */
  final ExecutableElement ensuresCalledMethodsMethodsElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "methods", 0, processingEnv);

  /** The CreatesMustCallFor.List.value element/field. */
  private final ExecutableElement createsMustCallForListValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.List.class, "value", 0, processingEnv);

  /** The CreatesMustCallFor.value element/field. */
  private final ExecutableElement createsMustCallForValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.class, "value", 0, processingEnv);

  /**
   * Bidirectional map to store temporary variables created for expressions with non-empty @MustCall
   * obligations and the corresponding trees. Keys are the artificial local variable nodes created
   * as temporary variables; values are the corresponding trees.
   */
  private final BiMap<LocalVariableNode, Tree> tempVarToTree = HashBiMap.create();

  /**
   * Map to store a set of FieldToFinalizers for classes. Keys are class trees; values are sets of
   * FieldToFinalizers which represents a set of method Elements that satisfy @MustCall obligation
   * of FieldToFinalizers's field along some path to their regular exit points
   */
  Map<ClassTree, Set<FieldToFinalizers>> classToFieldToFinalizers = new HashMap<>();

  /**
   * Creates a new ResourceLeakAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public ResourceLeakAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  /**
   * Is the given element a final field with non-empty @MustCall obligation?
   *
   * @param element a element
   * @return true iff the given element is a final field with non-empty @MustCall obligation
   */
  boolean isElementPossibleOwningField(Element element) {
    return (element.getKind().isField()
        && ElementUtils.isFinal(element)
        && !getMustCallValue(element).isEmpty());
  }

  /**
   * Given a set of FieldToFinalizers and a field, returns the fieldToFinalizers whose field is
   * equal to the given field if one exists in {@code fieldsToFinalizers}, otherwise returns {@code
   * null}.
   *
   * @param field a field
   * @param fieldsToFinalizers a set of FieldToFinalizers
   * @return the FieldToFinalizers in {@code fieldsToFinalizers} whose field is equal to {@code
   *     field}, or {@code null} if there is no such FieldToFinalizers
   */
  private @Nullable FieldToFinalizers getFieldToFinalizers(
      Element field, Set<FieldToFinalizers> fieldsToFinalizers) {
    for (FieldToFinalizers ftf : fieldsToFinalizers) {
      if (ftf.field.equals(field)) {
        return ftf;
      }
    }
    return null;
  }

  /**
   * Returns the set of FieldToFinalizers stored for the given key in classToFieldToFinalizers map
   * if one exists, otherwise it adds a new entry to the map.
   *
   * @param classTree a classTree
   * @return the set of FieldToFinalizers stored in classToFieldToFinalizers for the given classTree
   */
  private Set<FieldToFinalizers> getSetOfFieldToFinalizers(ClassTree classTree) {
    if (!classToFieldToFinalizers.containsKey(classTree)) {
      classToFieldToFinalizers.put(classTree, new HashSet<>());
    }
    return classToFieldToFinalizers.get(classTree);
  }

  /**
   * Updates the classToFieldToFinalizers map with the new detected finalizer method for the given
   * field.
   *
   * @param classTree the containing class
   * @param field the owning field
   * @param methodElt the new finalizer for the owning field
   */
  public void updateClassToFieldToFinalizers(
      ClassTree classTree, Element field, Element methodElt) {
    Set<FieldToFinalizers> fieldsToFinalizers = getSetOfFieldToFinalizers(classTree);
    FieldToFinalizers fieldToFinalizers = getFieldToFinalizers(field, fieldsToFinalizers);

    if (fieldToFinalizers == null) {
      Set<Element> finalizers = new HashSet<>();
      finalizers.add(methodElt);
      fieldsToFinalizers.add(new FieldToFinalizers(field, finalizers));
    } else {
      fieldToFinalizers.finalizers.add(methodElt);
    }
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree root) {
    super.setRoot(root);
    classToFieldToFinalizers.clear();
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
  public AnnotationMirror createCalledMethods(final String... val) {
    return createAccumulatorAnnotation(Arrays.asList(val));
  }

  @Override
  public void postAnalyze(ControlFlowGraph cfg) {
    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(this, this.analysis);
    mustCallConsistencyAnalyzer.analyze(cfg);

    // Inferring owning annotations for final owning fields
    if (getWholeProgramInference() != null && cfg != null) {
      if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
        MustCallInferenceLogic mustCallInferenceLogic = new MustCallInferenceLogic(this, cfg);
        mustCallInferenceLogic.inference();
      }
    }

    super.postAnalyze(cfg);
    tempVarToTree.clear();
  }

  @Override
  protected ResourceLeakAnalysis createFlowAnalysis() {
    return new ResourceLeakAnalysis(checker, this);
  }

  /**
   * Returns the {@link MustCall#value} element/argument of the @MustCall annotation on the type of
   * {@code tree}.
   *
   * <p>If possible, prefer {@link #getMustCallValue(Tree)}, which accounts for flow-sensitive
   * refinement.
   *
   * @param tree a tree
   * @return the strings in its must-call type
   */
  /* package-private */ List<String> getMustCallValue(Tree tree) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType = mustCallAnnotatedTypeFactory.getAnnotatedType(tree);
    AnnotationMirror mustCallAnnotation = mustCallAnnotatedType.getAnnotation(MustCall.class);
    return getMustCallValues(mustCallAnnotation);
  }

  /**
   * Returns the {@link MustCall#value} element/argument of the @MustCall annotation on the class
   * type of {@code element}.
   *
   * <p>Do not use this method to get the MustCall value of an {@link
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation}. Instead, use
   * {@link
   * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation#getMustCallMethods(ResourceLeakAnnotatedTypeFactory,
   * CFStore)}.
   *
   * @param element an element
   * @return the strings in its must-call type
   */
  /* package-private */ List<String> getMustCallValue(Element element) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType =
        mustCallAnnotatedTypeFactory.getAnnotatedType(element);
    AnnotationMirror mustCallAnnotation = mustCallAnnotatedType.getAnnotation(MustCall.class);
    return getMustCallValues(mustCallAnnotation);
  }

  /**
   * Helper method for getting the must-call values from a must-call annotation.
   *
   * @param mustCallAnnotation a {@link MustCall} annotation, or null
   * @return the strings in mustCallAnnotation's value element, or the empty list if
   *     mustCallAnnotation is null
   */
  /* package-private */ List<String> getMustCallValues(
      @Nullable AnnotationMirror mustCallAnnotation) {
    if (mustCallAnnotation == null) {
      return Collections.emptyList();
    }
    return AnnotationUtils.getElementValueArray(
        mustCallAnnotation, mustCallValueElement, String.class);
  }

  /**
   * Helper method to get the temporary variable that represents the given node, if one exists.
   *
   * @param node a node
   * @return the tempvar for node's expression, or null if one does not exist
   */
  /* package-private */
  @Nullable LocalVariableNode getTempVarForNode(Node node) {
    return tempVarToTree.inverse().get(node.getTree());
  }

  /**
   * Is the given node a temporary variable?
   *
   * @param node a node
   * @return true iff the given node is a temporary variable
   */
  /* package-private */ boolean isTempVar(Node node) {
    return tempVarToTree.containsKey(node);
  }

  /**
   * Registers a temporary variable by adding it to this type factory's tempvar map.
   *
   * @param tmpVar a temporary variable
   * @param tree the tree of the expression the tempvar represents
   */
  /* package-private */ void addTempVar(LocalVariableNode tmpVar, Tree tree) {
    tempVarToTree.put(tmpVar, tree);
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
  /* package-private */ boolean declaredTypeHasMustCall(Tree tree) {
    assert tree.getKind() == Tree.Kind.METHOD
            || tree.getKind() == Tree.Kind.VARIABLE
            || tree.getKind() == Tree.Kind.NEW_CLASS
            || tree.getKind() == Tree.Kind.METHOD_INVOCATION
        : "unexpected declaration tree kind: " + tree.getKind();
    return !getMustCallValue(tree).isEmpty();
  }

  /**
   * Returns true if the given tree has an {@link MustCallAlias} annotation and resource-alias
   * tracking is not disabled.
   *
   * @param tree a tree
   * @return true if the given tree has an {@link MustCallAlias} annotation
   */
  /* package-private */ boolean hasMustCallAlias(Tree tree) {
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
  /* package-private */ boolean hasMustCallAlias(Element elt) {
    if (checker.hasOption(MustCallChecker.NO_RESOURCE_ALIASES)) {
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
    return !checker.hasOption(MustCallChecker.NO_CREATES_MUSTCALLFOR);
  }

  @Override
  @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
  public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>> @Nullable T getTypeFactoryOfSubchecker(
      Class<? extends BaseTypeChecker> subCheckerClass) {
    if (subCheckerClass == MustCallChecker.class) {
      if (!canCreateObligations()) {
        return super.getTypeFactoryOfSubchecker(MustCallNoCreatesMustCallForChecker.class);
      }
    }
    return super.getTypeFactoryOfSubchecker(subCheckerClass);
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

  /** A pair of a field {@link Element} and a set of finalizer methods detected for that field. */
  static class FieldToFinalizers {

    /** The field element. */
    public final Element field;

    /** The set of finalizer methods. */
    public Set<Element> finalizers;

    /**
     * Creates a new FieldToFinalizers.
     *
     * @param fieldElt the field element
     * @param finalizers the set of finalizer methods
     */
    public FieldToFinalizers(Element fieldElt, Set<Element> finalizers) {
      this.field = fieldElt;
      this.finalizers = finalizers;
    }

    @Override
    public String toString() {
      return "(FieldToFinalizers: field: " + field + " |||| finalizers: " + finalizers + ")";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FieldToFinalizers that = (FieldToFinalizers) o;
      return field.equals(that.field);
    }

    @Override
    public int hashCode() {
      return Objects.hash(field);
    }
  }
}
