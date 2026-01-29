package org.checkerframework.afu.annotator.scanner;

import com.google.common.base.Throwables;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

/** Utility methods relating to TreePaths. */
public class TreePathUtil {

  /** Do not instantiate this class. */
  private TreePathUtil() {
    throw new Error("Do not instantiate.");
  }

  public static boolean hasClassKind(Tree tree) {
    Tree.Kind kind = tree.getKind();
    // Tree.Kind.NEW_CLASS is excluded here because 1) there is no
    // type name to be annotated on an anonymous inner class, and
    // consequently 2) NEW_CLASS insertions are handled separately.
    return kind == Tree.Kind.CLASS
        || kind == Tree.Kind.INTERFACE
        || kind == Tree.Kind.ENUM
        || kind == Tree.Kind.ANNOTATION_TYPE;
  }

  /**
   * Returns the counting context for new, typecast, instanceof, and locals.
   *
   * @param path a path to a method or a field/instance/static initializer
   * @return the counting context for new, typecast, instanceof, and locals
   */
  public static TreePath findCountingContext(TreePath path) {
    while (path != null) {
      if (path.getLeaf() instanceof MethodTree || isFieldInit(path) || isInitBlock(path)) {
        return path;
      }
      path = path.getParentPath();
    }
    return path;
  }

  // classes

  /**
   * Returns the enclosing class.
   *
   * @param path a tree path
   * @return the enclosing class
   */
  public static TreePath findEnclosingClass(TreePath path) {
    while (!hasClassKind(path.getLeaf())
        || path.getParentPath().getLeaf() instanceof NewClassTree) {
      path = path.getParentPath();
      if (path == null) {
        return null;
      }
    }
    return path;
  }

  // methods

  /**
   * Returns the enclosing method.
   *
   * @param path a tree path
   * @return the enclosing method
   */
  public static TreePath findEnclosingMethod(TreePath path) {
    while (!(path.getLeaf() instanceof MethodTree)) {
      path = path.getParentPath();
      if (path == null) {
        return null;
      }
    }
    return path;
  }

  // Field Initializers

  /**
   * Returns true if this is a field initialization.
   *
   * @param path a tree path
   * @return true if this is a field initialization
   */
  public static boolean isFieldInit(TreePath path) {
    return path.getLeaf() instanceof VariableTree
        && path.getParentPath() != null
        && hasClassKind(path.getParentPath().getLeaf());
  }

  public static TreePath findEnclosingFieldInit(TreePath path) {
    while (!isFieldInit(path)) {
      path = path.getParentPath();
      if (path == null) {
        return null;
      }
    }
    return path;
  }

  // initializer blocks

  public static boolean isInitBlock(TreePath path, boolean isStatic) {
    return isInitBlock(path) && ((BlockTree) path.getLeaf()).isStatic() == isStatic;
  }

  public static boolean isInitBlock(TreePath path) {
    return path.getParentPath() != null
        && hasClassKind(path.getParentPath().getLeaf())
        && path.getLeaf() instanceof BlockTree;
  }

  public static TreePath findEnclosingInitBlock(TreePath path, boolean isStatic) {
    while (!isInitBlock(path, isStatic)) {
      path = path.getParentPath();
      if (path == null) {
        return null;
      }
    }
    return path;
  }

  public static boolean isStaticInit(TreePath path) {
    return isInitBlock(path, true);
  }

  public static TreePath findEnclosingStaticInit(TreePath path) {
    while (!isStaticInit(path)) {
      path = path.getParentPath();
      if (path == null) {
        return null;
      }
    }
    return path;
  }

  public static boolean isInstanceInit(TreePath path) {
    return isInitBlock(path, false);
  }

  public static TreePath findEnclosingInstanceInit(TreePath path) {
    while (!isInstanceInit(path)) {
      path = path.getParentPath();
      if (path == null) {
        return null;
      }
    }
    return path;
  }

  /**
   * Returns true if the given class contains some constructor.
   *
   * @param ct a class
   * @return true if the given class contains some constructor
   */
  public static boolean hasConstructor(ClassTree ct) {
    // A position such that any explicit constructor is strictly after it.  -1 until initialized.
    int ctPos = -1;

    for (Tree member : ct.getMembers()) {
      if (member instanceof MethodTree) {
        MethodTree method = (MethodTree) member;
        if (method.getName().contentEquals("<init>")) {
          // The tree contains the implicit default constructor if the user wrote no constructor,
          // so must check position too. :-(
          if (ctPos == -1) {
            ctPos = classTreePos(ct);
          }
          int mPos1 = ((JCTree.JCMethodDecl) method).getStartPosition();
          int mPos2 = ((JCTree) method.getBody()).getStartPosition();
          if (mPos1 > ctPos && mPos1 != mPos2) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns a position such that any explicit constructor should be strictly after it. The implicit
   * default constructor seems to be given a position immediately after the modifiers?
   *
   * @param ct a class
   * @return a position before all the members
   */
  private static int classTreePos(ClassTree ct) {
    ModifiersTree mods = ct.getModifiers();
    // +1 is a conservative estimate as there might be more whitespace after the modifiers.
    int ctPos = ((JCTree) mods).getStartPosition() + mods.toString().length() + 1;
    Tree extendsClause = ct.getExtendsClause();
    if (extendsClause != null) {
      ctPos = Math.max(ctPos, ((JCTree) extendsClause).getStartPosition());
    }
    for (Tree implementsClause : ct.getImplementsClause()) {
      ctPos = Math.max(ctPos, ((JCTree) implementsClause).getStartPosition());
    }
    return ctPos;
  }

  /**
   * Given a TreePath, returns the binary name of the class it references. Does not give the right
   * name for anonymous classes, including those defined within methods.
   *
   * @param path a path to a class definition
   * @return the binary name of the class
   */
  public static String getBinaryName(TreePath path) {
    String result = "";
    for (Tree t : path) {
      switch (t.getKind()) {
        case CLASS:
        case INTERFACE:
        case ENUM:
        case ANNOTATION_TYPE:
          ClassTree ct = (ClassTree) t;
          String className = ct.getSimpleName().toString();
          result = result.isEmpty() ? className : className + "$" + result;
          break;

        case METHOD:
          // Hack that works for the first class defined within a method.
          result = "1" + result;
          break;

        case COMPILATION_UNIT:
          CompilationUnitTree cut = (CompilationUnitTree) t;
          ExpressionTree pkgExp = cut.getPackageName();
          if (pkgExp == null) {
            return result;
          } else {
            return pkgExp.toString() + "." + result;
          }

        default:
          break;
      }
    }
    throw new Error("unreachable");
  }

  /**
   * Returns the end position of the given tree, see {@link
   * SourcePositions#getEndPosition(CompilationUnitTree, Tree)}.
   *
   * @param tree an AST node
   * @param unit the compilation unit that contains {@code tree}
   * @return the end position of the given tree
   */
  public static int getEndPosition(Tree tree, CompilationUnitTree unit) {
    try {
      return (int) GET_END_POS_HANDLE.invokeExact((JCTree) tree, (JCTree.JCCompilationUnit) unit);
    } catch (Throwable e) {
      Throwables.throwIfUnchecked(e);
      throw new AssertionError(e);
    }
  }

  /** The {@link MethodHandle} for retrieving the end position of a {@link JCTree}. */
  private static final MethodHandle GET_END_POS_HANDLE = getEndPosMethodHandle();

  /**
   * Returns the {@link MethodHandle} for retrieving the end position of a {@link JCTree}.
   *
   * @return the {@link MethodHandle} for retrieving the end position of a {@link JCTree}.
   */
  private static MethodHandle getEndPosMethodHandle() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      // JDK versions after https://bugs.openjdk.org/browse/JDK-8372948
      // (tree, unit) -> tree.getEndPosition()
      return MethodHandles.dropArguments(
          lookup.findVirtual(JCTree.class, "getEndPosition", MethodType.methodType(int.class)),
          1,
          JCTree.JCCompilationUnit.class);
    } catch (ReflectiveOperationException e1) {
      // JDK versions before https://bugs.openjdk.org/browse/JDK-8372948
      // (tree, unit) -> tree.getEndPosition(unit.endPositions)
      try {
        return MethodHandles.filterArguments(
            lookup.findVirtual(
                JCTree.class,
                "getEndPosition",
                MethodType.methodType(
                    int.class, Class.forName("com.sun.tools.javac.tree.EndPosTable"))),
            1,
            lookup
                .findVarHandle(
                    JCTree.JCCompilationUnit.class,
                    "endPositions",
                    Class.forName("com.sun.tools.javac.tree.EndPosTable"))
                .toMethodHandle(VarHandle.AccessMode.GET));
      } catch (ReflectiveOperationException e2) {
        e2.addSuppressed(e1);
        throw new LinkageError(e2.getMessage(), e2);
      }
    }
  }
}
