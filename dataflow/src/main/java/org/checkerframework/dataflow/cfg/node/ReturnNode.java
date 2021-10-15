package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for a return statement:
 *
 * <pre>
 *   return
 *   return <em>expression</em>
 * </pre>
 *
 * No ReturnNode is created for implicit return statements.
 */
public class ReturnNode extends Node {

  /** The return tree. */
  protected final ReturnTree returnTree;

  /** The node of the returned expression. */
  protected final @Nullable Node result;

  /**
   * Creates a node for the given return statement.
   *
   * @param returnTree return tree
   * @param result the returned expression
   * @param types types util
   */
  public ReturnNode(ReturnTree returnTree, @Nullable Node result, Types types) {
    super(types.getNoType(TypeKind.NONE));
    this.result = result;
    this.returnTree = returnTree;
  }

  /**
   * Creates a node for the given return statement.
   *
   * @param returnTree return tree
   * @param result the returned expression
   * @param types types util
   * @param methodTree method tree
   * @deprecated Use {@link #ReturnNode(ReturnTree, Node, Types, LambdaExpressionTree,
   *     MethodSymbol)} instead.
   */
  @Deprecated
  public ReturnNode(
      ReturnTree returnTree, @Nullable Node result, Types types, MethodTree methodTree) {
    this(returnTree, result, types);
  }

  /**
   * Creates a node for the given return statement.
   *
   * @param returnTree return tree
   * @param result the returned expression
   * @param types types util
   * @param lambda lambda
   * @param methodSymbol methodSymbol
   * @deprecated Use {@link #ReturnNode(ReturnTree, Node, Types, LambdaExpressionTree,
   *     MethodSymbol)} instead.
   */
  @Deprecated
  public ReturnNode(
      ReturnTree returnTree,
      @Nullable Node result,
      Types types,
      LambdaExpressionTree lambda,
      MethodSymbol methodSymbol) {
    this(returnTree, result, types);
  }

  /** The result of the return node, {@code null} otherwise. */
  public @Nullable Node getResult() {
    return result;
  }

  @Override
  public ReturnTree getTree() {
    return returnTree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitReturn(this, p);
  }

  @Override
  public String toString() {
    if (result != null) {
      return "return " + result;
    }
    return "return";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ReturnNode)) {
      return false;
    }
    ReturnNode other = (ReturnNode) obj;
    return Objects.equals(result, other.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ReturnNode.class, result);
  }

  @Override
  public Collection<Node> getOperands() {
    if (result == null) {
      return Collections.emptyList();
    } else {
      return Collections.singletonList(result);
    }
  }
}
