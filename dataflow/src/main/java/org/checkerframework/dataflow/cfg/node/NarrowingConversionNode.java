package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A node for the narrowing primitive conversion operation. See JLS 5.1.3 for the definition of
 * narrowing primitive conversion.
 *
 * <p>A {@link NarrowingConversionNode} does not correspond to any tree node in the parsed AST. It
 * is introduced when a value of some primitive type appears in a context that requires a different
 * primitive type to which the value's type does not widen. Usually the required type has fewer bits
 * of precision, as in {@code byte b = 1;}. The node also represents a conversion between char and
 * short, which have the same size, as in {@code short s = 'a';}, and the widening and narrowing
 * primitive conversion from byte to char (JLS 5.1.4), as in {@code char c = (byte) 1;}.
 */
public class NarrowingConversionNode extends Node {

  protected final Tree tree;
  protected final Node operand;

  public NarrowingConversionNode(Tree tree, Node operand, TypeMirror type) {
    super(type);
    assert TypesUtils.isPrimitive(type) : "non-primitive type in narrowing conversion";
    this.tree = tree;
    this.operand = operand;
  }

  public Node getOperand() {
    return operand;
  }

  @Override
  public Tree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitNarrowingConversion(this, p);
  }

  @Override
  public String toString() {
    return "NarrowingConversion(" + getOperand() + ", " + type + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof NarrowingConversionNode other)) {
      return false;
    }
    return getOperand().equals(other.getOperand())
        && TypesUtils.areSamePrimitiveTypes(getType(), other.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(NarrowingConversionNode.class, getOperand());
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    return Collections.singletonList(getOperand());
  }
}
