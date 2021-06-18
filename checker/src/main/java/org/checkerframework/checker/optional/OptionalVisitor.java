package org.checkerframework.checker.optional;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The OptionalVisitor enforces the Optional Checker rules. These rules are described in the Checker
 * Framework Manual.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
public class OptionalVisitor
    extends BaseTypeVisitor</* OptionalAnnotatedTypeFactory*/ BaseAnnotatedTypeFactory> {

  private final TypeMirror collectionType;

  /** The element for java.util.Optional.get(). */
  private final ExecutableElement optionalGet;
  /** The element for java.util.Optional.isPresent(). */
  private final ExecutableElement optionalIsPresent;
  /** The element for java.util.Optional.isEmpty(), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalIsEmpty;
  /** The element for java.util.Optional.of(). */
  private final ExecutableElement optionalOf;
  /** The element for java.util.Optional.ofNullable(). */
  private final ExecutableElement optionalOfNullable;
  /** The element for java.util.Optional.orElse(). */
  private final ExecutableElement optionalOrElse;
  /** The element for java.util.Optional.orElseGet(). */
  private final ExecutableElement optionalOrElseGet;
  /** The element for java.util.Optional.orElseThrow(). */
  private final @Nullable ExecutableElement optionalOrElseThrow;
  /** The element for java.util.Optional.orElseThrow(Supplier), or null if running under JDK 8. */
  private final ExecutableElement optionalOrElseThrowSupplier;

  /** Create an OptionalVisitor. */
  public OptionalVisitor(BaseTypeChecker checker) {
    super(checker);
    collectionType = types.erasure(TypesUtils.typeFromClass(Collection.class, types, elements));

    ProcessingEnvironment env = checker.getProcessingEnvironment();
    optionalGet = TreeUtils.getMethod("java.util.Optional", "get", 0, env);
    optionalIsPresent = TreeUtils.getMethod("java.util.Optional", "isPresent", 0, env);
    optionalIsEmpty = TreeUtils.getMethodOrNull("java.util.Optional", "isEmpty", 0, env);
    optionalOf = TreeUtils.getMethod("java.util.Optional", "of", 1, env);
    optionalOfNullable = TreeUtils.getMethod("java.util.Optional", "ofNullable", 1, env);
    optionalOrElse = TreeUtils.getMethod("java.util.Optional", "orElse", 1, env);
    optionalOrElseGet = TreeUtils.getMethod("java.util.Optional", "orElseGet", 1, env);
    optionalOrElseThrow = TreeUtils.getMethodOrNull("java.util.Optional", "orElseThrow", 0, env);
    optionalOrElseThrowSupplier = TreeUtils.getMethod("java.util.Optional", "orElseThrow", 1, env);
  }

  @Override
  protected BaseTypeValidator createTypeValidator() {
    return new OptionalTypeValidator(checker, this, atypeFactory);
  }

  /**
   * Returns true iff {@code expression} is a call to java.util.Optional.get.
   *
   * @param expression an expression
   * @return true iff {@code expression} is a call to java.util.Optional.get
   */
  private boolean isCallToGet(ExpressionTree expression) {
    ProcessingEnvironment env = checker.getProcessingEnvironment();
    return TreeUtils.isMethodInvocation(expression, optionalGet, env);
  }

  /**
   * Is the expression a call to {@code isPresent} or {@code isEmpty}? If not, returns null. If so,
   * returns a pair of (boolean, receiver expression). The boolean is true if the given expression
   * is a call to {@code isPresent} and is false if the given expression is a call to {@code
   * isEmpty}.
   *
   * @param expression an expression
   * @return a pair of a boolean (indicating whether the expression is a call to {@code
   *     Optional.isPresent} or to {@code Optional.isEmpty}) and its receiver; or null if not a call
   *     to either of the methods
   */
  private @Nullable Pair<Boolean, ExpressionTree> isCallToIsPresent(ExpressionTree expression) {
    ProcessingEnvironment env = checker.getProcessingEnvironment();
    boolean negate = false;
    while (true) {
      switch (expression.getKind()) {
        case PARENTHESIZED:
          expression = ((ParenthesizedTree) expression).getExpression();
          break;
        case LOGICAL_COMPLEMENT:
          expression = ((UnaryTree) expression).getExpression();
          negate = !negate;
          break;
        case METHOD_INVOCATION:
          if (TreeUtils.isMethodInvocation(expression, optionalIsPresent, env)) {
            return Pair.of(!negate, TreeUtils.getReceiverTree(expression));
          } else if (optionalIsEmpty != null
              && TreeUtils.isMethodInvocation(expression, optionalIsEmpty, env)) {
            return Pair.of(negate, TreeUtils.getReceiverTree(expression));
          } else {
            return null;
          }
        default:
          return null;
      }
    }
  }

  /**
   * Returns true iff the method being callid is Optional creation: of, ofNullable.
   *
   * @param methInvok a method invocation
   * @return true iff the method being called is Optional creation: of, ofNullable
   */
  private boolean isOptionalCreation(MethodInvocationTree methInvok) {
    ProcessingEnvironment env = checker.getProcessingEnvironment();
    return TreeUtils.isMethodInvocation(methInvok, optionalOf, env)
        || TreeUtils.isMethodInvocation(methInvok, optionalOfNullable, env);
  }

  /**
   * Returns true iff the method being called is Optional elimination: get, orElse, orElseGet,
   * orElseThrow.
   *
   * @param methInvok a method invocation
   * @return true iff the method being called is Optional elimination: get, orElse, orElseGet,
   *     orElseThrow
   */
  private boolean isOptionalElimation(MethodInvocationTree methInvok) {
    ProcessingEnvironment env = checker.getProcessingEnvironment();
    return TreeUtils.isMethodInvocation(methInvok, optionalGet, env)
        || TreeUtils.isMethodInvocation(methInvok, optionalOrElse, env)
        || TreeUtils.isMethodInvocation(methInvok, optionalOrElseGet, env)
        || (optionalIsEmpty != null
            && TreeUtils.isMethodInvocation(methInvok, optionalOrElseThrow, env))
        || TreeUtils.isMethodInvocation(methInvok, optionalOrElseThrowSupplier, env);
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
    handleTernaryIsPresentGet(node);
    return super.visitConditionalExpression(node, p);
  }

  /**
   * Part of rule #3.
   *
   * <p>Pattern match for: {@code VAR.isPresent() ? VAR.get().METHOD() : VALUE}
   *
   * <p>Prefer: {@code VAR.map(METHOD).orElse(VALUE);}
   */
  // TODO: Should handle this via a transfer function, instead of pattern-matching.
  public void handleTernaryIsPresentGet(ConditionalExpressionTree node) {

    ExpressionTree condExpr = TreeUtils.withoutParens(node.getCondition());
    Pair<Boolean, ExpressionTree> isPresentCall = isCallToIsPresent(condExpr);
    if (isPresentCall == null) {
      return;
    }
    ExpressionTree trueExpr = TreeUtils.withoutParens(node.getTrueExpression());
    ExpressionTree falseExpr = TreeUtils.withoutParens(node.getFalseExpression());
    if (!isPresentCall.first) {
      ExpressionTree tmp = trueExpr;
      trueExpr = falseExpr;
      falseExpr = tmp;
    }

    if (trueExpr.getKind() != Kind.METHOD_INVOCATION) {
      return;
    }
    ExpressionTree trueReceiver = TreeUtils.getReceiverTree(trueExpr);
    if (!isCallToGet(trueReceiver)) {
      return;
    }
    ExpressionTree getReceiver = TreeUtils.getReceiverTree(trueReceiver);

    // What is a better way to do this than string comparison?
    // Use transfer functions and Store entries.
    ExpressionTree receiver = isPresentCall.second;
    if (sameExpression(receiver, getReceiver)) {
      ExecutableElement ele = TreeUtils.elementFromUse((MethodInvocationTree) trueExpr);

      checker.reportWarning(
          node,
          "prefer.map.and.orelse",
          receiver,
          // The literal "CONTAININGCLASS::" is gross.
          // TODO: add this to the error message.
          // ElementUtils.getQualifiedClassName(ele);
          ele.getSimpleName(),
          falseExpr);
    }
  }

  /**
   * Returns true if the two trees represent the same expression.
   *
   * @param tree1 the first tree
   * @param tree2 the second tree
   * @return true if the two trees represent the same expression
   */
  private boolean sameExpression(ExpressionTree tree1, ExpressionTree tree2) {
    JavaExpression r1 = JavaExpression.fromTree(tree1);
    JavaExpression r2 = JavaExpression.fromTree(tree1);
    if (r1 != null && !r1.containsUnknown() && r2 != null && !r2.containsUnknown()) {
      return r1.equals(r2);
    } else {
      return tree1.toString().equals(tree2.toString());
    }
  }

  @Override
  public Void visitIf(IfTree node, Void p) {
    handleConditionalStatementIsPresentGet(node);
    return super.visitIf(node, p);
  }

  /**
   * Part of rule #3.
   *
   * <p>Pattern match for: {@code if (VAR.isPresent()) { METHOD(VAR.get()); }}
   *
   * <p>Prefer: {@code VAR.ifPresent(METHOD);}
   */
  public void handleConditionalStatementIsPresentGet(IfTree node) {

    ExpressionTree condExpr = TreeUtils.withoutParens(node.getCondition());
    Pair<Boolean, ExpressionTree> isPresentCall = isCallToIsPresent(condExpr);
    if (isPresentCall == null) {
      return;
    }

    StatementTree thenStmt = skipBlocks(node.getThenStatement());
    StatementTree elseStmt = skipBlocks(node.getElseStatement());
    if (!isPresentCall.first) {
      StatementTree tmp = thenStmt;
      thenStmt = elseStmt;
      elseStmt = tmp;
    }

    if (!(elseStmt == null
        || (elseStmt.getKind() == Tree.Kind.BLOCK
            && ((BlockTree) elseStmt).getStatements().isEmpty()))) {
      // else block is missing or is an empty block: "{}"
      return;
    }

    if (thenStmt.getKind() != Kind.EXPRESSION_STATEMENT) {
      return;
    }
    ExpressionTree thenExpr = ((ExpressionStatementTree) thenStmt).getExpression();
    if (thenExpr.getKind() != Kind.METHOD_INVOCATION) {
      return;
    }
    MethodInvocationTree invok = (MethodInvocationTree) thenExpr;
    List<? extends ExpressionTree> args = invok.getArguments();
    if (args.size() != 1) {
      return;
    }
    ExpressionTree arg = TreeUtils.withoutParens(args.get(0));
    if (!isCallToGet(arg)) {
      return;
    }
    ExpressionTree receiver = isPresentCall.second;
    ExpressionTree getReceiver = TreeUtils.getReceiverTree(arg);
    if (!receiver.toString().equals(getReceiver.toString())) {
      return;
    }
    ExpressionTree method = invok.getMethodSelect();

    String methodString = method.toString();
    int dotPos = methodString.lastIndexOf(".");
    if (dotPos != -1) {
      methodString = methodString.substring(0, dotPos) + "::" + methodString.substring(dotPos + 1);
    }

    checker.reportWarning(node, "prefer.ifpresent", receiver, methodString);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    handleCreationElimination(node);
    return super.visitMethodInvocation(node, p);
  }

  /**
   * Rule #4.
   *
   * <p>Pattern match for: {@code CREATION().ELIMINATION()}
   *
   * <p>Prefer: {@code VAR.ifPresent(METHOD);}
   */
  public void handleCreationElimination(MethodInvocationTree node) {
    if (!isOptionalElimation(node)) {
      return;
    }
    ExpressionTree receiver = TreeUtils.getReceiverTree(node);
    if (!(receiver.getKind() == Kind.METHOD_INVOCATION
        && isOptionalCreation((MethodInvocationTree) receiver))) {
      return;
    }

    checker.reportWarning(node, "introduce.eliminate");
  }

  /**
   * Rule #6 (partial).
   *
   * <p>Don't use Optional in fields and method parameters.
   */
  @Override
  public Void visitVariable(VariableTree node, Void p) {
    VariableElement ve = TreeUtils.elementFromDeclaration(node);
    TypeMirror tm = ve.asType();
    if (isOptionalType(tm)) {
      ElementKind ekind = TreeUtils.elementFromDeclaration(node).getKind();
      if (ekind.isField()) {
        checker.reportWarning(node, "optional.field");
      } else if (ekind == ElementKind.PARAMETER) {
        checker.reportWarning(node, "optional.parameter");
      }
    }
    return super.visitVariable(node, p);
  }

  /**
   * Handles part of Rule #6, and also Rule #7: Don't permit {@code Collection<Optional<...>>} or
   * {@code Optional<Collection<...>>}.
   */
  private final class OptionalTypeValidator extends BaseTypeValidator {

    public OptionalTypeValidator(
        BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
      super(checker, visitor, atypeFactory);
    }

    /**
     * Rules 6 (partial) and 7: Don't permit {@code Collection<Optional<...>>} or {@code
     * Optional<Collection<...>>}.
     */
    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
      TypeMirror tm = type.getUnderlyingType();
      if (isCollectionType(tm)) {
        List<? extends TypeMirror> typeArgs = ((DeclaredType) tm).getTypeArguments();
        if (typeArgs.size() == 1) {
          // TODO: handle collections that have more than one type parameter
          TypeMirror typeArg = typeArgs.get(0);
          if (isOptionalType(typeArg)) {
            checker.reportWarning(tree, "optional.as.element.type");
          }
        }
      } else if (isOptionalType(tm)) {
        List<? extends TypeMirror> typeArgs = ((DeclaredType) tm).getTypeArguments();
        // If typeArgs.size()==0, then the user wrote a raw type `Optional`.
        if (typeArgs.size() == 1) {
          TypeMirror typeArg = typeArgs.get(0);
          if (isCollectionType(typeArg)) {
            checker.reportError(tree, "optional.collection");
          }
        }
      }
      return super.visitDeclared(type, tree);
    }
  }

  /** Return true if tm represents a subtype of Collection (other than the Null type). */
  private boolean isCollectionType(TypeMirror tm) {
    return tm.getKind() == TypeKind.DECLARED && types.isSubtype(tm, collectionType);
  }

  /** Return true if tm represents java.util.Optional. */
  private boolean isOptionalType(TypeMirror tm) {
    return TypesUtils.isDeclaredOfName(tm, "java.util.Optional");
  }

  /**
   * If the given tree is a block tree with a single element, return the enclosed non-block
   * statement. Otherwise, return the same tree.
   *
   * @param tree a statement tree
   * @return the single enclosed statement, if it exists; otherwise, the same tree
   */
  // TODO: The Optional Checker should work over the CFG, then it would not need this any longer.
  public static StatementTree skipBlocks(final StatementTree tree) {
    if (tree == null) {
      return tree;
    }
    StatementTree s = tree;
    while (s.getKind() == Tree.Kind.BLOCK) {
      List<? extends StatementTree> stmts = ((BlockTree) s).getStatements();
      if (stmts.size() == 1) {
        s = stmts.get(0);
      } else {
        return s;
      }
    }
    return s;
  }
}
