package org.checkerframework.javacutil;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.SwitchExpressionUtils;
import org.plumelib.util.IPair;

/**
 * Utility methods for obtaining or analyzing a javac {@code TreePath}.
 *
 * @see TreeUtils
 */
public final class TreePathUtil {

  /** Do not instantiate; this class is a collection of static methods. */
  private TreePathUtil() {
    throw new BugInCF("Class TreeUtils cannot be instantiated.");
  }

  //
  // Retrieving a path (from another path)
  //

  /**
   * Gets path to the first (innermost) enclosing tree of the given kind. May return {@code path}
   * itself.
   *
   * @param path the path defining the tree node
   * @param kind the kind of the desired tree
   * @return the path to the enclosing tree of the given type, {@code null} otherwise
   */
  public static @Nullable TreePath pathTillOfKind(TreePath path, Tree.Kind kind) {
    return pathTillOfKind(path, EnumSet.of(kind));
  }

  /**
   * Gets path to the first (innermost) enclosing tree with any one of the given kinds. May return
   * {@code path} itself.
   *
   * @param path the path defining the tree node
   * @param kinds the set of kinds of the desired tree
   * @return the path to the enclosing tree of the given type, {@code null} otherwise
   */
  public static @Nullable TreePath pathTillOfKind(TreePath path, Set<Tree.Kind> kinds) {
    for (TreePath p = path; p != null; p = p.getParentPath()) {
      if (kinds.contains(p.getLeaf().getKind())) {
        return p;
      }
    }
    return null;
  }

  /**
   * Gets path to the first (innermost) enclosing class tree, where class is defined by the {@link
   * TreeUtils#classTreeKinds()} method. May return {@code path} itself.
   *
   * @param path the path defining the tree node
   * @return the path to the enclosing class tree, {@code null} otherwise
   */
  public static @Nullable TreePath pathTillClass(TreePath path) {
    return pathTillOfKind(path, TreeUtils.classTreeKinds());
  }

  /**
   * Gets path to the first (innermost) enclosing method tree. May return {@code path} itself.
   *
   * @param path the path defining the tree node
   * @return the path to the enclosing class tree, {@code null} otherwise
   */
  public static @Nullable TreePath pathTillMethod(TreePath path) {
    return pathTillOfKind(path, Tree.Kind.METHOD);
  }

  //
  // Retrieving a tree (from a path)
  //

  /**
   * Gets the first (innermost) enclosing tree in path, of the given kind. May return the leaf of
   * {@code path} itself.
   *
   * @param path the path defining the tree node
   * @param kind the kind of the desired tree
   * @return the enclosing tree of the given type as given by the path, {@code null} otherwise
   */
  public static @Nullable Tree enclosingOfKind(TreePath path, Tree.Kind kind) {
    return enclosingOfKind(path, EnumSet.of(kind));
  }

  /**
   * Gets the first (innermost) enclosing tree in path, with any one of the given kinds. May return
   * the leaf of {@code path} itself.
   *
   * @param path the path defining the tree node
   * @param kinds the set of kinds of the desired tree
   * @return the enclosing tree of the given type as given by the path, {@code null} otherwise
   */
  public static @Nullable Tree enclosingOfKind(TreePath path, Set<Tree.Kind> kinds) {
    TreePath p = pathTillOfKind(path, kinds);
    return (p == null) ? null : p.getLeaf();
  }

  /**
   * Gets the first (innermost) enclosing tree in path, of the given class. May return the leaf of
   * {@code path} itself.
   *
   * @param <T> the type of {@code treeClass}
   * @param path the path defining the tree node
   * @param treeClass the class of the desired tree
   * @return the enclosing tree of the given type as given by the path, {@code null} otherwise
   */
  public static <T extends Tree> @Nullable T enclosingOfClass(TreePath path, Class<T> treeClass) {
    TreePath p = path;

    while (p != null) {
      Tree leaf = p.getLeaf();
      if (treeClass.isInstance(leaf)) {
        return treeClass.cast(leaf);
      }
      p = p.getParentPath();
    }

    return null;
  }

  /**
   * Gets the path to nearest enclosing declaration (class, method, or variable) of the tree node
   * defined by the given {@link TreePath}. May return the leaf of {@code path} itself.
   *
   * @param path the path defining the tree node
   * @return path to the nearest enclosing class/method/variable in the path, or {@code null} if one
   *     does not exist
   */
  public static @Nullable TreePath enclosingDeclarationPath(TreePath path) {
    return pathTillOfKind(path, TreeUtils.declarationTreeKinds());
  }

  /**
   * Gets the enclosing class of the tree node defined by the given {@link TreePath}. It returns a
   * {@link Tree}, from which {@code checkers.types.AnnotatedTypeMirror} or {@link Element} can be
   * obtained. May return the leaf of {@code path} itself.
   *
   * @param path the path defining the tree node
   * @return the enclosing class (or interface) as given by the path, or {@code null} if one does
   *     not exist
   */
  public static @Nullable ClassTree enclosingClass(TreePath path) {
    return (ClassTree) enclosingOfKind(path, TreeUtils.classTreeKinds());
  }

  /**
   * Gets the enclosing variable of a tree node defined by the given {@link TreePath}. May return
   * the leaf of {@code path} itself.
   *
   * @param path the path defining the tree node
   * @return the enclosing variable as given by the path, or {@code null} if one does not exist
   */
  public static @Nullable VariableTree enclosingVariable(TreePath path) {
    return (VariableTree) enclosingOfKind(path, Tree.Kind.VARIABLE);
  }

  /**
   * Gets the enclosing method of the tree node defined by the given {@link TreePath}. It returns a
   * {@link Tree}, from which an {@code checkers.types.AnnotatedTypeMirror} or {@link Element} can
   * be obtained. May return the leaf of {@code path} itself.
   *
   * <p>Also see {@code AnnotatedTypeFactory#getEnclosingMethod} and {@code
   * AnnotatedTypeFactory#getEnclosingClassOrMethod}, which do not require a TreePath.
   *
   * @param path the path defining the tree node
   * @return the enclosing method as given by the path, or {@code null} if one does not exist
   */
  public static @Nullable MethodTree enclosingMethod(TreePath path) {
    return (MethodTree) enclosingOfKind(path, Tree.Kind.METHOD);
  }

  /**
   * Gets the enclosing method or lambda expression of the tree node defined by the given {@link
   * TreePath}. It returns a {@link Tree}, from which an {@code checkers.types.AnnotatedTypeMirror}
   * or {@link Element} can be obtained. May return the leaf of {@code path} itself.
   *
   * @param path the path defining the tree node
   * @return the enclosing method or lambda as given by the path, or {@code null} if one does not
   *     exist
   */
  public static @Nullable Tree enclosingMethodOrLambda(TreePath path) {
    return enclosingOfKind(path, EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION));
  }

  /**
   * Returns the top-level block that encloses the given path, or null if none does. Never returns
   * the leaf of {@code path} itself.
   *
   * @param path a path
   * @return the top-level block that encloses the given path, or null if none does
   */
  public static @Nullable BlockTree enclosingTopLevelBlock(TreePath path) {
    TreePath parentPath = path.getParentPath();
    while (parentPath != null
        && !TreeUtils.classTreeKinds().contains(parentPath.getLeaf().getKind())) {
      path = parentPath;
      parentPath = parentPath.getParentPath();
    }
    if (path.getLeaf().getKind() == Tree.Kind.BLOCK) {
      return (BlockTree) path.getLeaf();
    }
    return null;
  }

  /**
   * Gets the first (innermost) enclosing tree in path, that is not a parenthesis. Never returns the
   * leaf of {@code path} itself.
   *
   * @param path the path defining the tree node
   * @return a pair of a non-parenthesis tree that contains the argument, and its child that is the
   *     argument or is a parenthesized version of it
   */
  public static IPair<Tree, Tree> enclosingNonParen(TreePath path) {
    TreePath parentPath = path.getParentPath();
    Tree enclosing = parentPath.getLeaf();
    Tree enclosingChild = path.getLeaf();
    while (enclosing.getKind() == Tree.Kind.PARENTHESIZED) {
      parentPath = parentPath.getParentPath();
      enclosingChild = enclosing;
      enclosing = parentPath.getLeaf();
    }
    return IPair.of(enclosing, enclosingChild);
  }

  /**
   * Returns the tree representing the context for the poly expression which is the leaf of {@code
   * treePath}. The context then can be used to find the target type of the poly expression. Returns
   * null if the leaf of {@code treePath} is not a poly expression.
   *
   * @param treePath a path. If the leaf of the path is a poly expression, then its context is
   *     returned.
   * @return the tree representing the context for the poly expression which is the leaf of {@code
   *     treePath}; or null if the leaf is not a poly expression
   */
  public static @Nullable Tree getContextForPolyExpression(TreePath treePath) {
    // If a lambda or a method reference is the expression in a type cast, then the type cast is
    // the context.  If a method or constructor invocation is the expression in a type cast,
    // then the invocation has no context.
    boolean isLambdaOrMethodRef =
        treePath.getLeaf().getKind() == Kind.LAMBDA_EXPRESSION
            || treePath.getLeaf().getKind() == Kind.MEMBER_REFERENCE;
    return getContextForPolyExpression(treePath, isLambdaOrMethodRef);
  }

  /**
   * Implementation of {@link #getContextForPolyExpression(TreePath)}.
   *
   * @param treePath a path
   * @param isLambdaOrMethodRef if the call is getting the context of a lambda or method reference
   * @return the assignment context as described, {@code null} otherwise
   */
  private static @Nullable Tree getContextForPolyExpression(
      TreePath treePath, boolean isLambdaOrMethodRef) {
    TreePath parentPath = treePath.getParentPath();

    if (parentPath == null) {
      return null;
    }

    Tree parent = parentPath.getLeaf();
    switch (parent.getKind()) {
      case ASSIGNMENT: // See below for CompoundAssignmentTree.
      case LAMBDA_EXPRESSION:
      case METHOD_INVOCATION:
      case NEW_ARRAY:
      case RETURN:
        return parent;
      case NEW_CLASS:
        @SuppressWarnings("interning:not.interned") // Checking for exact object.
        boolean enclosingExpr =
            ((NewClassTree) parent).getEnclosingExpression() == treePath.getLeaf();
        if (enclosingExpr) {
          return null;
        }
        return parent;
      case TYPE_CAST:
        if (isLambdaOrMethodRef) {
          return parent;
        } else {
          return null;
        }
      case VARIABLE:
        if (TreeUtils.isVariableTreeDeclaredUsingVar((VariableTree) parent)) {
          return null;
        }
        return parent;
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree cet = (ConditionalExpressionTree) parent;
        @SuppressWarnings("interning:not.interned") // AST node comparison
        boolean conditionIsLeaf = (cet.getCondition() == treePath.getLeaf());
        if (conditionIsLeaf) {
          // The assignment context for the condition is simply boolean.
          // No point in going on.
          return null;
        }
        // Otherwise use the context of the ConditionalExpressionTree.
        return getContextForPolyExpression(parentPath, isLambdaOrMethodRef);
      case PARENTHESIZED:
      case CASE:
        return getContextForPolyExpression(parentPath, isLambdaOrMethodRef);
      default:
        if (TreeUtils.isYield(parent)) {
          // A yield statement is only legal within a switch expression. Walk up the path
          // to the case tree instead of the switch expression tree so the code remains
          // backward compatible.
          TreePath pathToCase = pathTillOfKind(parentPath, Kind.CASE);
          assert pathToCase != null
              : "@AssumeAssertion(nullness): yield statements must be enclosed in a CaseTree";
          parentPath = pathToCase.getParentPath();
          parent = parentPath.getLeaf();
        }
        if (TreeUtils.isSwitchExpression(parent)) {
          @SuppressWarnings("interning:not.interned") // AST node comparison
          boolean switchIsLeaf = SwitchExpressionUtils.getExpression(parent) == treePath.getLeaf();
          if (switchIsLeaf) {
            // The assignment context for the switch selector expression is simply
            // boolean.
            // No point in going on.
            return null;
          }
          // Otherwise use the context of the ConditionalExpressionTree.
          return getContextForPolyExpression(parentPath, isLambdaOrMethodRef);
        }
        // 11 Tree.Kinds are CompoundAssignmentTrees,
        // so use instanceof rather than listing all 11.
        if (parent instanceof CompoundAssignmentTree) {
          return parent;
        }
        return null;
    }
  }

  //
  // Predicates
  //

  /**
   * Returns true if the tree is in a constructor or an initializer block.
   *
   * @param path the path to test
   * @return true if the path is in a constructor or an initializer block
   */
  public static boolean inConstructor(TreePath path) {
    MethodTree method = enclosingMethod(path);
    // If method is null, this is an initializer block.
    return method == null || TreeUtils.isConstructor(method);
  }

  /**
   * Returns true if the leaf of the tree path is in a static scope.
   *
   * @param path a TreePath whose leaf may or may not be in static scope
   * @return true if the leaf of the tree path is in a static scope
   */
  public static boolean isTreeInStaticScope(TreePath path) {
    MethodTree enclosingMethod = enclosingMethod(path);

    if (enclosingMethod != null) {
      return enclosingMethod.getModifiers().getFlags().contains(Modifier.STATIC);
    }
    // no enclosing method, check for static or initializer block
    BlockTree block = enclosingTopLevelBlock(path);
    if (block != null) {
      return block.isStatic();
    }

    // check if it's in a variable initializer
    Tree t = enclosingVariable(path);
    if (t != null) {
      return ((VariableTree) t).getModifiers().getFlags().contains(Modifier.STATIC);
    }
    ClassTree classTree = enclosingClass(path);
    if (classTree != null) {
      return classTree.getModifiers().getFlags().contains(Modifier.STATIC);
    }
    return false;
  }

  /**
   * Returns true if the path is to a top-level (not within a loop) assignment within an initializer
   * block. The initializer block might be instance or static. Will return true for a re-assignment
   * even if there is another initialization (within this initializer block, another initializer
   * block, a constructor, or the variable declaration).
   *
   * @param path the path to test
   * @return true if the path is to an initialization within an initializer block
   */
  public static boolean isTopLevelAssignmentInInitializerBlock(TreePath path) {
    TreePath origPath = path;
    if (path.getLeaf().getKind() != Tree.Kind.ASSIGNMENT) {
      return false;
    }
    path = path.getParentPath();
    if (path.getLeaf().getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
      return false;
    }
    Tree prevLeaf = path.getLeaf();
    path = path.getParentPath();

    for (Iterator<Tree> itor = path.iterator(); itor.hasNext(); ) {
      Tree leaf = itor.next();
      switch (leaf.getKind()) {
        case CLASS:
        case ENUM:
        case PARAMETERIZED_TYPE:
          return prevLeaf.getKind() == Tree.Kind.BLOCK;

        case COMPILATION_UNIT:
          throw new BugInCF("found COMPILATION_UNIT in " + toString(origPath));

        case DO_WHILE_LOOP:
        case ENHANCED_FOR_LOOP:
        case FOR_LOOP:
        case LAMBDA_EXPRESSION:
        case METHOD:
          return false;

        default:
          prevLeaf = leaf;
      }
    }
    throw new BugInCF("path did not contain method or class: " + toString(origPath));
  }

  //
  // Formatting
  //

  /**
   * Return a printed representation of a TreePath.
   *
   * @param path a TreePath
   * @return a printed representation of the given TreePath
   */
  public static String toString(TreePath path) {
    StringJoiner result = new StringJoiner(System.lineSeparator() + "    ");
    result.add("TreePath:");
    for (Tree t : path) {
      result.add(TreeUtils.toStringTruncated(t, 65) + " " + t.getKind());
    }
    return result.toString();
  }

  /**
   * Returns a string representation of the leaf of the given path, using {@link
   * TreeUtils#toStringTruncated}.
   *
   * @param path a path
   * @param length the maximum length for the result; must be at least 6
   * @return a one-line string representation of the leaf of the given path that is no longer than
   *     {@code length} characters long
   */
  public static String leafToStringTruncated(@Nullable TreePath path, int length) {
    if (path == null) {
      return "null";
    }
    return TreeUtils.toStringTruncated(path.getLeaf(), length);
  }

  /**
   * Retrieves the nearest enclosing method or class element for the specified path in the AST. This
   * utility method prioritizes method elements over class elements. It returns the element of the
   * closest method scope if available; otherwise, it defaults to the enclosing class scope.
   *
   * @param path the {@link TreePath} to analyze for the nearest enclosing scope.
   * @return the {@link Element} of the nearest enclosing method or class, or {@code null} if no
   *     such enclosing element can be found.
   */
  public static @Nullable Element findNearestEnclosingElement(TreePath path) {
    MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(path);
    if (enclosingMethodTree != null) {
      return TreeUtils.elementFromDeclaration(enclosingMethodTree);
    }
    ClassTree enclosingClassTree = TreePathUtil.enclosingClass(path);
    if (enclosingClassTree != null) {
      return TreeUtils.elementFromDeclaration(enclosingClassTree);
    }
    return null;
  }
}
