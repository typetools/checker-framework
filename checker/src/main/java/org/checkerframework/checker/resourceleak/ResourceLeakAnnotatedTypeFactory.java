package org.checkerframework.checker.resourceleak;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
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
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFCFGBuilder;
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

  private Set<FieldToFinalizers> fieldToFinalizers = new HashSet<>();

  /**
   * Creates a new ResourceLeakAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public ResourceLeakAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  public Set<FieldToFinalizers> CollectFields(ClassTree classTree) {
    List<? extends Tree> members = classTree.getMembers();
    Set<FieldToFinalizers> fieldsToFinalizers = new HashSet<>();
    for (Tree tree : members) {
      Element elt = TreeUtils.elementFromTree(tree);
      if (elt != null
          && elt.getKind().isField()
          && ElementUtils.isFinal(elt)
          && !getMustCallValue(elt).isEmpty()) {
        fieldsToFinalizers.add(new FieldToFinalizers(elt, new LinkedHashSet<>()));
      }
    }
    return fieldsToFinalizers;
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree root) {
    super.setRoot(root);
  }

  public Set<FieldToFinalizers> getFieldToFinalizers() {
    return fieldToFinalizers;
  }

  @Override
  public void preProcessClassTree(ClassTree classTree) {
    fieldToFinalizers.clear();
    this.fieldToFinalizers = CollectFields(classTree);
    super.preProcessClassTree(classTree);
  }

  @Override
  public void postProcessClassTree(ClassTree tree) {
    if (!fieldToFinalizers.isEmpty()) {
      List<? extends Tree> members = tree.getMembers();
      for (Tree t : members) {
        if (t.getKind() == Tree.Kind.METHOD) {
          MethodTree mt = (MethodTree) t;
          Set<Modifier> flags = mt.getModifiers().getFlags();
          if (flags.contains(Modifier.ABSTRACT) || flags.contains(Modifier.NATIVE)) {
            break;
          }
          if (mt.getBody() == null) {
            break;
          }
          CFGMethod met = new CFGMethod(mt, tree);
          ControlFlowGraph cfg = CFCFGBuilder.build(this.root, met, checker, this, processingEnv);
          if (cfg != null) {
            MustCallInferenceLogic mustCallInferenceLogic = new MustCallInferenceLogic(this);
            mustCallInferenceLogic.inference(cfg);
          }
        }
      }
    }
    super.postProcessClassTree(tree);
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

  public static class FieldToFinalizers {
    public final Element field;

    public ImmutableSet<Element> finalizers;

    public FieldToFinalizers(Element f, Set<Element> set) {
      this.field = f;
      this.finalizers = ImmutableSet.copyOf(set);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      ResourceLeakAnnotatedTypeFactory.FieldToFinalizers that =
          (ResourceLeakAnnotatedTypeFactory.FieldToFinalizers) obj;
      return field.equals(that.field) && this.finalizers.equals(that.finalizers);
    }

    @Override
    public int hashCode() {
      return Objects.hash(field, finalizers);
    }
  }
}
