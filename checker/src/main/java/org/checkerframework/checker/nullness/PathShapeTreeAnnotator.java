package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.PathShapeClassifier.PathShape;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Implements the type rules of the Path Shape Checker that depend on the types of the receiver and
 * arguments of a method or constructor invocation, or on the values of compile-time constants.
 * These rules map one qualifier to a different one, so they cannot be expressed as annotations.
 *
 * <p>This class also refines the type arguments and array component type of each use of an
 * effectively final local variable, from the type of the variable's initializer. For example, given
 * {@code Stream<Path> s = Files.list(dir)}, the type of each use of {@code s} is {@code
 * Stream<@HasName Path>}. {@link PathShapeVisitor} does the same for the variable's declaration.
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
    ExecutableElement method = TreeUtils.elementFromUse(tree);
    Element clazz = method.getEnclosingElement();
    if (clazz.equals(factory.pathElement)) {
      visitPathMethod(tree, method, type);
    } else if (clazz.equals(factory.fileElement)) {
      visitFileMethod(tree, method, type);
    } else if (clazz.equals(factory.filesElement)) {
      visitFilesMethod(tree, method, type);
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
      case "listFiles" -> {
        if (type.getKind() == TypeKind.ARRAY) {
          factory.refine(
              ((AnnotatedArrayType) type).getComponentType(), listingShape(receiverShape(tree)));
        }
      }
      default -> {}
    }
  }

  /**
   * Refines the type of an invocation of a method in {@code java.nio.file.Files}.
   *
   * @param tree an invocation of a method in {@code Files}
   * @param method the invoked method
   * @param type the type of {@code tree}; is side-effected by this method
   */
  private void visitFilesMethod(
      MethodInvocationTree tree, ExecutableElement method, AnnotatedTypeMirror type) {
    List<? extends ExpressionTree> args = tree.getArguments();
    switch (method.getSimpleName().toString()) {
      case "createTempFile", "createTempDirectory" -> {
        if (firstParameterIsPath(method)) {
          factory.refine(type, listingShape(factory.getPathShape(args.get(0))));
        } else {
          // The result is relative to the java.io.tmpdir system property, which might be empty.
          factory.refine(type, PathShape.HAS_NAME);
        }
      }
      case "list", "newDirectoryStream" ->
          refineTypeArgument(type, listingShape(factory.getPathShape(args.get(0))));
      case "walk", "find" ->
          // The first element is the start path itself.
          refineTypeArgument(type, factory.getPathShape(args.get(0)));
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

  @Override
  public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror type) {
    factory.refineFromInitializer(TreeUtils.elementFromUse(tree), type);
    return super.visitIdentifier(tree, type);
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
   * Returns true if the first formal parameter of the given method is a {@code Path}.
   *
   * @param method a method
   * @return true if the first formal parameter of {@code method} is a {@code Path}
   */
  private boolean firstParameterIsPath(ExecutableElement method) {
    return !method.getParameters().isEmpty()
        && factory.pathElement.equals(
            TypesUtils.getTypeElement(method.getParameters().get(0).asType()));
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

  /**
   * Returns the path shape of an entry in a directory listing, such as the result of {@code
   * Files.list(dir)}, or of a temporary file that is created in a directory.
   *
   * @param dir the path shape of the directory
   * @return the path shape of an entry in {@code dir}
   */
  private static PathShape listingShape(PathShape dir) {
    // An entry is dir.resolve(name).  If dir is the empty path, the entry has no parent.
    return dir == PathShape.UNKNOWN ? PathShape.HAS_NAME : PathShape.HAS_PARENT;
  }

  /**
   * Refines the type argument of the given type, such as {@code Stream<Path>}, to the given path
   * shape.
   *
   * @param type a type with one type argument; is side-effected by this method
   * @param shape a path shape
   */
  private void refineTypeArgument(AnnotatedTypeMirror type, PathShape shape) {
    if (type.getKind() == TypeKind.DECLARED) {
      List<AnnotatedTypeMirror> typeArgs = ((AnnotatedDeclaredType) type).getTypeArguments();
      if (typeArgs.size() == 1 && typeArgs.get(0).getKind() == TypeKind.DECLARED) {
        factory.refine(typeArgs.get(0), shape);
      }
    }
  }
}
