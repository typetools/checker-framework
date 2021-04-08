package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SubtypeIsSupersetQualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

public class KeyForAnnotatedTypeFactory
    extends GenericAnnotatedTypeFactory<KeyForValue, KeyForStore, KeyForTransfer, KeyForAnalysis> {

  /** The @{@link UnknownKeyFor} annotation. */
  protected final AnnotationMirror UNKNOWNKEYFOR =
      AnnotationBuilder.fromClass(elements, UnknownKeyFor.class);
  /** The @{@link KeyForBottom} annotation. */
  protected final AnnotationMirror KEYFORBOTTOM =
      AnnotationBuilder.fromClass(elements, KeyForBottom.class);

  /** The canonical name of the KeyFor class. */
  protected final @CanonicalName String KEYFOR_NAME = KeyFor.class.getCanonicalName();

  /** The Map.containsKey method. */
  private final ExecutableElement mapContainsKey =
      TreeUtils.getMethod("java.util.Map", "containsKey", 1, processingEnv);
  /** The Map.get method. */
  private final ExecutableElement mapGet =
      TreeUtils.getMethod("java.util.Map", "get", 1, processingEnv);
  /** The Map.put method. */
  private final ExecutableElement mapPut =
      TreeUtils.getMethod("java.util.Map", "put", 2, processingEnv);
  /** The KeyFor.value field/element. */
  protected final ExecutableElement keyForValueElement =
      TreeUtils.getMethod(KeyFor.class, "value", 0, processingEnv);

  /** Moves annotations from one side of a pseudo-assignment to the other. */
  private final KeyForPropagator keyForPropagator = new KeyForPropagator(UNKNOWNKEYFOR);

  /** Create a new KeyForAnnotatedTypeFactory. */
  public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, true);

    // Add compatibility annotations:
    addAliasedTypeAnnotation(
        "org.checkerframework.checker.nullness.compatqual.KeyForDecl", KeyFor.class, true);
    addAliasedTypeAnnotation(
        "org.checkerframework.checker.nullness.compatqual.KeyForType", KeyFor.class, true);

    // While strictly required for soundness, this leads to too many false positives.  Printing
    // a key or putting it in a map erases all knowledge of what maps it was a key for.
    // TODO: Revisit when side effect annotations are more precise.
    // sideEffectsUnrefineAliases = true;

    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(KeyFor.class, UnknownKeyFor.class, KeyForBottom.class, PolyKeyFor.class));
  }

  @Override
  public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
    ParameterizedExecutableType result = super.constructorFromUse(tree);
    keyForPropagator.propagateNewClassTree(tree, result.executableType.getReturnType(), this);
    return result;
  }

  @Override
  protected TypeHierarchy createTypeHierarchy() {
    return new KeyForTypeHierarchy(
        checker,
        getQualifierHierarchy(),
        checker.getBooleanOption("ignoreRawTypeArguments", true),
        checker.hasOption("invariantArrays"));
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new KeyForPropagationTreeAnnotator(this, keyForPropagator));
  }

  // TODO: work on removing this class
  protected static class KeyForTypeHierarchy extends DefaultTypeHierarchy {

    public KeyForTypeHierarchy(
        BaseTypeChecker checker,
        QualifierHierarchy qualifierHierarchy,
        boolean ignoreRawTypes,
        boolean invariantArrayComponents) {
      super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
    }

    @Override
    protected boolean isSubtype(
        AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
      // TODO: THIS IS FROM THE OLD TYPE HIERARCHY.  WE SHOULD FIX DATA-FLOW/PROPAGATION TO DO
      // THE RIGHT THING
      if (supertype.getKind() == TypeKind.TYPEVAR && subtype.getKind() == TypeKind.TYPEVAR) {
        // TODO: Investigate whether there is a nicer and more proper way to
        // get assignments between two type variables working.
        if (supertype.getAnnotations().isEmpty()) {
          return true;
        }
      }

      // Otherwise Covariant would cause trouble.
      if (subtype.hasAnnotation(KeyForBottom.class)) {
        return true;
      }
      return super.isSubtype(subtype, supertype, top);
    }
  }

  @Override
  protected KeyForAnalysis createFlowAnalysis(
      List<Pair<VariableElement, KeyForValue>> fieldValues) {
    // Explicitly call the constructor instead of using reflection.
    return new KeyForAnalysis(checker, this, fieldValues);
  }

  @Override
  public KeyForTransfer createFlowTransferFunction(
      CFAbstractAnalysis<KeyForValue, KeyForStore, KeyForTransfer> analysis) {
    // Explicitly call the constructor instead of using reflection.
    return new KeyForTransfer((KeyForAnalysis) analysis);
  }

  /**
   * Given a string array 'values', returns an AnnotationMirror corresponding to @KeyFor(values)
   *
   * @param values the values for the {@code @KeyFor} annotation
   * @return a {@code @KeyFor} annotation with the given values
   */
  public AnnotationMirror createKeyForAnnotationMirrorWithValue(Set<String> values) {
    // Create an AnnotationBuilder with the ArrayList
    AnnotationBuilder builder = new AnnotationBuilder(getProcessingEnv(), KeyFor.class);
    builder.setValue("value", values.toArray());

    // Return the resulting AnnotationMirror
    return builder.build();
  }

  /**
   * Given a string 'value', returns an AnnotationMirror corresponding to @KeyFor(value)
   *
   * @param value the argument to {@code @KeyFor}
   * @return a {@code @KeyFor} annotation with the given value
   */
  public AnnotationMirror createKeyForAnnotationMirrorWithValue(String value) {
    return createKeyForAnnotationMirrorWithValue(Collections.singleton(value));
  }

  /**
   * Returns true if the expression tree is a key for the map.
   *
   * @param mapExpression expression that has type Map
   * @param tree expression that might be a key for the map
   * @return whether or not the expression is a key for the map
   */
  public boolean isKeyForMap(String mapExpression, ExpressionTree tree) {
    Collection<String> maps = null;
    AnnotatedTypeMirror type = getAnnotatedType(tree);
    AnnotationMirror keyForAnno = type.getAnnotation(KeyFor.class);
    if (keyForAnno != null) {
      maps = AnnotationUtils.getElementValueArray(keyForAnno, keyForValueElement, String.class);
    } else {
      KeyForValue value = getInferredValueFor(tree);
      if (value != null) {
        maps = value.getKeyForMaps();
      }
    }

    return maps != null && maps.contains(mapExpression);
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy() {
    return new SubtypeIsSupersetQualifierHierarchy(getSupportedTypeQualifiers(), processingEnv);
  }

  /** Returns true if the node is an invocation of Map.containsKey. */
  boolean isMapContainsKey(Tree tree) {
    return TreeUtils.isMethodInvocation(tree, mapContainsKey, getProcessingEnv());
  }

  /** Returns true if the node is an invocation of Map.get. */
  boolean isMapGet(Tree tree) {
    return TreeUtils.isMethodInvocation(tree, mapGet, getProcessingEnv());
  }

  /** Returns true if the node is an invocation of Map.put. */
  boolean isMapPut(Tree tree) {
    return TreeUtils.isMethodInvocation(tree, mapPut, getProcessingEnv());
  }

  /** Returns true if the node is an invocation of Map.containsKey. */
  boolean isMapContainsKey(Node node) {
    return NodeUtils.isMethodInvocation(node, mapContainsKey, getProcessingEnv());
  }

  /** Returns true if the node is an invocation of Map.get. */
  boolean isMapGet(Node node) {
    return NodeUtils.isMethodInvocation(node, mapGet, getProcessingEnv());
  }

  /** Returns true if the node is an invocation of Map.put. */
  boolean isMapPut(Node node) {
    return NodeUtils.isMethodInvocation(node, mapPut, getProcessingEnv());
  }

  /** Returns false. Redundancy in the KeyFor hierarchy is not worth warning about. */
  @Override
  public boolean shouldWarnIfStubRedundantWithBytecode() {
    return false;
  }
}
