package org.checkerframework.checker.nullness;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * KeyForTransfer ensures that java.util.Map.put and containsKey cause the appropriate @KeyFor
 * annotation to be added to the key.
 */
public class KeyForTransfer extends CFAbstractTransfer<KeyForValue, KeyForStore, KeyForTransfer> {

  /** The KeyFor.value element/field. */
  protected final ExecutableElement keyForValueElement;

  /** The Map.keySet method. */
  private final ExecutableElement mapKeySet;

  /**
   * Creates a new KeyForTransfer.
   *
   * @param analysis the analysis
   */
  public KeyForTransfer(KeyForAnalysis analysis) {
    super(analysis);

    ProcessingEnvironment processingEnv =
        ((KeyForAnnotatedTypeFactory) analysis.getTypeFactory()).getProcessingEnv();
    keyForValueElement = TreeUtils.getMethod(KeyFor.class, "value", 0, processingEnv);
    mapKeySet = TreeUtils.getMethod("java.util.Map", "keySet", 0, processingEnv);
  }

  /*
   * Provided that m is of a type that implements interface java.util.Map:
   * <ul>
   * <li>Given a call m.containsKey(k), ensures that k is @KeyFor("m") in the thenStore of the transfer result.
   * <li>Given a call m.put(k, ...), ensures that k is @KeyFor("m") in the thenStore and elseStore of the transfer result.
   * <li>Given a call m.keySet(), merges KeyFor annotations from the Map receiver's type arguments into the return type's type arguments.
   * </ul>
   */
  @Override
  public TransferResult<KeyForValue, KeyForStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<KeyForValue, KeyForStore> in) {

    TransferResult<KeyForValue, KeyForStore> result = super.visitMethodInvocation(node, in);
    KeyForAnnotatedTypeFactory factory = (KeyForAnnotatedTypeFactory) analysis.getTypeFactory();

    if (isMapKeySet(node)) {
      // Use getReceiverType which may use cached type information from dataflow
      // This avoids recomputing the receiver type that dataflow already computed
      AnnotatedTypeMirror receiverType = factory.getReceiverType(node.getTree());
      if (receiverType != null && receiverType.getKind() == TypeKind.DECLARED) {
        AnnotatedDeclaredType receiverDeclaredType = (AnnotatedDeclaredType) receiverType;

        // Get the return type and modify it
        AnnotatedTypeMirror returnType = factory.getAnnotatedType(node.getTree());
        if (returnType.getKind() == TypeKind.DECLARED) {
          AnnotatedDeclaredType keySetReturnType = (AnnotatedDeclaredType) returnType;
          // Update the result value with the modified return type
          mergeKeyForFromMapReceiverIntoKeySetReturn(
              receiverDeclaredType, keySetReturnType, factory);

          // Extract the merged KeyFor annotation from the type argument and include it
          // in the abstract value, since createAbstractValue only processes primary annotations
          KeyForValue newValue = analysis.createAbstractValue(keySetReturnType);

          KeyForValue oldValue = result.getResultValue();
          if (oldValue != null) {
            // Use the more specific value (the one with merged annotations)
            KeyForValue mergedValue = newValue.mostSpecific(oldValue, newValue);
            result.setResultValue(mergedValue);
          } else {
            result.setResultValue(newValue);
          }
        }
      }
    } else if (factory.isMapContainsKey(node) || factory.isMapPut(node)) {

      Node receiver = node.getTarget().getReceiver();
      JavaExpression receiverJe = JavaExpression.fromNode(receiver);
      String mapName = receiverJe.toString();
      JavaExpression keyExpr = JavaExpression.fromNode(node.getArgument(0));

      LinkedHashSet<String> keyForMaps = new LinkedHashSet<>();
      keyForMaps.add(mapName);

      KeyForValue previousKeyValue = in.getValueOfSubNode(node.getArgument(0));
      if (previousKeyValue != null) {
        for (AnnotationMirror prevAm : previousKeyValue.getAnnotations()) {
          if (prevAm != null && factory.areSameByClass(prevAm, KeyFor.class)) {
            keyForMaps.addAll(getKeys(prevAm));
          }
        }
      }

      AnnotationMirror am = factory.createKeyForAnnotationMirrorWithValue(keyForMaps);

      if (factory.isMapContainsKey(node)) {
        // method is Map.containsKey
        result.getThenStore().insertValue(keyExpr, am);
      } else {
        // method is Map.put
        result.getThenStore().insertValue(keyExpr, am);
        result.getElseStore().insertValue(keyExpr, am);
      }
    }

    return result;
  }

  /**
   * Returns true if the node is an invocation of Map.keySet.
   *
   * @param node a node
   * @return true if the node is an invocation of Map.keySet
   */
  private boolean isMapKeySet(Node node) {
    return NodeUtils.isMethodInvocation(node, mapKeySet, analysis.getTypeFactory().getProcessingEnv());
  }

  /**
   * Returns the elements/arguments of a {@code @KeyFor} annotation.
   *
   * @param keyFor a {@code @KeyFor} annotation
   * @return the elements/arguments of a {@code @KeyFor} annotation
   */
  private Set<String> getKeys(AnnotationMirror keyFor) {
    if (keyFor.getElementValues().isEmpty()) {
      return Collections.emptySet();
    }

    return new LinkedHashSet<>(
        AnnotationUtils.getElementValueArray(keyFor, keyForValueElement, String.class));
  }

  /**
   * Merges KeyFor annotations from the Map receiver's first type argument (key type) into the Set's
   * first type argument (element type) in the keySet() return type.
   *
   * @param mapReceiverType the type of the Map receiver (e.g., Map&lt;@KeyFor("m") String,
   *     Integer&gt;)
   * @param keySetReturnType the return type of keySet() (e.g., Set&lt;@KeyFor("mapVar") String&gt;)
   * @param factory the type factory
   */
  private void mergeKeyForFromMapReceiverIntoKeySetReturn(
      AnnotatedDeclaredType mapReceiverType,
      AnnotatedDeclaredType keySetReturnType,
      KeyForAnnotatedTypeFactory factory) {
    // Get the Map's first type argument (the key type)
    List<AnnotatedTypeMirror> mapTypeArgs = mapReceiverType.getTypeArguments();
    if (mapTypeArgs.isEmpty()) {
      return;
    }
    AnnotatedTypeMirror mapKeyType = mapTypeArgs.get(0);

    // Get the Set's first type argument (the element type)
    List<AnnotatedTypeMirror> setTypeArgs = keySetReturnType.getTypeArguments();
    if (setTypeArgs.isEmpty()) {
      return;
    }
    AnnotatedTypeMirror setElementType = setTypeArgs.get(0);

    // Extract KeyFor annotation from the Map's key type
    AnnotationMirror mapKeyKeyFor = mapKeyType.getEffectiveAnnotation(KeyFor.class);
    if (mapKeyKeyFor == null) {
      return;
    }

    // Get the KeyFor values from the Map's key type
    List<String> mapKeyForValues =
        AnnotationUtils.getElementValueArray(mapKeyKeyFor, keyForValueElement, String.class);

    // Extract KeyFor annotation from the Set's element type
    AnnotationMirror setElementKeyFor = setElementType.getEffectiveAnnotation(KeyFor.class);

    // Collect all KeyFor values
    Set<String> mergedKeyForValues = new LinkedHashSet<>(mapKeyForValues);

    // Add existing KeyFor values from the Set's element type
    if (setElementKeyFor != null) {
      List<String> setKeyForValues =
          AnnotationUtils.getElementValueArray(setElementKeyFor, keyForValueElement, String.class);
      mergedKeyForValues.addAll(setKeyForValues);
    }

    // Create a new KeyFor annotation with merged values
    if (!mergedKeyForValues.isEmpty()) {
      AnnotationMirror mergedKeyFor;
      if (setElementKeyFor != null) {
        // Use greatestLowerBoundQualifiers to merge the annotations
        mergedKeyFor =
            factory
                .getQualifierHierarchy()
                .greatestLowerBoundQualifiers(mapKeyKeyFor, setElementKeyFor);
      } else {
        // If setElementKeyFor is null, create a new annotation with the merged values
        mergedKeyFor = factory.createKeyForAnnotationMirrorWithValue(mergedKeyForValues);
      }
      setElementType.replaceAnnotation(mergedKeyFor);
    }
  }
}

