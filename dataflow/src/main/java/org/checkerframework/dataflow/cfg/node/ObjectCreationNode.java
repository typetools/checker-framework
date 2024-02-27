package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.StringsPlume;

/**
 * A node for a new object creation.
 *
 * <pre>
 *   <em>new typeToInstantiate(arg1, arg2, ...)</em>
 *   <em>enclosingExpression.new typeToInstantiate(arg1, arg2, ...)</em>
 *   <em>enclosingExpression.new &lt;Ts&gt;typeToInstantiate(arg1, arg2, ...)</em>
 * </pre>
 *
 * <p>We use the term "typeToInstantiate" to represent what is called the "identifier" in {@link
 * NewClassTree} and what is called "ClassOrInterfaceTypeToInstantiate" in the
 * "ClassInstanceCreationExpression" in the JLS. The former term "identifier" is misleading, as this
 * can be a type with type arguments. The latter term "ClassOrInterfaceTypeToInstantiate" is rather
 * long and we shortened it to "typeToInstantiate".
 *
 * <p>Class type arguments can be accessed through the "typeToInstantiate" node. To access
 * constructor type arguments one needs to use the {@link NewClassTree}.
 */
public class ObjectCreationNode extends Node {

  /** The tree for the object creation. */
  protected final NewClassTree tree;

  /** The enclosing expression of the object creation or null. */
  protected final @Nullable Node enclosingExpression;

  /**
   * The type to instantiate node of the object creation. A non-generic typeToInstantiate node will
   * refer to a {@link ClassNameNode}, while a generic typeToInstantiate node will refer to a {@link
   * ParameterizedTypeNode}.
   */
  protected final Node typeToInstantiate;

  /** The arguments of the object creation. */
  protected final List<Node> arguments;

  /** Class body for anonymous classes, otherwise null. */
  protected final @Nullable ClassDeclarationNode classbody;

  /**
   * Constructs a {@link ObjectCreationNode}.
   *
   * @param tree the NewClassTree
   * @param enclosingExpr the enclosing expression Node if it exists, or null
   * @param typeToInstantiate the typeToInstantiate node
   * @param arguments the passed arguments
   * @param classbody the ClassDeclarationNode
   */
  public ObjectCreationNode(
      NewClassTree tree,
      @Nullable Node enclosingExpr,
      Node typeToInstantiate,
      List<Node> arguments,
      @Nullable ClassDeclarationNode classbody) {
    super(TreeUtils.typeOf(tree));
    this.tree = tree;
    this.enclosingExpression = enclosingExpr;
    this.typeToInstantiate = typeToInstantiate;
    this.arguments = arguments;
    this.classbody = classbody;
  }

  /**
   * Returns the constructor node.
   *
   * @return the constructor node
   * @deprecated use {@link #getTypeToInstantiate()}
   */
  @Deprecated // 2024-02-16
  public Node getConstructor() {
    return typeToInstantiate;
  }

  /**
   * Returns the typeToInstantiate node. A non-generic typeToInstantiate node can refer to a {@link
   * ClassNameNode}, while a generic typeToInstantiate node can refer to a {@link
   * ParameterizedTypeNode}.
   *
   * @return the typeToInstantiate node
   */
  @Pure
  public Node getTypeToInstantiate() {
    return typeToInstantiate;
  }

  /**
   * Returns the explicit arguments to the object creation.
   *
   * @return the arguments
   */
  @Pure
  public List<Node> getArguments() {
    return arguments;
  }

  /**
   * Returns the i-th explicit argument to the object creation.
   *
   * @param i the index of the argument
   * @return the argument
   */
  @Pure
  public Node getArgument(int i) {
    return arguments.get(i);
  }

  /**
   * Returns the enclosing expression node, which only exists if it is an inner class instantiation.
   *
   * @return the enclosing type expression node
   */
  @Pure
  public @Nullable Node getEnclosingExpression() {
    return enclosingExpression;
  }

  /**
   * Returns the classbody.
   *
   * @return the classbody
   */
  @Pure
  public @Nullable Node getClassBody() {
    return classbody;
  }

  @Override
  @Pure
  public NewClassTree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitObjectCreation(this, p);
  }

  @Override
  @SideEffectFree
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (enclosingExpression != null) {
      sb.append(enclosingExpression + ".");
    }
    sb.append("new ");
    if (!tree.getTypeArguments().isEmpty()) {
      sb.append("<");
      sb.append(StringsPlume.join(", ", tree.getTypeArguments()));
      sb.append(">");
    }
    sb.append(typeToInstantiate + "(");
    sb.append(StringsPlume.join(", ", arguments));
    sb.append(")");
    if (classbody != null) {
      // TODO: maybe this can be done nicer...
      sb.append(" ");
      sb.append(classbody.toString());
    }
    return sb.toString();
  }

  @Override
  @Pure
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ObjectCreationNode)) {
      return false;
    }
    ObjectCreationNode other = (ObjectCreationNode) obj;
    // TODO: See issue 470.
    if (typeToInstantiate == null && other.getTypeToInstantiate() != null) {
      return false;
    }

    return getTypeToInstantiate().equals(other.getTypeToInstantiate())
        && getArguments().equals(other.getArguments())
        && (getEnclosingExpression() == null
            ? null == other.getEnclosingExpression()
            : getEnclosingExpression().equals(other.getEnclosingExpression()));
  }

  @Override
  @SideEffectFree
  public int hashCode() {
    return Objects.hash(enclosingExpression, typeToInstantiate, arguments);
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    ArrayList<Node> list = new ArrayList<>(2 + arguments.size());
    if (enclosingExpression != null) {
      list.add(enclosingExpression);
    }
    list.add(typeToInstantiate);
    list.addAll(arguments);
    return list;
  }
}
