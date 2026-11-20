package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
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
  static @Nullable Name getNameFromExpressionTree(ExpressionTree expr) {
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
        ExpressionTree methodSelect = ((MethodInvocationTree) expr).getMethodSelect();
        // An unqualified call, such as getResources(), has no receiver to name.
        if (!(methodSelect instanceof MemberSelectTree)) {
          return null;
        }
        return getNameFromExpressionTree(methodSelect);
      }
      default -> {
        return null;
      }
    }
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
  static @Nullable ExpressionTree referenceExpression(ExpressionTree expr) {
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
        ExpressionTree methodSelect = ((MethodInvocationTree) expr).getMethodSelect();
        // An unqualified call, such as getResources(), has no receiver: the call is the value.
        if (!(methodSelect instanceof MemberSelectTree)) {
          return expr;
        }
        return referenceExpression(methodSelect);
      }
      default -> {
        return null;
      }
    }
  }

  /**
   * Methods that replace elements of a collection but are not annotated
   * {@code @CreatesCollectionObligation}.
   */
  private static final Set<String> REPLACING_METHODS = Set.of("set", "replaceAll");

  /**
   * Returns true if {@code invocation} inserts into or replaces elements of the collection named
   * {@code collectionName}. An iterator over the collection might not visit the new elements (for
   * example, the iterator of a {@code CopyOnWriteArrayList} iterates over a snapshot), so a loop
   * that iterates over the collection and does this is not a disposal loop.
   *
   * @param invocation a method invocation
   * @param collectionName the name of the iterated collection
   * @param coAtf the CO type factory
   * @return true if {@code invocation} inserts into or replaces elements of the collection
   */
  static boolean insertsIntoOrReplacesElementsOf(
      MethodInvocationTree invocation,
      Name collectionName,
      CollectionOwnershipAnnotatedTypeFactory coAtf) {
    if (!(invocation.getMethodSelect() instanceof MemberSelectTree memberSelectTree)) {
      return false;
    }
    if (getNameFromExpressionTree(memberSelectTree.getExpression()) != collectionName) {
      return false;
    }
    ExecutableElement method = TreeUtils.elementFromUse(invocation);
    return coAtf.isCreatesCollectionObligationMethod(method)
        || REPLACING_METHODS.contains(method.getSimpleName().toString());
  }

  /**
   * Returns true if {@code collection} is an owning collection field of {@code this}. A call whose
   * {@code @CreatesMustCallFor} target is {@code this} might insert into such a field; see {@link
   * #createsMustCallForThis}.
   *
   * @param collection a collection expression
   * @param coAtf the CO type factory
   * @return true if {@code collection} is an owning collection field of {@code this}
   */
  static boolean isOwningCollectionFieldOfThis(
      ExpressionTree collection, CollectionOwnershipAnnotatedTypeFactory coAtf) {
    return coAtf.isOwningCollectionField(collection) && TreeUtils.isSelfAccess(collection);
  }

  /**
   * Returns true if some {@code @CreatesMustCallFor} target of {@code invocation}, resolved at the
   * call site, is {@code this}. Such a call might insert into an owning collection field of {@code
   * this}.
   *
   * @param invocation a method invocation
   * @param cfg the CFG that contains {@code invocation}
   * @param coAtf the CO type factory
   * @return true if {@code invocation} creates a must-call obligation for {@code this}
   */
  static boolean createsMustCallForThis(
      MethodInvocationTree invocation,
      ControlFlowGraph cfg,
      CollectionOwnershipAnnotatedTypeFactory coAtf) {
    return anyNodeForTree(cfg, invocation) instanceof MethodInvocationNode node
        && coAtf.createsMustCallForThis(node);
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
