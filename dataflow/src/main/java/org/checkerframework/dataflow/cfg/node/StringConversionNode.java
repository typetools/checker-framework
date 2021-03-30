package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the string conversion operation. See JLS 5.1.11 for the definition of string
 * conversion.
 *
 * <p>A {@link StringConversionNode} does not correspond to any tree node in the parsed AST. It is
 * introduced when a value of non-string type appears in a context that requires a {@link String},
 * such as in a string concatenation. A {@link StringConversionNode} should be treated as a
 * potential call to the toString method of its operand, but does not necessarily call any method
 * because null is converted to the string "null".
 *
 * <p>Conversion of primitive types to Strings requires first boxing and then string conversion.
 */
public class StringConversionNode extends Node {

  protected final Tree tree;
  protected final Node operand;

  // TODO: The type of a string conversion should be a final TypeMirror representing
  // java.lang.String. Currently we require the caller to pass in a TypeMirror instead of creating
  // one through the javax.lang.model.type.Types interface.
  public StringConversionNode(Tree tree, Node operand, TypeMirror type) {
    super(type);
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
    return visitor.visitStringConversion(this, p);
  }

  @Override
  public String toString() {
    return "StringConversion(" + getOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof StringConversionNode)) {
      return false;
    }
    StringConversionNode other = (StringConversionNode) obj;
    return getOperand().equals(other.getOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(StringConversionNode.class, getOperand());
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.singletonList(getOperand());
  }
}
