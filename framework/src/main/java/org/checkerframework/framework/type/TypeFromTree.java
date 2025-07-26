package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.BugInCF;

/**
 * A utility class to convert trees into corresponding AnnotatedTypeMirrors. This class should be
 * used ONLY from AnnotatedTypeFactory.
 *
 * <p>For each method in TypeFromTree there is a corresponding TypeFromTreeVisitor that handles the
 * input tree. The list of methods implemented by these visitors outline which trees each method
 * will support. If a tree kind is not handled by the given visitor, then execution is halted and an
 * RuntimeException is thrown which includes a list of supported tree types.
 */
class TypeFromTree {

  private static final TypeFromTypeTreeVisitor typeTreeVisitor = new TypeFromTypeTreeVisitor();
  private static final TypeFromMemberVisitor memberVisitor = new TypeFromMemberVisitor();
  private static final TypeFromClassVisitor classVisitor = new TypeFromClassVisitor();
  private static final TypeFromExpressionVisitor expressionVisitor =
      new TypeFromExpressionVisitor();

  /**
   * Returns an AnnotatedTypeMirror representing the input expression tree.
   *
   * @param typeFactory type factory
   * @param tree must be an ExpressionTree
   * @return an AnnotatedTypeMirror representing the input expression tree
   */
  public static AnnotatedTypeMirror fromExpression(
      AnnotatedTypeFactory typeFactory, ExpressionTree tree) {
    abortIfTreeIsNull(typeFactory, tree);

    final AnnotatedTypeMirror type;
    try {
      type = expressionVisitor.visit(tree, typeFactory);
    } catch (Throwable t) {
      throw BugInCF.addLocation(tree, t);
    }
    ifExecutableCheckElement(typeFactory, tree, type);

    return type;
  }

  /**
   * Returns an AnnotatedTypeMirror representing the input tree.
   *
   * @param typeFactory type factory
   * @param tree must represent a class member
   * @return an AnnotatedTypeMirror representing the input tree
   */
  public static AnnotatedTypeMirror fromMember(AnnotatedTypeFactory typeFactory, Tree tree) {
    abortIfTreeIsNull(typeFactory, tree);
    AnnotatedTypeMirror type;
    try {
      type = memberVisitor.visit(tree, typeFactory);
    } catch (Throwable t) {
      throw BugInCF.addLocation(tree, t);
    }
    ifExecutableCheckElement(typeFactory, tree, type);
    return type;
  }

  /**
   * Returns an AnnotatedTypeMirror representing the input type tree.
   *
   * @param typeFactory type factory
   * @param tree must be a type tree
   * @return an AnnotatedTypeMirror representing the input type tree
   */
  public static AnnotatedTypeMirror fromTypeTree(AnnotatedTypeFactory typeFactory, Tree tree) {
    abortIfTreeIsNull(typeFactory, tree);
    AnnotatedTypeMirror type;
    try {
      type = typeTreeVisitor.visit(tree, typeFactory);
    } catch (Throwable t) {
      throw BugInCF.addLocation(tree, t);
    }
    abortIfTypeIsExecutable(typeFactory, tree, type);
    return type;
  }

  /**
   * Returns an AnnotatedDeclaredType representing the input ClassTree.
   *
   * @param typeFactory type factory
   * @param tree class tree
   * @return an AnnotatedDeclaredType representing the input ClassTree
   */
  public static AnnotatedDeclaredType fromClassTree(
      AnnotatedTypeFactory typeFactory, ClassTree tree) {
    abortIfTreeIsNull(typeFactory, tree);
    AnnotatedDeclaredType type;
    try {
      type = (AnnotatedDeclaredType) classVisitor.visit(tree, typeFactory);
    } catch (Throwable t) {
      throw BugInCF.addLocation(tree, t);
    }
    abortIfTypeIsExecutable(typeFactory, tree, type);
    return type;
  }

  protected static void abortIfTreeIsNull(AnnotatedTypeFactory typeFactory, Tree tree) {
    if (tree == null) {
      throw new BugInCF("Encountered null tree" + summarize(typeFactory, tree));
    }
  }

  protected static void ifExecutableCheckElement(
      AnnotatedTypeFactory typeFactory, Tree tree, AnnotatedTypeMirror type) {
    if (type.getKind() == TypeKind.EXECUTABLE) {
      if (((AnnotatedExecutableType) type).getElement() == null) {
        throw new BugInCF("Executable has no element:%n%s", summarize(typeFactory, tree, type));
      }
    }
  }

  protected static void abortIfTypeIsExecutable(
      AnnotatedTypeFactory typeFactory, Tree tree, AnnotatedTypeMirror type) {
    if (type.getKind() == TypeKind.EXECUTABLE) {
      throw new BugInCF("Unexpected Executable typekind:%n%s", summarize(typeFactory, tree, type));
    }
  }

  /**
   * Returns a string with the two arguments, for diagnostics.
   *
   * @param typeFactory a type factory
   * @param tree a tree
   * @return a string with the two arguments
   */
  protected static String summarize(AnnotatedTypeFactory typeFactory, Tree tree) {
    return String.format("tree=%s%ntypeFactory=%s", tree, typeFactory.getClass().getSimpleName());
  }

  protected static String summarize(
      AnnotatedTypeFactory typeFactory, Tree tree, AnnotatedTypeMirror type) {
    return "type=" + type + System.lineSeparator() + summarize(typeFactory, tree);
  }
}
