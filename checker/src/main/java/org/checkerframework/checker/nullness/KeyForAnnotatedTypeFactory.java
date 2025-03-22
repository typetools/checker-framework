package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SubtypeIsSupersetQualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
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
      // Special handling for static fields with KeyFor annotations.
      if (isUseOfStaticField(tree)) {
        // Get the element for the static field
        javax.lang.model.element.Element element =
            TreeUtils.elementFromUse((com.sun.source.tree.IdentifierTree) tree);

        // Get the name of class for the static field
        javax.lang.model.element.Element enclosingClass = element.getEnclosingElement();
        String className = enclosingClass.toString();

        // Get the class and field name from the map expression
        String mapClass = null;
        String mapField = null;

        int lastDotIndex = mapExpression.lastIndexOf(".");
        if (lastDotIndex > 0) {
          mapClass = mapExpression.substring(0, lastDotIndex);
          mapField = mapExpression.substring(lastDotIndex + 1);
        }

        // Check if any map in KeyFor annotation matches this map
        for (String map : maps) {
          if (map.startsWith("this.")) {
            String fieldName = map.substring(5);

            // Create canonical form by replacing "this." with class name
            String canonicalMapReference = className + "." + fieldName;

            // For static field access (Class.field) - direct match with canonical form
            if (mapExpression.equals(canonicalMapReference)) {
              return true;
            }
            // For instance access (instance.field) - field name match
            // This ensures static keys work with any instance's map
            if (mapField != null && mapField.equals(fieldName)) {
              // Verify mapClass is same as or subclass of className
              try {
                @ClassGetName String classGetName = mapClass;
                Class<?> mapClassType = Class.forName(classGetName);
                Class<?> keyClassType = Class.forName(className);

                if (keyClassType.isAssignableFrom(mapClassType)) {
                  return true;
                }
              } catch (ClassNotFoundException e) {
                // Fall back to string comparison
                if (mapClass.equals(className)) {
                  return true;
                }
              }
            }
          }
        }
      } else {
        KeyForValue value = getInferredValueFor(tree);
        if (value != null) {
          maps = value.getKeyForMaps();
        }
      }

      return maps != null && maps.contains(mapExpression);
    }
    return false;
  }

  /**
   * Returns true if the expression tree represents a use of a static field.
   *
   * @param tree the tree to check
   * @return true if the tree is a use of a static field
   */
  private boolean isUseOfStaticField(ExpressionTree tree) {
    if (!(tree instanceof com.sun.source.tree.IdentifierTree)) {
      return false;
    }
    javax.lang.model.element.Element element =
        TreeUtils.elementFromUse((com.sun.source.tree.IdentifierTree) tree);
    return element != null
        && element.getKind() == javax.lang.model.element.ElementKind.FIELD
        && element.getModifiers().contains(javax.lang.model.element.Modifier.STATIC);
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
}
