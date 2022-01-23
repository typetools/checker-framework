package org.checkerframework.dataflow.cfg;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.util.StringsPlume;
import org.plumelib.util.UniqueId;

/**
 * Represents an abstract syntax tree of type {@link Tree} that underlies a given control flow
 * graph.
 */
public abstract class UnderlyingAST implements UniqueId {
  /** The kinds of underlying ASTs. */
  public enum Kind {
    /** The underlying code is a whole method. */
    METHOD,
    /** The underlying code is a lambda expression. */
    LAMBDA,

    /** The underlying code is an arbitrary Java statement or expression. */
    ARBITRARY_CODE,
  }

  /** The kind of the underlying AST. */
  protected final Kind kind;

  /** The unique ID for the next-created object. */
  static final AtomicLong nextUid = new AtomicLong(0);
  /** The unique ID of this object. */
  final transient long uid = nextUid.getAndIncrement();

  @Override
  public long getUid(@UnknownInitialization UnderlyingAST this) {
    return uid;
  }

  /**
   * Creates an UnderlyingAST.
   *
   * @param kind the kind of the AST
   */
  protected UnderlyingAST(Kind kind) {
    this.kind = kind;
  }

  /**
   * Returns the code that corresponds to the CFG. For a method or lamdda, this returns the body.
   * For other constructs, it returns the tree itself (a statement or expression).
   *
   * @return the code that corresponds to the CFG
   */
  public abstract Tree getCode();

  public Kind getKind() {
    return kind;
  }

  /** If the underlying AST is a method. */
  public static class CFGMethod extends UnderlyingAST {

    /** The method declaration. */
    protected final MethodTree method;

    /** The class tree this method belongs to. */
    protected final ClassTree classTree;

    public CFGMethod(MethodTree method, ClassTree classTree) {
      super(Kind.METHOD);
      this.method = method;
      this.classTree = classTree;
    }

    @Override
    public Tree getCode() {
      return method.getBody();
    }

    public MethodTree getMethod() {
      return method;
    }

    /**
     * Returns the name of the method.
     *
     * @return the name of the method
     */
    public String getMethodName() {
      return method.getName().toString();
    }

    /**
     * Returns the class tree this method belongs to.
     *
     * @return the class tree this method belongs to
     */
    public ClassTree getClassTree() {
      return classTree;
    }

    /**
     * Returns the simple name of the enclosing class.
     *
     * @return the simple name of the enclosing class
     */
    public String getSimpleClassName() {
      return classTree.getSimpleName().toString();
    }

    @Override
    public String toString() {
      return StringsPlume.joinLines("CFGMethod(", method, ")");
    }
  }

  /** If the underlying AST is a lambda. */
  public static class CFGLambda extends UnderlyingAST {

    /** The lambda expression. */
    private final LambdaExpressionTree lambda;

    /** The enclosing class of the lambda. */
    private final ClassTree classTree;

    /** The enclosing method of the lambda. */
    private final @Nullable MethodTree enclosingMethod;

    /**
     * Create a new CFGLambda.
     *
     * @param lambda the lambda expression
     * @param classTree the enclosing class of the lambda
     * @param enclosingMethod the enclosing method of the lambda
     */
    public CFGLambda(
        LambdaExpressionTree lambda, ClassTree classTree, @Nullable MethodTree enclosingMethod) {
      super(Kind.LAMBDA);
      this.lambda = lambda;
      this.enclosingMethod = enclosingMethod;
      this.classTree = classTree;
    }

    @Override
    public Tree getCode() {
      return lambda.getBody();
    }

    /**
     * Returns the lambda expression tree.
     *
     * @return the lambda expression tree
     */
    public LambdaExpressionTree getLambdaTree() {
      return lambda;
    }

    /**
     * Returns the enclosing class of the lambda.
     *
     * @return the enclosing class of the lambda
     */
    public ClassTree getClassTree() {
      return classTree;
    }

    /**
     * Returns the simple name of the enclosing class.
     *
     * @return the simple name of the enclosing class
     */
    public String getSimpleClassName() {
      return classTree.getSimpleName().toString();
    }

    /**
     * Returns the enclosing method of the lambda.
     *
     * @return the enclosing method of the lambda, or {@code null} if there is no enclosing method
     * @deprecated use #getEnclosingMethod()
     */
    @Deprecated // 2022-01-23
    public @Nullable MethodTree getMethod() {
      return enclosingMethod;
    }

    /**
     * Returns the enclosing method of the lambda.
     *
     * @return the enclosing method of the lambda, or {@code null} if there is no enclosing method
     */
    public @Nullable MethodTree getEnclosingMethod() {
      return enclosingMethod;
    }

    /**
     * Returns the name of the enclosing method of the lambda.
     *
     * @return the name of the enclosing method of the lambda, or {@code null} if there is no
     *     enclosing method
     * @deprecated use #getEnclosingMethodName()
     */
    @Deprecated // 2022-01-23
    public @Nullable String getMethodName() {
      return enclosingMethod == null ? null : enclosingMethod.getName().toString();
    }

    /**
     * Returns the name of the enclosing method of the lambda.
     *
     * @return the name of the enclosing method of the lambda, or {@code null} if there is no
     *     enclosing method
     */
    public @Nullable String getEnclosingMethodName() {
      return enclosingMethod == null ? null : enclosingMethod.getName().toString();
    }

    @Override
    public String toString() {
      return StringsPlume.joinLines("CFGLambda(", lambda, ")");
    }
  }

  /**
   * If the underlying AST is a statement or expression. This is for field definitions (with
   * initializers) and initializer blocks.
   */
  public static class CFGStatement extends UnderlyingAST {

    protected final Tree code;

    /** The class tree this method belongs to. */
    protected final ClassTree classTree;

    public CFGStatement(Tree code, ClassTree classTree) {
      super(Kind.ARBITRARY_CODE);
      this.code = code;
      this.classTree = classTree;
    }

    @Override
    public Tree getCode() {
      return code;
    }

    public ClassTree getClassTree() {
      return classTree;
    }

    /**
     * Returns the simple name of the enclosing class.
     *
     * @return the simple name of the enclosing class
     */
    public String getSimpleClassName() {
      return classTree.getSimpleName().toString();
    }

    @Override
    public String toString() {
      return StringsPlume.joinLines("CFGStatement(", code, ")");
    }
  }
}
