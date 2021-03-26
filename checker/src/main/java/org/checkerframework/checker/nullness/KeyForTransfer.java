package org.checkerframework.checker.nullness;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * KeyForTransfer ensures that java.util.Map.put and containsKey cause the appropriate @KeyFor
 * annotation to be added to the key.
 */
public class KeyForTransfer extends CFAbstractTransfer<KeyForValue, KeyForStore, KeyForTransfer> {

  public KeyForTransfer(KeyForAnalysis analysis) {
    super(analysis);
  }

  /*
   * Provided that m is of a type that implements interface java.util.Map:
   * <ul>
   * <li>Given a call m.containsKey(k), ensures that k is @KeyFor("m") in the thenStore of the transfer result.
   * <li>Given a call m.put(k, ...), ensures that k is @KeyFor("m") in the thenStore and elseStore of the transfer result.
   * </ul>
   */
  @Override
  public TransferResult<KeyForValue, KeyForStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<KeyForValue, KeyForStore> in) {

    TransferResult<KeyForValue, KeyForStore> result = super.visitMethodInvocation(node, in);
    KeyForAnnotatedTypeFactory factory = (KeyForAnnotatedTypeFactory) analysis.getTypeFactory();
    if (factory.isMapContainsKey(node) || factory.isMapPut(node)) {

      Node receiver = node.getTarget().getReceiver();
      JavaExpression receiverJe = JavaExpression.fromNode(receiver);
      String mapName = receiverJe.toString();
      JavaExpression keyExpr = JavaExpression.fromNode(node.getArgument(0));

      LinkedHashSet<String> keyForMaps = new LinkedHashSet<>();
      keyForMaps.add(mapName);

      final KeyForValue previousKeyValue = in.getValueOfSubNode(node.getArgument(0));
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
   * Returns the elements/arguments of a {@code @KeyFor} annotation.
   *
   * @param keyFor a {@code @KeyFor} annotation
   * @return the elements/arguments of a {@code @KeyFor} annotation
   */
  private Set<String> getKeys(final AnnotationMirror keyFor) {
    if (keyFor.getElementValues().isEmpty()) {
      return Collections.emptySet();
    }

    return new LinkedHashSet<>(
        AnnotationUtils.getElementValueArray(keyFor, "value", String.class, false));
  }
}
