package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A node for the instanceof operator:
 *
 * <p><em>x</em> instanceof <em>Point</em>
 */
public class InstanceOfNode extends Node {

  /** The value being tested. */
  protected final Node operand;

  /** The reference type being tested against. */
  protected final TypeMirror refType;

  /** The tree associated with this node. */
  protected final InstanceOfTree tree;

  /** The node of the binding variable is one exists. */
  protected final @Nullable LocalVariableNode bindingVariable;

  /** For Types.isSameType. */
  protected final Types types;

  /** Create an InstanceOfNode. */
  public InstanceOfNode(Tree tree, Node operand, TypeMirror refType, Types types) {
    this(tree, operand, null, refType, types);
  }

  /** Create an InstanceOfNode. */
  public InstanceOfNode(
      Tree tree, Node operand, LocalVariableNode bindingVariable, TypeMirror refType, Types types) {
    super(types.getPrimitiveType(TypeKind.BOOLEAN));
    assert tree.getKind() == Tree.Kind.INSTANCE_OF;
    this.tree = (InstanceOfTree) tree;
    this.operand = operand;
    this.refType = refType;
    this.types = types;
    this.bindingVariable = bindingVariable;
  }

  public Node getOperand() {
    return operand;
  }

  public @Nullable LocalVariableNode getBindingVariable() {
    return bindingVariable;
  }

  /** The reference type being tested against. */
  public TypeMirror getRefType() {
    return refType;
  }

  @Override
  public InstanceOfTree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitInstanceOf(this, p);
  }

  @Override
  public String toString() {
    return "(" + getOperand() + " instanceof " + TypesUtils.simpleTypeName(getRefType()) + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof InstanceOfNode)) {
      return false;
    }
    InstanceOfNode other = (InstanceOfNode) obj;
    // TODO: TypeMirror.equals may be too restrictive.
    // Check whether Types.isSameType is the better comparison.
    return getOperand().equals(other.getOperand())
        && types.isSameType(getRefType(), other.getRefType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(InstanceOfNode.class, getOperand());
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.singletonList(getOperand());
  }
}
