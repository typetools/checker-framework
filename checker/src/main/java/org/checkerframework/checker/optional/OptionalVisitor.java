package org.checkerframework.checker.optional;

import com.sun.source.tree.BinaryTree;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/**
 * The OptionalVisitor enforces the Optional Checker rules. These rules are described in the Checker
 * Framework Manual.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
public class OptionalVisitor
    extends BaseTypeVisitor</* OptionalAnnotatedTypeFactory*/ BaseAnnotatedTypeFactory> {

  /** The Collection type. */
  private final TypeMirror collectionType;

  /** The element for java.util.Optional.empty(). */
  private final ExecutableElement optionalEmpty;

  /** The element for java.util.Optional.filter(). */
  private final ExecutableElement optionalFilter;

  /** The element for java.util.Optional.flatMap(). */
  private final ExecutableElement optionalFlatMap;

  /** The element for java.util.Optional.get(). */
  private final ExecutableElement optionalGet;

  /** The element for java.util.Optional.hashCode(). */
  private final ExecutableElement optionalHashCode;

  /** The element for java.util.Optional.ifPresent(). */
  private final ExecutableElement optionalIfPresent;

  /** The element for java.util.Optional.ifPresentOrElse(). */
  private final ExecutableElement optionalIfPresentOrElse;

  /** The element for java.util.Optional.isPresent(). */
  private final ExecutableElement optionalIsPresent;

  /** The element for java.util.Optional.isEmpty(), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalIsEmpty;

  /** The element for java.util.Optional.map(). */
  private final ExecutableElement optionalMap;

  /** The element for java.util.Optional.of(). */
  private final ExecutableElement optionalOf;

  /** The element for java.util.Optional.ofNullable(). */
  private final ExecutableElement optionalOfNullable;

  /** The element for java.util.Optional.or(), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalOr;

  /** The element for java.util.Optional.orElse(). */
  private final ExecutableElement optionalOrElse;

  /** The element for java.util.Optional.orElseGet(). */
  private final ExecutableElement optionalOrElseGet;

  /** The element for java.util.Optional.orElseThrow(), or null if running below Java 10. */
  private final @Nullable ExecutableElement optionalOrElseThrow;

  /** The element for java.util.Optional.orElseThrow(Supplier), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalOrElseThrowSupplier;

  /** The element for java.util.Optional.toString(). */
  private final @Nullable ExecutableElement optionalToString;

  /** The element for java.util.OptionalDouble.empty(). */
  private final ExecutableElement optionalDoubleEmpty;

  /** The element for java.util.OptionalDouble.getAsDouble(). */
  private final ExecutableElement optionalDoubleGetAsDouble;

  /** The element for java.util.OptionalDouble.hashCode(). */
  private final ExecutableElement optionalDoubleHashCode;

  /** The element for java.util.OptionalDouble.ifPresent(). */
  private final ExecutableElement optionalDoubleIfPresent;

  /** The element for java.util.OptionalDouble.ifPresentOrElse(). */
  private final ExecutableElement optionalDoubleIfPresentOrElse;

  /** The element for java.util.OptionalDouble.isPresent(). */
  private final ExecutableElement optionalDoubleIsPresent;

  /** The element for java.util.OptionalDouble.isEmpty(), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalDoubleIsEmpty;

  /** The element for java.util.OptionalDouble.of(). */
  private final ExecutableElement optionalDoubleOf;

  /** The element for java.util.OptionalDouble.orElse(). */
  private final ExecutableElement optionalDoubleOrElse;

  /** The element for java.util.OptionalDouble.orElseGet(). */
  private final ExecutableElement optionalDoubleOrElseGet;

  /** The element for java.util.OptionalDouble.orElseThrow(), or null if running below Java 10. */
  private final @Nullable ExecutableElement optionalDoubleOrElseThrow;

  /**
   * The element for java.util.OptionalDouble.orElseThrow(Supplier), or null if running under JDK 8.
   */
  private final @Nullable ExecutableElement optionalDoubleOrElseThrowSupplier;

  /** The element for java.util.OptionalDouble.toString(). */
  private final @Nullable ExecutableElement optionalDoubleToString;

  /** The element for java.util.OptionalInt.empty(). */
  private final ExecutableElement optionalIntEmpty;

  /** The element for java.util.OptionalInt.getAsInt(). */
  private final ExecutableElement optionalIntGetAsInt;

  /** The element for java.util.OptionalInt.hashCode(). */
  private final ExecutableElement optionalIntHashCode;

  /** The element for java.util.OptionalInt.ifPresent(). */
  private final ExecutableElement optionalIntIfPresent;

  /** The element for java.util.OptionalInt.ifPresentOrElse(). */
  private final ExecutableElement optionalIntIfPresentOrElse;

  /** The element for java.util.OptionalInt.isPresent(). */
  private final ExecutableElement optionalIntIsPresent;

  /** The element for java.util.OptionalInt.isEmpty(), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalIntIsEmpty;

  /** The element for java.util.OptionalInt.of(). */
  private final ExecutableElement optionalIntOf;

  /** The element for java.util.OptionalInt.orElse(). */
  private final ExecutableElement optionalIntOrElse;

  /** The element for java.util.OptionalInt.orElseGet(). */
  private final ExecutableElement optionalIntOrElseGet;

  /** The element for java.util.OptionalInt.orElseThrow(), or null if running below Java 10. */
  private final @Nullable ExecutableElement optionalIntOrElseThrow;

  /**
   * The element for java.util.OptionalInt.orElseThrow(Supplier), or null if running under JDK 8.
   */
  private final @Nullable ExecutableElement optionalIntOrElseThrowSupplier;

  /** The element for java.util.OptionalInt.toString(). */
  private final @Nullable ExecutableElement optionalIntToString;

  /** The element for java.util.OptionalLong.empty(). */
  private final ExecutableElement optionalLongEmpty;

  /** The element for java.util.OptionalLong.getAsLong(). */
  private final ExecutableElement optionalLongGetAsLong;

  /** The element for java.util.OptionalLong.hashCode(). */
  private final ExecutableElement optionalLongHashCode;

  /** The element for java.util.OptionalLong.ifPresent(). */
  private final ExecutableElement optionalLongIfPresent;

  /** The element for java.util.OptionalLong.ifPresentOrElse(). */
  private final ExecutableElement optionalLongIfPresentOrElse;

  /** The element for java.util.OptionalLong.isEmpty(), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalLongIsEmpty;

  /** The element for java.util.OptionalLong.isPresent(). */
  private final ExecutableElement optionalLongIsPresent;

  /** The element for java.util.OptionalLong.of(). */
  private final ExecutableElement optionalLongOf;

  /** The element for java.util.OptionalLong.orElse(). */
  private final ExecutableElement optionalLongOrElse;

  /** The element for java.util.OptionalLong.orElseGet(). */
  private final ExecutableElement optionalLongOrElseGet;

  /** The element for java.util.OptionalLong.orElseThrow(), or null if running below Java 10. */
  private final @Nullable ExecutableElement optionalLongOrElseThrow;

  /**
   * The element for java.util.OptionalLong.orElseThrow(Supplier), or null if running under JDK 8.
   */
  private final @Nullable ExecutableElement optionalLongOrElseThrowSupplier;

  /** The element for java.util.OptionalLong.toString(). */
  private final @Nullable ExecutableElement optionalLongToString;

  /** The element for java.lang.Object.getClass(). */
  private final ExecutableElement objectGetClass;

  /** Static methods that create an Optional. */
  private final List<ExecutableElement> optionalCreators;

  /** Methods whose receiver is an Optional, and return an Optional. */
  private final List<ExecutableElement> optionalPropagators;

  /** Methods whose receiver is an Optional, and return a non-optional. */
  private final List<ExecutableElement> optionalEliminators;

  /**
   * Create an OptionalVisitor.
   *
   * @param checker the associated OptionalChecker
   */
  public OptionalVisitor(BaseTypeChecker checker) {
    super(checker);
    collectionType = types.erasure(TypesUtils.typeFromClass(Collection.class, types, elements));

    ProcessingEnvironment env = checker.getProcessingEnvironment();

    optionalEmpty = TreeUtils.getMethod("java.util.Optional", "empty", 0, env);
    optionalFilter = TreeUtils.getMethod("java.util.Optional", "filter", 1, env);
    optionalFlatMap = TreeUtils.getMethod("java.util.Optional", "flatMap", 1, env);
    optionalGet = TreeUtils.getMethod("java.util.Optional", "get", 0, env);
    optionalHashCode = TreeUtils.getMethod("java.util.Optional", "hashCode", 0, env);
    optionalIfPresent = TreeUtils.getMethod("java.util.Optional", "ifPresent", 1, env);
    optionalIfPresentOrElse = TreeUtils.getMethod("java.util.Optional", "ifPresentOrElse", 2, env);
    optionalIsEmpty = TreeUtils.getMethodOrNull("java.util.Optional", "isEmpty", 0, env);
    optionalIsPresent = TreeUtils.getMethod("java.util.Optional", "isPresent", 0, env);
    optionalMap = TreeUtils.getMethod("java.util.Optional", "map", 1, env);
    optionalOf = TreeUtils.getMethod("java.util.Optional", "of", 1, env);
    optionalOr = TreeUtils.getMethodOrNull("java.util.Optional", "or", 1, env);
    optionalOfNullable = TreeUtils.getMethod("java.util.Optional", "ofNullable", 1, env);
    optionalOrElse = TreeUtils.getMethod("java.util.Optional", "orElse", 1, env);
    optionalOrElseGet = TreeUtils.getMethod("java.util.Optional", "orElseGet", 1, env);
    optionalOrElseThrow = TreeUtils.getMethodOrNull("java.util.Optional", "orElseThrow", 0, env);
    optionalOrElseThrowSupplier = TreeUtils.getMethod("java.util.Optional", "orElseThrow", 1, env);
    optionalToString = TreeUtils.getMethod("java.util.Optional", "toString", 0, env);

    optionalDoubleEmpty = TreeUtils.getMethod("java.util.OptionalDouble", "empty", 0, env);
    optionalDoubleGetAsDouble =
        TreeUtils.getMethod("java.util.OptionalDouble", "getAsDouble", 0, env);
    optionalDoubleHashCode = TreeUtils.getMethod("java.util.OptionalDouble", "hashCode", 0, env);
    optionalDoubleIfPresent = TreeUtils.getMethod("java.util.OptionalDouble", "ifPresent", 1, env);
    optionalDoubleIfPresentOrElse =
        TreeUtils.getMethod("java.util.OptionalDouble", "ifPresentOrElse", 2, env);
    optionalDoubleIsEmpty =
        TreeUtils.getMethodOrNull("java.util.OptionalDouble", "isEmpty", 0, env);
    optionalDoubleIsPresent = TreeUtils.getMethod("java.util.OptionalDouble", "isPresent", 0, env);
    optionalDoubleOf = TreeUtils.getMethod("java.util.OptionalDouble", "of", 1, env);
    optionalDoubleOrElse = TreeUtils.getMethod("java.util.OptionalDouble", "orElse", 1, env);
    optionalDoubleOrElseGet = TreeUtils.getMethod("java.util.OptionalDouble", "orElseGet", 1, env);
    optionalDoubleOrElseThrow =
        TreeUtils.getMethodOrNull("java.util.OptionalDouble", "orElseThrow", 0, env);
    optionalDoubleOrElseThrowSupplier =
        TreeUtils.getMethod("java.util.OptionalDouble", "orElseThrow", 1, env);
    optionalDoubleToString = TreeUtils.getMethod("java.util.OptionalDouble", "toString", 0, env);

    optionalIntEmpty = TreeUtils.getMethod("java.util.OptionalInt", "empty", 0, env);
    optionalIntGetAsInt = TreeUtils.getMethod("java.util.OptionalInt", "getAsInt", 0, env);
    optionalIntHashCode = TreeUtils.getMethod("java.util.OptionalInt", "hashCode", 0, env);
    optionalIntIfPresent = TreeUtils.getMethod("java.util.OptionalInt", "ifPresent", 1, env);
    optionalIntIfPresentOrElse =
        TreeUtils.getMethod("java.util.OptionalInt", "ifPresentOrElse", 2, env);
    optionalIntIsEmpty = TreeUtils.getMethodOrNull("java.util.OptionalInt", "isEmpty", 0, env);
    optionalIntIsPresent = TreeUtils.getMethod("java.util.OptionalInt", "isPresent", 0, env);
    optionalIntOf = TreeUtils.getMethod("java.util.OptionalInt", "of", 1, env);
    optionalIntOrElse = TreeUtils.getMethod("java.util.OptionalInt", "orElse", 1, env);
    optionalIntOrElseGet = TreeUtils.getMethod("java.util.OptionalInt", "orElseGet", 1, env);
    optionalIntOrElseThrow =
        TreeUtils.getMethodOrNull("java.util.OptionalInt", "orElseThrow", 0, env);
    optionalIntOrElseThrowSupplier =
        TreeUtils.getMethod("java.util.OptionalInt", "orElseThrow", 1, env);
    optionalIntToString = TreeUtils.getMethod("java.util.OptionalInt", "toString", 0, env);

    optionalLongEmpty = TreeUtils.getMethod("java.util.OptionalLong", "empty", 0, env);
    optionalLongGetAsLong = TreeUtils.getMethod("java.util.OptionalLong", "getAsLong", 0, env);
    optionalLongHashCode = TreeUtils.getMethod("java.util.OptionalLong", "hashCode", 0, env);
    optionalLongIfPresent = TreeUtils.getMethod("java.util.OptionalLong", "ifPresent", 1, env);
    optionalLongIfPresentOrElse =
        TreeUtils.getMethod("java.util.OptionalLong", "ifPresentOrElse", 2, env);
    optionalLongIsEmpty = TreeUtils.getMethodOrNull("java.util.OptionalLong", "isEmpty", 0, env);
    optionalLongIsPresent = TreeUtils.getMethod("java.util.OptionalLong", "isPresent", 0, env);
    optionalLongOf = TreeUtils.getMethod("java.util.OptionalLong", "of", 1, env);
    optionalLongOrElse = TreeUtils.getMethod("java.util.OptionalLong", "orElse", 1, env);
    optionalLongOrElseGet = TreeUtils.getMethod("java.util.OptionalLong", "orElseGet", 1, env);
    optionalLongOrElseThrow =
        TreeUtils.getMethodOrNull("java.util.OptionalLong", "orElseThrow", 0, env);
    optionalLongOrElseThrowSupplier =
        TreeUtils.getMethod("java.util.OptionalLong", "orElseThrow", 1, env);
    optionalLongToString = TreeUtils.getMethod("java.util.OptionalLong", "toString", 0, env);

    objectGetClass = TreeUtils.getMethod("java.lang.Object", "getClass", 0, env);

    optionalCreators =
        Arrays.asList(
            optionalEmpty,
            optionalOf,
            optionalOfNullable,
            optionalDoubleEmpty,
            optionalDoubleOf,
            optionalIntEmpty,
            optionalIntOf,
            optionalLongEmpty,
            optionalLongOf);

    // There are no propagators in OptionalDouble, OptionalInt, or OptionalLong.
    optionalPropagators =
        optionalOr == null
            ? Arrays.asList(optionalFilter, optionalFlatMap, optionalMap)
            : Arrays.asList(optionalFilter, optionalFlatMap, optionalMap, optionalOr);

    // Iterating through this is inefficient.  I should find a better way.
    // One idea: check the class that declares the method (using isOptionalType) and then the name
    // as a string.  Is there a better way?
    optionalEliminators =
        new ArrayList<>(
            Arrays.asList(
                optionalGet,
                optionalHashCode,
                optionalIfPresent,
                optionalIfPresentOrElse,
                optionalIsEmpty,
                optionalIsPresent,
                optionalOrElse,
                optionalOrElseGet,
                optionalOrElseThrow,
                optionalOrElseThrowSupplier,
                optionalToString,
                optionalDoubleGetAsDouble,
                optionalDoubleHashCode,
                optionalDoubleIfPresent,
                optionalDoubleIfPresentOrElse,
                optionalDoubleIsEmpty,
                optionalDoubleIsPresent,
                optionalDoubleOrElse,
                optionalDoubleOrElseGet,
                optionalDoubleOrElseThrow,
                optionalDoubleOrElseThrowSupplier,
                optionalDoubleToString,
                optionalIntGetAsInt,
                optionalIntHashCode,
                optionalIntIfPresent,
                optionalIntIfPresentOrElse,
                optionalIntIsEmpty,
                optionalIntIsPresent,
                optionalIntOrElse,
                optionalIntOrElseGet,
                optionalIntOrElseThrow,
                optionalIntOrElseThrowSupplier,
                optionalIntToString,
                optionalLongGetAsLong,
                optionalLongHashCode,
                optionalLongIfPresent,
                optionalLongIfPresentOrElse,
                optionalLongIsEmpty,
                optionalLongIsPresent,
                optionalLongOrElse,
                optionalLongOrElseGet,
                optionalLongOrElseThrow,
                optionalLongOrElseThrowSupplier,
                optionalLongToString,
                objectGetClass));
    optionalEliminators.removeAll(Collections.singleton(null));
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
  private @Nullable IPair<Boolean, @Nullable ExpressionTree> isCallToIsPresent(
      ExpressionTree expression) {
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
            return IPair.of(!negate, TreeUtils.getReceiverTree(expression));
          } else if (optionalIsEmpty != null
              && TreeUtils.isMethodInvocation(expression, optionalIsEmpty, env)) {
            return IPair.of(negate, TreeUtils.getReceiverTree(expression));
          } else {
            return null;
          }
        default:
          return null;
      }
    }
  }

  /**
   * Returns true iff the method being called is Optional creation: empty, of, ofNullable.
   *
   * @param methInvok a method invocation
   * @return true iff the method being called is Optional creation: empty, of, ofNullable
   */
  private boolean isOptionalCreation(MethodInvocationTree methInvok) {
    return TreeUtils.isMethodInvocation(
        methInvok, optionalCreators, checker.getProcessingEnvironment());
  }

  /**
   * Returns true iff the method being called is Optional propagation: filter, flatMap, map, or.
   *
   * @param methInvok a method invocation
   * @return true true iff the method being called is Optional propagation: filter, flatMap, map, or
   */
  private boolean isOptionalPropagation(MethodInvocationTree methInvok) {
    return TreeUtils.isMethodInvocation(
        methInvok, optionalPropagators, checker.getProcessingEnvironment());
  }

  /**
   * Returns true iff the method being called is Optional elimination: get, orElse, orElseGet,
   * orElseThrow.
   *
   * @param methInvok a method invocation
   * @return true iff the method being called is Optional elimination: get, orElse, orElseGet,
   *     orElseThrow
   */
  private boolean isOptionalElimination(MethodInvocationTree methInvok) {
    return TreeUtils.isMethodInvocation(
        methInvok, optionalEliminators, checker.getProcessingEnvironment());
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
    handleTernaryIsPresentGet(tree);
    return super.visitConditionalExpression(tree, p);
  }

  /**
   * Part of rule #3.
   *
   * <p>Pattern match for: {@code VAR.isPresent() ? VAR.get().METHOD() : VALUE}
   *
   * <p>Prefer: {@code VAR.map(METHOD).orElse(VALUE);}
   *
   * @param tree a conditional expression that can perhaps be simplified
   */
  // TODO: Should handle this via a transfer function, instead of pattern-matching.
  public void handleTernaryIsPresentGet(ConditionalExpressionTree tree) {

    ExpressionTree condExpr = TreeUtils.withoutParens(tree.getCondition());
    IPair<Boolean, ExpressionTree> isPresentCall = isCallToIsPresent(condExpr);
    if (isPresentCall == null) {
      return;
    }
    ExpressionTree trueExpr = TreeUtils.withoutParens(tree.getTrueExpression());
    ExpressionTree falseExpr = TreeUtils.withoutParens(tree.getFalseExpression());
    if (!isPresentCall.first) {
      ExpressionTree tmp = trueExpr;
      trueExpr = falseExpr;
      falseExpr = tmp;
    }

    if (trueExpr.getKind() != Tree.Kind.METHOD_INVOCATION) {
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
          tree,
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
    JavaExpression r2 = JavaExpression.fromTree(tree2);
    if (r1 != null && !r1.containsUnknown() && r2 != null && !r2.containsUnknown()) {
      return r1.equals(r2);
    } else {
      return tree1.toString().equals(tree2.toString());
    }
  }

  @Override
  public Void visitIf(IfTree tree, Void p) {
    handleConditionalStatementIsPresentGet(tree);
    return super.visitIf(tree, p);
  }

  /**
   * Part of rule #3.
   *
   * <p>Pattern match for: {@code if (VAR.isPresent()) { METHOD(VAR.get()); }}
   *
   * <p>Prefer: {@code VAR.ifPresent(METHOD);}
   *
   * @param tree an if statement that can perhaps be simplified
   */
  public void handleConditionalStatementIsPresentGet(IfTree tree) {

    ExpressionTree condExpr = TreeUtils.withoutParens(tree.getCondition());
    IPair<Boolean, ExpressionTree> isPresentCall = isCallToIsPresent(condExpr);
    if (isPresentCall == null) {
      return;
    }

    StatementTree thenStmt = skipBlocks(tree.getThenStatement());
    StatementTree elseStmt = skipBlocks(tree.getElseStatement());
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

    if (thenStmt.getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
      return;
    }
    ExpressionTree thenExpr = ((ExpressionStatementTree) thenStmt).getExpression();
    if (thenExpr.getKind() != Tree.Kind.METHOD_INVOCATION) {
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

    checker.reportWarning(tree, "prefer.ifpresent", receiver, methodString);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    handleCreationElimination(tree);
    handleNestedOptionalCreation(tree);
    return super.visitMethodInvocation(tree, p);
  }

  @Override
  public Void visitBinary(BinaryTree tree, Void p) {
    handleCompareToNull(tree);
    return super.visitBinary(tree, p);
  }

  /**
   * Partially enforces Rule #1.
   *
   * <p>If an Optional value is compared with the null literal, it indicates that the programmer
   * expects it might have been assigned a null value (or no value at all) somewhere in the code.
   *
   * @param tree a binary tree representing a binary operation.
   */
  private void handleCompareToNull(BinaryTree tree) {
    if (!isEqualityOperation(tree)) {
      return;
    }
    ExpressionTree leftOp = TreeUtils.withoutParens(tree.getLeftOperand());
    ExpressionTree rightOp = TreeUtils.withoutParens(tree.getRightOperand());
    TypeMirror leftOpType = TreeUtils.typeOf(leftOp);
    TypeMirror rightOpType = TreeUtils.typeOf(rightOp);

    if (leftOp.getKind() == Tree.Kind.NULL_LITERAL && isOptionalType(rightOpType)) {
      checker.reportWarning(tree, "optional.null.comparison");
    }
    if (rightOp.getKind() == Tree.Kind.NULL_LITERAL && isOptionalType(leftOpType)) {
      checker.reportWarning(tree, "optional.null.comparison");
    }
  }

  /**
   * Returns true if the binary operation is {@code ==} or {@code !=}.
   *
   * @param tree a binary operation
   * @return true if the binary operation is {@code ==} or {@code !=}
   */
  private boolean isEqualityOperation(BinaryTree tree) {
    return tree.getKind() == Tree.Kind.EQUAL_TO || tree.getKind() == Tree.Kind.NOT_EQUAL_TO;
  }

  // Partially enforces Rule #1.  (Only handles the literal `null`, not all nullable expressions.)
  @Override
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      ExpressionTree valueExpTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    boolean result = super.commonAssignmentCheck(varType, valueExpTree, errorKey, extraArgs);
    ExpressionTree valueWithoutParens = TreeUtils.withoutParens(valueExpTree);
    if (valueWithoutParens.getKind() == Kind.NULL_LITERAL
        && isOptionalType(varType.getUnderlyingType())) {
      checker.reportWarning(valueWithoutParens, "optional.null.assignment");
      return false;
    }
    return result;
  }

  /**
   * Rule #4.
   *
   * <p>Pattern match for: {@code CREATION().PROPAGATION()*.ELIMINATION()}
   *
   * <p>Prefer: {@code VAR.ifPresent(METHOD);}
   *
   * @param tree a method invocation that can perhaps be simplified
   */
  public void handleCreationElimination(MethodInvocationTree tree) {
    if (!isOptionalElimination(tree)) {
      return;
    }
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    while (true) {
      if (receiver == null) {
        // The receiver can be null if the receiver is the implicit "this.".
        return;
      }
      if (receiver.getKind() != Tree.Kind.METHOD_INVOCATION) {
        return;
      }
      MethodInvocationTree methodCall = (MethodInvocationTree) receiver;
      if (isOptionalPropagation(methodCall)) {
        receiver = TreeUtils.getReceiverTree(methodCall);
        continue;
      } else if (isOptionalCreation(methodCall)) {
        checker.reportWarning(tree, "introduce.eliminate");
        return;
      } else {
        return;
      }
    }
  }

  /**
   * Partial support for Rule #5 and Rule #7.
   *
   * <p>Rule #5: Avoid nested Optional chains, or operations that have an intermediate Optional
   * value.
   *
   * <p>Rule #7: Don't use Optional to wrap any collection type.
   *
   * <p>Certain types are illegal, such as {@code Optional<Optional>}. The type validator may see a
   * supertype of the most precise run-time type; for example, it may see the type as {@code
   * Optional<? extends Object>}, and it would not flag any problem with such a type. This method
   * checks at {@code Optional} creation sites.
   *
   * <p>TODO: This finds only some {@code Optional<Optional>}: those that consist of {@code
   * Optional.of(optionalExpr)} or {@code Optional.ofNullable(optionalExpr)}, where {@code
   * optionalExpr} has type {@code Optional}. There are other ways that {@code Optional<Optional>}
   * can be created, such as {@code optionalExpr.map(Optional::of)}.
   *
   * <p>TODO: Also check at collection creation sites, but there are so many of them, and there
   * often are not values of the element type at the collection creation site.
   *
   * @param tree a method invocation that might create an Optional of an illegal type
   */
  public void handleNestedOptionalCreation(MethodInvocationTree tree) {
    if (!isOptionalCreation(tree)) {
      return;
    }
    if (tree.getArguments().isEmpty()) {
      // This is a call to Optional.empty(), which takes no argument.
      return;
    }
    ExpressionTree arg = tree.getArguments().get(0);
    AnnotatedTypeMirror argAtm = atypeFactory.getAnnotatedType(arg);
    TypeMirror argType = argAtm.getUnderlyingType();
    if (isOptionalType(argType)) {
      checker.reportWarning(tree, "optional.nesting");
    } else if (isCollectionType(argType)) {
      checker.reportWarning(tree, "optional.collection");
    }
  }

  /**
   * Rule #6 (partial).
   *
   * <p>Don't use Optional in fields and method parameters.
   */
  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    VariableElement ve = TreeUtils.elementFromDeclaration(tree);
    TypeMirror tm = ve.asType();
    if (isOptionalType(tm)) {
      ElementKind ekind = TreeUtils.elementFromDeclaration(tree).getKind();
      if (ekind.isField()) {
        checker.reportWarning(tree, "optional.field");
      } else if (ekind == ElementKind.PARAMETER) {
        checker.reportWarning(tree, "optional.parameter");
      }
    }
    return super.visitVariable(tree, p);
  }

  /**
   * Handles Rule #5, part of Rule #6, and also Rule #7.
   *
   * <p>Rule #5: Avoid nested Optional chains, or operations that have an intermediate Optional
   * value.
   *
   * <p>Rule #6: Don't use Optional in fields, parameters, and collections.
   *
   * <p>Rule #7: Don't use Optional to wrap any collection type.
   *
   * <p>The validator is called on the type of every expression, such as on the right-hand side of
   * {@code x = Optional.of(Optional.of("baz"));}. However, the type of the right-hand side is
   * {@code Optional<? extends Object>}, not {@code Optional<Optional<String>>}. Therefore, to fully
   * check for improper types, it is necessary to examine, in the type checker, the argument to
   * construction of an Optional. Method {@link handleNestedOptionalCreation} does so.
   */
  private final class OptionalTypeValidator extends BaseTypeValidator {

    public OptionalTypeValidator(
        BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
      super(checker, visitor, atypeFactory);
    }

    /**
     * Handles Rule #5, part of Rule #6, and also Rule #7.
     *
     * <p>Rule #5: Avoid nested Optional chains, or operations that have an intermediate Optional
     * value.
     *
     * <p>Rule #6: Don't use Optional in fields, parameters, and collections.
     *
     * <p>Rule #7: Don't use Optional to wrap any collection type.
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
            checker.reportWarning(tree, "optional.collection");
          }
          if (isOptionalType(typeArg)) {
            checker.reportWarning(tree, "optional.nesting");
          }
        }
      }
      return super.visitDeclared(type, tree);
    }
  }

  /**
   * Return true if tm is a subtype of Collection (other than the Null type).
   *
   * @param tm a type
   * @return true if the given type is a subtype of Collection
   */
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
  public static StatementTree skipBlocks(StatementTree tree) {
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
