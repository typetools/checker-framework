package org.checkerframework.afu.scenelib.io;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Structure bundling an {@link ASTPath} with information about its starting point. Necessary
 * because the {@link ASTPath} structure does not include the declaration from which it originates.
 */
public class ASTRecord implements Comparable<ASTRecord> {
  /** The AST to which this {@code ASTRecord} pertains. */
  public final CompilationUnitTree ast;

  /** Name of the enclosing class declaration. */
  public final String className;

  /** Name of the enclosing method declaration, or null if there is none. */
  public final String methodName;

  /** Name of the enclosing variable declaration, or null if there is none. */
  public final String varName;

  /** Path through AST, from specified declaration to descendant node. */
  public final ASTPath astPath;

  /**
   * Creates a new ASTRecord.
   *
   * @param ast the AST to which this {@code ASTRecord} pertains
   * @param className name of the enclosing class declaration
   * @param methodName name of the enclosing method declaration, or null if there is none
   * @param varName name of the enclosing variable declaration, or null if there is none
   * @param astPath path through AST, from specified declaration to descendant node
   */
  public ASTRecord(
      CompilationUnitTree ast,
      String className,
      String methodName,
      String varName,
      ASTPath astPath) {
    this.ast = ast;
    this.className = className;
    this.methodName = methodName;
    this.varName = varName;
    // FIXME: ensure path is canonical
    if (varName != null) {
      // TODO?
    } else if (methodName != null) {
      int n = astPath.size();
      if (n > 0
          && astPath.get(0).getTreeKind() != Tree.Kind.METHOD
          && astPath.get(0).getTreeKind() != Tree.Kind.VARIABLE) {
        ASTPath bodyPath =
            ASTPath.empty().add(new ASTPath.ASTEntry(Tree.Kind.METHOD, ASTPath.BODY));
        for (int i = 0; i < n; i++) {
          bodyPath = bodyPath.add(astPath.get(i));
        }
        astPath = bodyPath;
      }
    }
    this.astPath = astPath;
  }

  public ASTRecord newArrayLevel(int depth) {
    return new ASTRecord(ast, className, methodName, varName, astPath.extendNewArray(depth));
  }

  public ASTRecord replacePath(ASTPath newPath) {
    return new ASTRecord(ast, className, methodName, varName, newPath);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ASTRecord && equals((ASTRecord) o);
  }

  public boolean equals(ASTRecord astRecord) {
    return compareTo(astRecord) == 0;
  }

  @Override
  public int compareTo(ASTRecord rec) {
    int d =
        ast == null
            ? rec.ast == null ? 0 : -1
            : rec.ast == null ? 1 : Integer.compare(ast.hashCode(), rec.ast.hashCode());
    if (d == 0) {
      d =
          className == null
              ? rec.className == null ? 0 : -1
              : rec.className == null ? 1 : className.compareTo(rec.className);
      if (d == 0) {
        d =
            methodName == null
                ? rec.methodName == null ? 0 : -1
                : rec.methodName == null ? 1 : methodName.compareTo(rec.methodName);
        if (d == 0) {
          d =
              varName == null
                  ? rec.varName == null ? 0 : -1
                  : rec.varName == null ? 1 : varName.compareTo(rec.varName);
          if (d == 0) {
            d =
                astPath == null
                    ? rec.astPath == null ? 0 : -1
                    : rec.astPath == null ? 1 : astPath.compareTo(rec.astPath);
          }
        }
      }
    }
    return d;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ast, className, methodName, varName, astPath);
  }

  /** Indicates whether this record identifies the given {@link TreePath}. */
  public boolean matches(TreePath treePath) {
    String clazz = null;
    String meth = null;
    String var = null;
    boolean matchVars = false; // members only!
    Deque<Tree> stack = new ArrayDeque<Tree>();
    for (Tree tree : treePath) {
      stack.push(tree);
    }
    while (!stack.isEmpty()) {
      Tree tree = stack.pop();
      switch (tree.getKind()) {
        case CLASS:
        case INTERFACE:
        case ENUM:
        case ANNOTATION_TYPE:
          clazz = ((ClassTree) tree).getSimpleName().toString();
          meth = null;
          var = null;
          matchVars = true;
          break;
        case METHOD:
          assert meth == null;
          meth = ((MethodTree) tree).getName().toString();
          matchVars = false;
          break;
        case VARIABLE:
          if (matchVars) {
            assert var == null;
            var = ((VariableTree) tree).getName().toString();
            matchVars = false;
          }
          break;
        default:
          matchVars = false;
          continue;
      }
    }
    return className.equals(clazz)
        && (methodName == null ? meth == null : methodName.equals(meth))
        && (varName == null ? var == null : varName.equals(var))
        && astPath.matches(treePath);
  }

  @Override
  public String toString() {
    return (className == null ? "" : className)
        + ":"
        + (methodName == null ? "" : methodName)
        + ":"
        + (varName == null ? "" : varName)
        + ":"
        + astPath;
  }

  public ASTRecord extend(ASTPath.ASTEntry entry) {
    return new ASTRecord(ast, className, methodName, varName, astPath.extend(entry));
  }

  public ASTRecord extend(Kind kind, String sel) {
    return extend(new ASTPath.ASTEntry(kind, sel));
  }

  public ASTRecord extend(Kind kind, String sel, int arg) {
    return extend(new ASTPath.ASTEntry(kind, sel, arg));
  }
}
