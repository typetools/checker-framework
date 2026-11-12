package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.PathShapeClassifier.PathShape;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Implements the type rules of the Path Shape Checker that depend on the types of the receiver and
 * arguments of a method or constructor invocation, or on the values of compile-time constants.
 * These rules map one qualifier to a different one, so they cannot be expressed as annotations.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
public class PathShapeTreeAnnotator extends TreeAnnotator {

  /** The type factory. */
  private final PathShapeAnnotatedTypeFactory factory;

  /**
   * Creates a new PathShapeTreeAnnotator.
   *
   * @param factory the type factory
   */
  public PathShapeTreeAnnotator(PathShapeAnnotatedTypeFactory factory) {
    super(factory);
    this.factory = factory;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    // An override is treated like the method in Path or File that it overrides.
    ExecutableElement method = factory.getPathOrFileMethod(TreeUtils.elementFromUse(tree));
    Element clazz = method.getEnclosingElement();
    if (clazz.equals(factory.pathElement)) {
      visitPathMethod(tree, method, type);
    } else if (clazz.equals(factory.fileElement)) {
      visitFileMethod(tree, method, type);
    } else if (clazz.equals(factory.pathsElement)) {
      if (method.getSimpleName().contentEquals("get")) {
        factory.refine(type, pathOfShape(tree));
      }
    }
    return super.visitMethodInvocation(tree, type);
  }

  /**
   * Refines the type of an invocation of a method in {@code java.nio.file.Path}.
   *
   * @param tree an invocation of a method in {@code Path}
   * @param method the invoked method
   * @param type the type of {@code tree}; is side-effected by this method
   */
  private void visitPathMethod(
      MethodInvocationTree tree, ExecutableElement method, AnnotatedTypeMirror type) {
    List<? extends ExpressionTree> args = tree.getArguments();
    switch (method.getSimpleName().toString()) {
      case "of" -> factory.refine(type, pathOfShape(tree));
      case "toAbsolutePath" -> {
        if (receiverShape(tree) != PathShape.UNKNOWN) {
          factory.refine(type, PathShape.HAS_PARENT);
        }
      }
      case "getFileName", "getName", "subpath" -> {
        if (receiverShape(tree) != PathShape.UNKNOWN) {
          factory.refine(type, PathShape.HAS_NAME);
        }
      }
      case "resolve" -> {
        if (args.size() > 1 && !TreeUtils.isVarargsCall(tree)) {
          // The other paths are passed in an array, whose contents are unknown.
          return;
        }
        PathShape result = receiverShape(tree);
        for (ExpressionTree arg : args) {
          result = resolveShape(result, factory.getPathShapeOfPathOrString(arg));
        }
        factory.refine(type, result);
      }
      case "resolveSibling" -> {
        if (args.size() == 1) {
          factory.refine(
              type,
              resolveSiblingShape(
                  receiverShape(tree), factory.getPathShapeOfPathOrString(args.get(0))));
        }
      }
      default -> {}
    }
  }

  /**
   * Refines the type of an invocation of a method in {@code java.io.File}.
   *
   * @param tree an invocation of a method in {@code File}
   * @param method the invoked method
   * @param type the type of {@code tree}; is side-effected by this method
   */
  private void visitFileMethod(
      MethodInvocationTree tree, ExecutableElement method, AnnotatedTypeMirror type) {
    switch (method.getSimpleName().toString()) {
      case "getAbsoluteFile" -> {
        if (receiverShape(tree) != PathShape.UNKNOWN) {
          factory.refine(type, PathShape.HAS_PARENT);
        }
      }
      default -> {}
    }
  }

  @Override
  public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
    if (tree.getClassBody() == null) {
      ExecutableElement constructor = TreeUtils.elementFromUse(tree);
      if (constructor.getEnclosingElement().equals(factory.fileElement)) {
        List<? extends ExpressionTree> args = tree.getArguments();
        // The constructors are File(String), File(String, String), File(File, String), and
        // File(URI).  For the two-argument constructors, the result has the child's name, and it
        // has a parent if the child does.  An empty or null parent does not change that.
        ExpressionTree child = args.get(args.size() - 1);
        String constant = TreeUtils.stringConstantValue(child);
        if (constant != null) {
          factory.refine(type, PathShapeClassifier.classifyFile(constant));
        }
      }
    }
    return super.visitNewClass(tree, type);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Helper methods
  //

  /**
   * Returns the path shape of the receiver of the given method invocation.
   *
   * @param tree a method invocation
   * @return the path shape of the receiver of {@code tree}
   */
  private PathShape receiverShape(MethodInvocationTree tree) {
    AnnotatedTypeMirror receiverType = factory.getReceiverType(tree);
    return receiverType == null ? PathShape.UNKNOWN : factory.getPathShape(receiverType);
  }

  /**
   * Returns the path shape of {@code Path.of(...)} or {@code Paths.get(...)}. It is {@code UNKNOWN}
   * unless every argument is a compile-time constant {@code String}.
   *
   * @param tree an invocation of {@code Path.of} or {@code Paths.get}
   * @return the path shape of the result of {@code tree}
   */
  private PathShape pathOfShape(MethodInvocationTree tree) {
    List<? extends ExpressionTree> args = tree.getArguments();
    if (args.size() > 1 && !TreeUtils.isVarargsCall(tree)) {
      // The other strings are passed in an array.
      return PathShape.UNKNOWN;
    }
    List<String> constants = new ArrayList<>(args.size());
    for (ExpressionTree arg : args) {
      String constant = TreeUtils.stringConstantValue(arg);
      if (constant == null) {
        return PathShape.UNKNOWN;
      }
      constants.add(constant);
    }
    return PathShapeClassifier.classifyPath(constants);
  }

  /**
   * Returns the path shape of {@code a.resolve(b)}.
   *
   * @param a the path shape of the receiver
   * @param b the path shape of the argument
   * @return the path shape of {@code a.resolve(b)}
   */
  /*package-private*/ static PathShape resolveShape(PathShape a, PathShape b) {
    // If b is absolute, the result is b.  If b is empty, the result is a.  Otherwise, the result
    // is b appended to a.
    if (b == PathShape.HAS_PARENT || (b == PathShape.HAS_NAME && a != PathShape.UNKNOWN)) {
      return PathShape.HAS_PARENT;
    } else {
      return b;
    }
  }

  /**
   * Returns the path shape of {@code a.resolveSibling(b)}.
   *
   * @param a the path shape of the receiver
   * @param b the path shape of the argument
   * @return the path shape of {@code a.resolveSibling(b)}
   */
  /*package-private*/ static PathShape resolveSiblingShape(PathShape a, PathShape b) {
    // The result is b if a has no parent, and a.getParent().resolve(b) otherwise.
    if (b == PathShape.HAS_PARENT || (b == PathShape.HAS_NAME && a == PathShape.HAS_PARENT)) {
      return PathShape.HAS_PARENT;
    } else {
      return b;
    }
  }
}
