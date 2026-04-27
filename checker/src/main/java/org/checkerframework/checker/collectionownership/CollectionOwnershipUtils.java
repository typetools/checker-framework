package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;

/** Utility methods shared by disposal-loop scanning and matcher resolution. */
public final class CollectionOwnershipUtils {

  private CollectionOwnershipUtils() {}

  /**
   * Returns the enclosing method for the current loop, or {@code null} if the loop is inside a
   * lambda expression or a different method.
   *
   * @param methodTree the method currently being scanned, or {@code null}
   * @return the enclosing method for the current loop, or {@code null} if it is not part of the
   *     scanned method
   */
  static @Nullable MethodTree getEnclosingMethodForCollectionLoop(@Nullable MethodTree methodTree) {
    return methodTree;
  }

  /**
   * Returns the statements in a loop body, regardless of whether the body is a block.
   *
   * @param statement the loop body statement
   * @return the loop body statements, or {@code null} if {@code statement} is {@code null}
   */
  static @Nullable List<? extends StatementTree> getLoopBodyStatements(
      @Nullable StatementTree statement) {
    if (statement == null) {
      return null;
    }
    return statement instanceof BlockTree
        ? ((BlockTree) statement).getStatements()
        : Collections.singletonList(statement);
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
   * Returns the tree to use as the loop-condition key.
   *
   * @param cfg the CFG containing the tree
   * @param tree the original condition tree
   * @return the CFG-associated tree for {@code tree}, if one exists; otherwise {@code tree}
   */
  static Tree treeForLoopCondition(ControlFlowGraph cfg, Tree tree) {
    Node node = anyNodeForTree(cfg, tree);
    if (node != null && node.getTree() != null) {
      return node.getTree();
    }
    return tree;
  }

  /**
   * Returns the simple name of the identifier referenced by the given expression, or {@code null}
   * if the expression does not reference an identifier.
   *
   * @param expr an expression
   * @return the name of the referenced identifier, or {@code null} if none
   */
  static Name getNameFromExpressionTree(ExpressionTree expr) {
    if (expr == null) {
      return null;
    }
    switch (expr.getKind()) {
      case IDENTIFIER:
        return ((com.sun.source.tree.IdentifierTree) expr).getName();
      case MEMBER_SELECT:
        MemberSelectTree mst = (MemberSelectTree) expr;
        Element elt = TreeUtils.elementFromUse(mst);
        if (elt.getKind() == ElementKind.FIELD) {
          return mst.getIdentifier();
        } else if (elt.getKind() == ElementKind.METHOD) {
          return getNameFromExpressionTree(mst.getExpression());
        } else {
          return null;
        }
      case METHOD_INVOCATION:
        return getNameFromExpressionTree(((MethodInvocationTree) expr).getMethodSelect());
      default:
        return null;
    }
  }

  /**
   * Returns the simple name of the identifier declared or referenced by the given statement, or
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
    switch (expr.getKind()) {
      case VARIABLE:
        return ((VariableTree) expr).getName();
      case EXPRESSION_STATEMENT:
        return getNameFromExpressionTree(((ExpressionStatementTree) expr).getExpression());
      default:
        return null;
    }
  }

  /**
   * Returns the ExpressionTree of the collection in the given expression.
   *
   * @param expr ExpressionTree
   * @return the expression evaluates to or null if it doesn't
   */
  static ExpressionTree collectionTreeFromExpression(ExpressionTree expr) {
    switch (expr.getKind()) {
      case IDENTIFIER:
        return expr;
      case MEMBER_SELECT:
        MemberSelectTree mst = (MemberSelectTree) expr;
        Element elt = TreeUtils.elementFromUse(mst);
        if (elt.getKind() == ElementKind.METHOD) {
          return ((MemberSelectTree) expr).getExpression();
        } else if (elt.getKind() == ElementKind.FIELD) {
          return expr;
        } else {
          return null;
        }
      case METHOD_INVOCATION:
        return collectionTreeFromExpression(((MethodInvocationTree) expr).getMethodSelect());
      default:
        return null;
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
