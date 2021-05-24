package org.checkerframework.checker.resourceleak;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
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
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsBottom;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.CreatesObligationElementSupplier;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesObligationChecker;
import org.checkerframework.checker.mustcall.qual.CreatesObligation;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.LocalVarWithTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The type factory for the Resource Leak Checker. The main difference between this and the Called
 * Methods type factory from which it is derived is that this version's {@link
 * #postAnalyze(ControlFlowGraph)} method checks that must-call obligations are fulfilled.
 */
public class ResourceLeakAnnotatedTypeFactory extends CalledMethodsAnnotatedTypeFactory
    implements CreatesObligationElementSupplier {

  /** The MustCall.value field/element */
  final ExecutableElement mustCallValueElement =
      TreeUtils.getMethod(MustCall.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.value field/element */
  final ExecutableElement ensuresCalledMethodsValueElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.methods field/element */
  final ExecutableElement ensuresCalledMethodsMethodsElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "methods", 0, processingEnv);

  /** The CreatesObligation.List.value field/element. */
  private final ExecutableElement createsObligationListValueElement =
      TreeUtils.getMethod(CreatesObligation.List.class, "value", 0, processingEnv);

  /** The CreatesObligation.value field/element. */
  private final ExecutableElement createsObligationValueElement =
      TreeUtils.getMethod(CreatesObligation.class, "value", 0, processingEnv);

  /**
   * Bidirectional map to preserve temporary variables created for nodes with non-empty @MustCall
   * annotation and the corresponding nodes.
   */
  private BiMap<LocalVariableNode, Tree> tempVarToNode = HashBiMap.create();

  /**
   * Default constructor matching super. Should be called automatically.
   *
   * @param checker the checker associated with this type factory
   */
  public ResourceLeakAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    this.postInit();
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
    tempVarToNode.clear();
  }

  /**
   * Use the must-call store to get the must-call value of the resource represented by the given
   * local variables.
   *
   * @param localVarWithTreeSet a set of local variables with their assignment trees, all of which
   *     represent the same resource
   * @param mcStore a CFStore produced by the MustCall checker's dataflow analysis. If this is null,
   *     then the default MustCall type of each variable's class will be used.
   * @return the list of must-call method names
   */
  public @Nullable List<String> getMustCallValue(
      ImmutableSet<LocalVarWithTree> localVarWithTreeSet, @Nullable CFStore mcStore) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);

    // Need to get the LUB of the MC values, because if a CreatesObligation method was
    // called on just one of the locals then they all need to be treated as if
    // they need to call the relevant methods.
    AnnotationMirror mcLub = mustCallAnnotatedTypeFactory.BOTTOM;
    for (LocalVarWithTree lvt : localVarWithTreeSet) {
      AnnotationMirror mcAnno = null;
      LocalVariable local = lvt.localVar;
      CFValue value = mcStore == null ? null : mcStore.getValue(local);
      if (value != null) {
        for (AnnotationMirror anno : value.getAnnotations()) {
          if (AnnotationUtils.areSameByName(
              anno, "org.checkerframework.checker.mustcall.qual.MustCall")) {
            mcAnno = anno;
            break;
          }
        }
      }
      // If it wasn't in the store, fall back to the default must-call type for the class.
      // TODO: we currently end up in this case when checking a call to the return type
      // of a returns-receiver method on something with a MustCall type; for example,
      // see tests/socket/ZookeeperReport6.java. We should instead use a poly type if we
      // can; that would probably require us to change the Must Call Checker to also
      // track temporaries.
      if (mcAnno == null) {
        TypeElement typeElt = TypesUtils.getTypeElement(local.getType());
        if (typeElt == null) {
          mcAnno = mustCallAnnotatedTypeFactory.TOP;
        } else {
          // Why does this happen sometimes?
          if (typeElt.asType().getKind() == TypeKind.VOID) {
            return Collections.emptyList();
          }
          mcAnno =
              mustCallAnnotatedTypeFactory
                  .getAnnotatedType(typeElt)
                  .getAnnotationInHierarchy(mustCallAnnotatedTypeFactory.TOP);
        }
      }
      mcLub = mustCallAnnotatedTypeFactory.getQualifierHierarchy().leastUpperBound(mcLub, mcAnno);
    }
    if (AnnotationUtils.areSameByName(
        mcLub, "org.checkerframework.checker.mustcall.qual.MustCall")) {
      return getMustCallValues(mcLub);
    } else {
      return null;
    }
  }

  /**
   * Returns the String value of @MustCall annotation of the type of {@code tree}.
   *
   * <p>If possible, prefer {@link #getMustCallValue(Tree)}, which will account for flow-sensitive
   * refinement.
   *
   * @param tree the tree
   * @return the strings in its must-call type
   */
  /* package-private */ List<String> getMustCallValue(Tree tree) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    if (mustCallAnnotatedTypeFactory == null) {
      return Collections.emptyList();
    }
    AnnotationMirror mustCallAnnotation =
        mustCallAnnotatedTypeFactory.getAnnotatedType(tree).getAnnotation(MustCall.class);

    return getMustCallValues(mustCallAnnotation);
  }

  /**
   * Returns the String value of @MustCall annotation declared on the class type of {@code element}.
   *
   * <p>If possible, prefer {@link #getMustCallValue(Tree)}, which will account for flow-sensitive
   * refinement.
   *
   * @param element the element
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
   * @return the list of strings in mustCallAnnotation's value element, or the empty list if it was
   *     null
   */
  private List<String> getMustCallValues(@Nullable AnnotationMirror mustCallAnnotation) {
    List<String> mustCallValues =
        (mustCallAnnotation != null)
            ? AnnotationUtils.getElementValueArray(
                mustCallAnnotation, mustCallValueElement, String.class)
            : Collections.emptyList();
    return mustCallValues;
  }

  /**
   * Helper method to get the temporary variable that represents the given node, if one exists.
   *
   * @param node a node
   * @return the tempvar for node's expression, or null if one does not exist
   */
  /* package-private */
  @Nullable LocalVariableNode getTempVarForTree(Node node) {
    return tempVarToNode.inverse().get(node.getTree());
  }

  /**
   * Is the given node a temporary variable?
   *
   * @param node a node
   * @return true iff the given node is a temporary variable
   */
  /* package-private */ boolean isTempVar(Node node) {
    return tempVarToNode.containsKey(node);
  }

  /**
   * Registers a temporary variables by adding it to this type factory's tempvar map.
   *
   * @param tmpVar a temporary variable
   * @param tree the tree of the expression the tempvar represents
   */
  /* package-private */ void addTempVar(LocalVariableNode tmpVar, Tree tree) {
    tempVarToNode.put(tmpVar, tree);
  }

  /**
   * Returns true if the type of the tree includes a must-call annotation. Note that this method may
   * not consider dataflow, and is only safe to use on declarations, such as method trees or
   * parameter trees. Use {@link #getMustCallValue(ImmutableSet, CFStore)} (and check for emptiness)
   * if you are trying to determine whether a local variable has must-call obligations.
   *
   * @param declTree a tree representing a declaration
   * @return whether that declaration has must-call obligations
   */
  /* package-private */ boolean hasMustCall(Tree declTree) {
    return !getMustCallValue(declTree).isEmpty();
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
   * CreatesObligation} annotations.
   *
   * @param node a method invocation node
   * @return true iff there is one or more create obligation annotations on the declaration of the
   *     invoked method
   */
  public boolean hasCreatesObligation(MethodInvocationNode node) {
    ExecutableElement decl = TreeUtils.elementFromUse(node.getTree());
    return getDeclAnnotation(decl, CreatesObligation.class) != null
        || getDeclAnnotation(decl, CreatesObligation.List.class) != null;
  }

  /**
   * Does this type factory support {@link CreatesObligation}?
   *
   * @return true iff the -AnoCreatesObligation was not supplied to the checker
   */
  public boolean canCreateObligations() {
    return !checker.hasOption(MustCallChecker.NO_CREATES_OBLIGATION);
  }

  @Override
  @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
  public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>, U extends BaseTypeChecker>
      T getTypeFactoryOfSubchecker(Class<U> checkerClass) {
    if (checkerClass.equals(MustCallChecker.class)) {
      if (!canCreateObligations()) {
        return super.getTypeFactoryOfSubchecker(MustCallNoCreatesObligationChecker.class);
      }
    }
    return super.getTypeFactoryOfSubchecker(checkerClass);
  }

  /**
   * Returns the CreatesObligation.value field.
   *
   * @return the CreatesObligation.value field
   */
  @Override
  public ExecutableElement getCreatesObligationValueElement() {
    return createsObligationValueElement;
  }

  /**
   * Returns the CreatesObligation.List.value field.
   *
   * @return the CreatesObligation.List.value field
   */
  @Override
  public ExecutableElement getCreatesObligationListValueElement() {
    return createsObligationListValueElement;
  }
}
