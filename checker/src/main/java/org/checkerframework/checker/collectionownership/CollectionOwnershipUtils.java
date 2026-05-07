package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;

/** Utility methods shared by {@link DisposalLoopInfo} scanning and AST matching. */
public final class CollectionOwnershipUtils {

  /** Do not instantiate */
  private CollectionOwnershipUtils() {
    throw new BugInCF("CollectionOwnershipUtils is a utility class and should not be instantiated");
  }

  /**
   * Returns the first CFG block associated with the given tree.
   *
   * @param cfg the CFG containing the tree
   * @param tree a tree
   * @return the first CFG block associated with {@code tree}, or {@code null} if none is known
   */
  static @Nullable Block firstBlockForTree(ControlFlowGraph cfg, Tree tree) {
    Set<Node> nodes = cfg.getNodesCorrespondingToTree(tree);
    if (nodes == null || nodes.isEmpty()) {
      return null;
    }
    for (Node n : nodes) {
      Block block = n.getBlock();
      if (block != null) {
        return block;
      }
    }
    return null;
  }

  /**
   * Returns an arbitrary CFG node associated with the given tree.
   *
   * @param cfg the CFG containing the tree
   * @param tree a tree
   * @return a CFG node associated with {@code tree}, or {@code null} if none is known
   */
  static @Nullable Node anyNodeForTree(ControlFlowGraph cfg, Tree tree) {
    Set<Node> nodes = cfg.getNodesCorrespondingToTree(tree);
    if (nodes == null || nodes.isEmpty()) {
      return null;
    }
    return nodes.iterator().next();
  }

  /**
   * Returns the CFG-associated tree for the given tree, if one exists; otherwise returns the
   * original tree.
   *
   * @param cfg the CFG containing the tree
   * @param tree the original tree
   * @return the CFG-associated tree for {@code tree}, or {@code tree} if no associated tree exists
   */
  static Tree cfgAssociatedTreeFor(ControlFlowGraph cfg, Tree tree) {
    Node node = anyNodeForTree(cfg, tree);
    if (node != null && node.getTree() != null) {
      return node.getTree();
    }
    return tree;
  }

  /**
   * Returns the {@code Name} of the identifier referenced by the given expression, or {@code null}
   * if the expression does not reference an identifier.
   *
   * @param expr an expression
   * @return the name of the referenced identifier, or {@code null} if none
   */
  static @Nullable Name getNameFromExpressionTree(@Nullable ExpressionTree expr) {
    if (expr == null) {
      return null;
    }
    switch (expr.getKind()) {
      case IDENTIFIER -> {
        return ((com.sun.source.tree.IdentifierTree) expr).getName();
      }
      case MEMBER_SELECT -> {
        MemberSelectTree mst = (MemberSelectTree) expr;
        Element elt = TreeUtils.elementFromUse(mst);
        if (elt.getKind() == ElementKind.FIELD) {
          return mst.getIdentifier();
        } else if (elt.getKind() == ElementKind.METHOD) {
          return getNameFromExpressionTree(mst.getExpression());
        } else {
          return null;
        }
      }
      case METHOD_INVOCATION -> {
        return getNameFromExpressionTree(((MethodInvocationTree) expr).getMethodSelect());
      }
      default -> {
        return null;
      }
    }
  }

  /**
   * Returns the {@code Name} of the identifier declared or referenced by the given statement, or
   * {@code null} if the statement does not declare or reference an identifier.
   *
   * @param expr the {@code StatementTree}
   * @return the name of the identifier declared or referenced by the statement, or {@code null} if
   *     none
   */
  static Name getNameFromStatementTree(StatementTree expr) {
    if (expr == null) {
      return null;
    }
    return switch (expr.getKind()) {
      case VARIABLE -> ((VariableTree) expr).getName();
      case EXPRESSION_STATEMENT ->
          getNameFromExpressionTree(((ExpressionStatementTree) expr).getExpression());
      default -> null;
    };
  }

  /**
   * Returns the expression that directly identifies the referenced value.
   *
   * <p>Identifiers and field accesses are returned unchanged. Method invocations are unwrapped to
   * their receiver expression. Returns {@code null} for expressions that do not identify a value.
   *
   * @param expr an expression
   * @return the expression that directly identifies the referenced value, or {@code null}
   */
  static ExpressionTree baseExpression(ExpressionTree expr) {
    switch (expr.getKind()) {
      case IDENTIFIER -> {
        return expr;
      }
      case MEMBER_SELECT -> {
        MemberSelectTree mst = (MemberSelectTree) expr;
        Element elt = TreeUtils.elementFromUse(mst);
        if (elt.getKind() == ElementKind.METHOD) {
          return ((MemberSelectTree) expr).getExpression();
        } else if (elt.getKind() == ElementKind.FIELD) {
          return expr;
        } else {
          return null;
        }
      }
      case METHOD_INVOCATION -> {
        return baseExpression(((MethodInvocationTree) expr).getMethodSelect());
      }
      default -> {
        return null;
      }
    }
  }

  /**
   * Returns all successor blocks for some block, except for those corresponding to ignored
   * exception types.
   *
   * @param block input block
   * @param atypeFactory the CO type factory used to ignore exception types
   * @return set of pairs (b, t), where b is a successor block, and t is the type of exception for
   *     the CFG edge from block to b, or {@code null} if b is a non-exceptional successor
   */
  static Set<IPair<Block, @Nullable TypeMirror>> getSuccessorsExceptIgnoredExceptions(
      Block block, CollectionOwnershipAnnotatedTypeFactory atypeFactory) {
    if (block.getType() == Block.BlockType.EXCEPTION_BLOCK) {
      ExceptionBlock exceptionBlock = (ExceptionBlock) block;
      Set<IPair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
      Block regularSuccessor = exceptionBlock.getSuccessor();
      if (regularSuccessor != null) {
        result.add(IPair.of(regularSuccessor, null));
      }
      Map<TypeMirror, Set<Block>> exceptionalSuccessors = exceptionBlock.getExceptionalSuccessors();
      for (Map.Entry<TypeMirror, Set<Block>> entry : exceptionalSuccessors.entrySet()) {
        TypeMirror exceptionType = entry.getKey();
        if (!atypeFactory.isIgnoredExceptionType(exceptionType)) {
          for (Block exceptionalSuccessor : entry.getValue()) {
            result.add(IPair.of(exceptionalSuccessor, exceptionType));
          }
        }
      }
      return result;
    } else {
      Set<IPair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
      for (Block successorBlock : block.getSuccessors()) {
        result.add(IPair.of(successorBlock, null));
      }
      return result;
    }
  }

  /**
   * Returns blocks reachable from {@code entryBlock}.
   *
   * @param entryBlock the CFG entry block
   * @return the reachable blocks
   */
  public static Set<Block> reachableFrom(Block entryBlock) {
    Set<Block> seen = new HashSet<>();
    ArrayDeque<Block> queue = new ArrayDeque<>();
    queue.add(entryBlock);
    seen.add(entryBlock);

    while (!queue.isEmpty()) {
      Block block = queue.remove();
      for (Block successor : block.getSuccessors()) {
        if (successor != null && seen.add(successor)) {
          queue.add(successor);
        }
      }
    }
    return seen;
  }
}
