package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SubtypeIsSupersetQualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
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
  protected static final @CanonicalName String KEYFOR_NAME = KeyFor.class.getCanonicalName();

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

  /**
   * If true, assume the argument to Map.get is always a key for the receiver map. This is set by
   * the `-AassumeKeyFor` command-line argument. However, if the Nullness Checker is being run, then
   * `-AassumeKeyFor` disables the Map Key Checker.
   */
  private final boolean assumeKeyFor;

  /**
   * Creates a new KeyForAnnotatedTypeFactory.
   *
   * @param checker the associated checker
   */
  public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, true);

    assumeKeyFor = checker.hasOption("assumeKeyFor");

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
  protected ParameterizedExecutableType constructorFromUse(
      NewClassTree tree, boolean inferTypeArgs) {
    ParameterizedExecutableType result = super.constructorFromUse(tree, inferTypeArgs);
    keyForPropagator.propagateNewClassTree(tree, result.executableType.getReturnType(), this);
    return result;
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new KeyForPropagationTreeAnnotator(this, keyForPropagator));
  }

  @Override
  protected KeyForAnalysis createFlowAnalysis() {
    // Explicitly call the constructor instead of using reflection.
    return new KeyForAnalysis(checker, this);
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
    // This test only has an effect if the Map Key Checker is being run on its own.  If the
    // Nullness Checker is being run, then -AassumeKeyFor disables the Map Key Checker.
    if (assumeKeyFor) {
      return true;
    }
    Collection<String> maps = null;
    AnnotatedTypeMirror type = getAnnotatedType(tree);
    AnnotationMirror keyForAnno = type.getEffectiveAnnotation(KeyFor.class);
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
  protected QualifierHierarchy createQualifierHierarchy() {
    return new SubtypeIsSupersetQualifierHierarchy(
        getSupportedTypeQualifiers(), processingEnv, KeyForAnnotatedTypeFactory.this);
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

  @Override
  protected DependentTypesHelper createDependentTypesHelper() {
    return new KeyForDependentTypesHelper(this);
  }

  /**
   * Converts KeyFor annotations with errors into {@code @UnknownKeyFor} in the type of method
   * invocations. This changes all qualifiers on the type of a method invocation expression, even
   * qualifiers that are not primary annotations. This is unsound for qualifiers on type arguments
   * or array elements on modifiable objects.
   *
   * <p>For example, if the type of some method called, {@code getList(...)}, is changed from {@code
   * List<@KeyFor("a ? b : c") String>} to {@code List<@UnknownKeyFor String>}, then the returned
   * list may have @UnknownKeyFor strings added.
   *
   * <pre>{@code
   * List<String> l = getList(...);
   * l.add(randoString);
   * }</pre>
   *
   * This is probably ok for the KeyFor Checker because most of the collections of keys are
   * unmodifiable.
   */
  static class KeyForDependentTypesHelper extends DependentTypesHelper {

    /**
     * Creates a {@code KeyForDependentTypesHelper}.
     *
     * @param factory annotated type factory
     */
    public KeyForDependentTypesHelper(AnnotatedTypeFactory factory) {
      super(factory);
    }

    @Override
    public void atMethodInvocation(
        AnnotatedExecutableType methodType, MethodInvocationTree methodInvocationTree) {
      if (!hasDependentAnnotations()) {
        return;
      }
      Element methodElt = TreeUtils.elementFromUse(methodInvocationTree);

      // The annotations on `declaredMethodType` will be copied to `methodType`.
      AnnotatedExecutableType declaredMethodType =
          (AnnotatedExecutableType) factory.getAnnotatedType(methodElt);
      if (!hasDependentType(declaredMethodType)) {
        return;
      }

      StringToJavaExpression stringToJavaExpr;
      stringToJavaExpr =
          stringExpr -> {
            JavaExpression result =
                StringToJavaExpression.atMethodInvocation(
                    stringExpr, methodInvocationTree, factory.getChecker());
            Unknown unknown = result.containedOfClass(Unknown.class);
            if (unknown != null) {
              throw JavaExpressionParseUtil.constructJavaExpressionParseError(
                  result.toString(), "Expression " + unknown.toString() + " is unparsable.");
            }
            return result;
          };

      convertAnnotatedTypeMirror(stringToJavaExpr, declaredMethodType);
      this.viewpointAdaptedCopier.visit(declaredMethodType, methodType);
      this.errorAnnoReplacer.visit(methodType.getReturnType());
    }
  }
}
