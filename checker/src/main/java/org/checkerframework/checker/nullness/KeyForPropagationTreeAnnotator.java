package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.KeyForPropagator.PropagationDirection;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * For the following initializations we wish to propagate the annotations from the left-hand side to
 * the right-hand side or vice versa:
 *
 * <p>1. If a keySet is being saved to a newly declared set, we transfer the annotations from the
 * keySet to the lhs. e.g.,
 *
 * <pre>{@code
 * // The user is not required to explicitly annotate the LHS's type argument with @KeyFor("m")
 * Set<String> keySet = m.keySet();
 * }</pre>
 *
 * 2. If a variable declaration contains type arguments with an @KeyFor annotation and its
 * initializer is a new class tree with corresponding type arguments that have an @UknownKeyFor
 * primary annotation, we transfer from the LHS to RHS. e.g.,
 *
 * <pre>{@code
 * // The user does not have to write @KeyFor("m") on both sides
 * List<@KeyFor("m") String> keys = new ArrayList<String>();
 * }</pre>
 *
 * 3. IMPORTANT NOTE: The following case must be (and is) handled in KeyForAnnotatedTypeFactory. In
 * BaseTypeVisitor we check to make sure that the constructor called in a NewClassTree is actually
 * compatible with the annotations placed on the NewClassTree. This requires that, prior to this
 * check we also propagate the annotations to this constructor in constructorFromUse so that the
 * constructor call matches the type given to the NewClassTree.
 *
 * @see
 *     org.checkerframework.checker.nullness.KeyForAnnotatedTypeFactory#constructorFromUse(com.sun.source.tree.NewClassTree)
 *     <p>Note propagation only occurs between two AnnotatedDeclaredTypes. If either side is not an
 *     AnnotatedDeclaredType then this class does nothing.
 */
public class KeyForPropagationTreeAnnotator extends TreeAnnotator {
  private final KeyForPropagator keyForPropagator;
  private final ExecutableElement keySetMethod;

  public KeyForPropagationTreeAnnotator(
      AnnotatedTypeFactory atypeFactory, KeyForPropagator propagationTreeAnnotator) {
    super(atypeFactory);
    this.keyForPropagator = propagationTreeAnnotator;
    keySetMethod =
        TreeUtils.getMethod("java.util.Map", "keySet", 0, atypeFactory.getProcessingEnv());
  }

  /**
   * Returns true iff expression is a call to java.util.Map.KeySet.
   *
   * @return true iff expression is a call to java.util.Map.KeySet
   */
  public boolean isCallToKeyset(ExpressionTree expression) {
    return TreeUtils.isMethodInvocation(expression, keySetMethod, atypeFactory.getProcessingEnv());
  }

  /**
   * Transfers annotations on type arguments from the initializer to the variableTree, if the
   * initializer is a call to java.util.Map.keySet.
   */
  @Override
  public Void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
    super.visitVariable(variableTree, type);

    // This should only happen on Map.keySet();
    if (type.getKind() == TypeKind.DECLARED) {
      ExpressionTree initializer = variableTree.getInitializer();

      if (isCallToKeyset(initializer)) {
        AnnotatedDeclaredType variableType = (AnnotatedDeclaredType) type;
        AnnotatedTypeMirror initializerType = atypeFactory.getAnnotatedType(initializer);

        // Propagate just for declared (class) types, not for array types, boxed primitives,
        // etc.
        if (variableType.getKind() == TypeKind.DECLARED) {
          keyForPropagator.propagate(
              (AnnotatedDeclaredType) initializerType,
              variableType,
              PropagationDirection.TO_SUPERTYPE,
              atypeFactory);
        }
      }
    }

    return null;
  }

  /** Transfers annotations to type if the left-hand side is a variable declaration. */
  @Override
  public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
    keyForPropagator.propagateNewClassTree(tree, type, (KeyForAnnotatedTypeFactory) atypeFactory);
    return super.visitNewClass(tree, type);
  }

  /**
   * When visiting {@code Map.keySet()} calls, merge the map key's {@code @KeyFor} into the returned
   * Set element.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    if (isCallToKeyset(tree) && type.getKind() == TypeKind.DECLARED) {
      AnnotatedDeclaredType keySetReturnType = (AnnotatedDeclaredType) type;

      AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
      if (receiverType != null) {
        AnnotatedDeclaredType receiverDeclaredType = (AnnotatedDeclaredType) receiverType;
        mergeKeyForFromMapReceiverIntoKeySetReturn(
            receiverDeclaredType, keySetReturnType, (KeyForAnnotatedTypeFactory) atypeFactory);
      }
    }
    return super.visitMethodInvocation(tree, type);
  }

  /**
   * Merge {@code @KeyFor} annotations from a Map receiver's key type into a {@code keySet()} return
   * type.
   *
   * @param mapReceiverType the annotated type of the Map receiver
   * @param keySetReturnType the annotated type of the Set returned by {@code Map.keySet()}
   * @param factory the {@link KeyForAnnotatedTypeFactory} used to create and merge annotations
   */
  private void mergeKeyForFromMapReceiverIntoKeySetReturn(
      AnnotatedDeclaredType mapReceiverType,
      AnnotatedDeclaredType keySetReturnType,
      KeyForAnnotatedTypeFactory factory) {
    // Get the Map's first type argument (the key type).
    List<AnnotatedTypeMirror> mapTypeArgs = mapReceiverType.getTypeArguments();
    if (mapTypeArgs.isEmpty()) {
      return;
    }
    AnnotatedTypeMirror mapKeyType = mapTypeArgs.get(0);

    // Get the Set's first type argument (the element type).
    List<AnnotatedTypeMirror> setTypeArgs = keySetReturnType.getTypeArguments();
    if (setTypeArgs.isEmpty()) {
      return;
    }
    AnnotatedTypeMirror setElementType = setTypeArgs.get(0);

    // Extract KeyFor annotation from the Map's key type.
    AnnotationMirror mapKeyKeyFor = mapKeyType.getEffectiveAnnotation(KeyFor.class);
    if (mapKeyKeyFor == null) {
      return;
    }

    // Get the KeyFor values from the Map's key type.
    List<String> mapKeyForValues =
        AnnotationUtils.getElementValueArray(
            mapKeyKeyFor, factory.keyForValueElement, String.class);

    // Extract KeyFor annotation from the Set's element type.
    AnnotationMirror setElementKeyFor = setElementType.getEffectiveAnnotation(KeyFor.class);

    // Collect all KeyFor values.
    Set<String> mergedKeyForValues = new LinkedHashSet<>(mapKeyForValues);

    if (setElementKeyFor != null) {
      mergedKeyForValues.addAll(
          AnnotationUtils.getElementValueArray(
              setElementKeyFor, factory.keyForValueElement, String.class));
    }

    // Create a new KeyFor annotation with merged values.
    if (mergedKeyForValues.isEmpty()) {
      return;
    }
    AnnotationMirror mergedKeyFor;
    if (setElementKeyFor != null) {
      // Use greatestLowerBoundQualifiers to merge the annotations.
      mergedKeyFor =
          factory
              .getQualifierHierarchy()
              .greatestLowerBoundQualifiers(mapKeyKeyFor, setElementKeyFor);
    } else {
      // If setElementKeyFor is null, just use the mapKeyKeyFor (but we still need to create
      // a new annotation with the merged values in case there are additional values).
      mergedKeyFor = factory.createKeyForAnnotationMirrorWithValue(mergedKeyForValues);
    }
    if (mergedKeyFor != null) {
      setElementType.replaceAnnotation(mergedKeyFor);
    }
  }
}
