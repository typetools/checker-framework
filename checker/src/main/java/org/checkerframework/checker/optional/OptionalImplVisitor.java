package org.checkerframework.checker.optional;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nonempty.qual.RequiresNonEmpty;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.optional.qual.OptionalCreator;
import org.checkerframework.checker.optional.qual.OptionalEliminator;
import org.checkerframework.checker.optional.qual.OptionalPropagator;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/**
 * The OptionalImplVisitor enforces the Optional Checker rules. These rules are described in the
 * Checker Framework Manual.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
public class OptionalImplVisitor
    extends BaseTypeVisitor</* OptionalAnnotatedTypeFactory*/ BaseAnnotatedTypeFactory> {

  /** The Collection type. */
  private final TypeMirror collectionType;

  /** The element for java.util.Optional.get(). */
  private final ExecutableElement optionalGet;

  /** The element for java.util.Optional.isPresent(). */
  private final ExecutableElement optionalIsPresent;

  /** The element for java.util.Optional.isEmpty(), or null if running under JDK 8. */
  private final @Nullable ExecutableElement optionalIsEmpty;

  /** The element for java.util.stream.Stream.filter(). */
  private final ExecutableElement streamFilter;

  /** The element for java.util.stream.Stream.map(). */
  private final ExecutableElement streamMap;

  /**
   * The set of names of methods to be verified by the Non-Empty Checker.
   *
   * <p>This set is updated whenever a method that depends on the {@link NonEmpty} type system is
   * visited. A method depends on the {@link NonEmpty} type system if any of the following is true:
   *
   * <ul>
   *   <li>Has any formal parameters annotated with {@link NonEmpty}
   *   <li>Has pre- or post-conditions from the {@link NonEmpty} type system (e.g., {@link
   *       RequiresNonEmpty}, {@link org.checkerframework.checker.nonempty.qual.EnsuresNonEmpty},
   *       {@link org.checkerframework.checker.nonempty.qual.EnsuresNonEmptyIf}
   *   <li>Its body contains a variable or value that is annotated with {@link NonEmpty}
   * </ul>
   *
   * <p>This set is used to help compute {@link calleesToCallers} whenever a method invocation is
   * visited. The method being invoked is checked for membership in this set. If it is found, then
   * the caller of the method (i.e., the method that encloses the method invocation) is added to the
   * list of callers of the method being invoked (i.e., the callee) in {@link calleesToCallers}.
   */
  private final Set<String> namesOfMethodsToVerifyWithNonEmptyChecker = new HashSet<>();

  /**
   * Map from simple names of callees to the simple names of methods that call them. Use of simple
   * names (rather than fully-qualified names or signatures) is a bit imprecise, because it includes
   * all overloads.
   *
   * <p>This is not a complete mapping of <i>all</i> callees and callers in a program. It comprises
   * methods that have programmer-written annotations from the {@link NonEmpty} type system, and
   * their immediate dependents.
   *
   * <p>This mapping is used to help compute {@link namesOfMethodsToVerifyWithNonEmptyChecker}.
   * Whenever a method declaration is visited, its name is checked in the keys (i.e., the set of
   * callees). If it is found, then the corresponding values (i.e., the names of the callers of the
   * method being visited) are added to {@link namesOfMethodsToVerifyWithNonEmptyChecker}.
   */
  private final Map<String, Set<String>> calleesToCallers = new HashMap<>();

  /**
   * Create an OptionalImplVisitor.
   *
   * @param checker the associated instance of {@link
   *     org.checkerframework.checker.optional.OptionalImplChecker}
   */
  public OptionalImplVisitor(BaseTypeChecker checker) {
    super(checker);
    collectionType = types.erasure(TypesUtils.typeFromClass(Collection.class, types, elements));

    ProcessingEnvironment env = checker.getProcessingEnvironment();
    optionalGet = TreeUtils.getMethod("java.util.Optional", "get", 0, env);
    optionalIsPresent = TreeUtils.getMethod("java.util.Optional", "isPresent", 0, env);
    optionalIsEmpty = TreeUtils.getMethodOrNull("java.util.Optional", "isEmpty", 0, env);

    streamFilter = TreeUtils.getMethod("java.util.stream.Stream", "filter", 1, env);
    streamMap = TreeUtils.getMethod("java.util.stream.Stream", "map", 1, env);
  }

  @Override
  protected BaseTypeValidator createTypeValidator() {
    return new OptionalImplTypeValidator(checker, this, atypeFactory);
  }

  /**
   * Returns the set of methods that should be verified using the {@link
   * org.checkerframework.checker.nonempty.NonEmptyChecker}.
   *
   * <p>This should only be called by the Non-Empty Checker.
   *
   * @return the set of methods that should be verified using the {@link
   *     org.checkerframework.checker.nonempty.NonEmptyChecker}
   */
  @Pure
  public Set<String> getNamesOfMethodsToVerifyWithNonEmptyChecker() {
    return namesOfMethodsToVerifyWithNonEmptyChecker;
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
   * Returns true iff the method being called is annotated with {@code @}{@link OptionalCreator}.
   * This is the Optional creation methods: empty, of, ofNullable.
   *
   * @param methInvok a method invocation
   * @return true iff the method being called is Optional creation: empty, of, ofNullable
   */
  private boolean isOptionalCreation(MethodInvocationTree methInvok) {
    ExecutableElement method = TreeUtils.elementFromUse(methInvok);
    return atypeFactory.getDeclAnnotation(method, OptionalCreator.class) != null;
  }

  /**
   * Returns true iff the method being called is annotated with {@code @}{@link OptionalPropagator}.
   * This is the Optional propagation methods: filter, flatMap, map, or.
   *
   * @param methInvok a method invocation
   * @return true iff the method being called is Optional propagation: filter, flatMap, map, or
   */
  private boolean isOptionalPropagation(MethodInvocationTree methInvok) {
    ExecutableElement method = TreeUtils.elementFromUse(methInvok);
    return atypeFactory.getDeclAnnotation(method, OptionalPropagator.class) != null;
  }

  /**
   * Returns true iff the method being called is annotated with {@code @}{@link OptionalEliminator}.
   * This is the Optional elimination methods: get, orElse, orElseGet, orElseThrow.
   *
   * @param methInvok a method invocation
   * @return true iff the method being called is Optional elimination: get, orElse, orElseGet,
   *     orElseThrow
   */
  private boolean isOptionalElimination(MethodInvocationTree methInvok) {
    ExecutableElement method = TreeUtils.elementFromUse(methInvok);
    return atypeFactory.getDeclAnnotation(method, OptionalEliminator.class) != null;
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

    ExpressionTree receiver = isPresentCall.second;
    ExecutableElement ele = TreeUtils.elementFromUse((MethodInvocationTree) trueExpr);
    boolean isPure =
        PurityUtils.isDeterministic(atypeFactory, ele)
            && PurityUtils.isSideEffectFree(atypeFactory, ele);

    if (sameExpression(receiver, getReceiver) && isPure) {

      checker.reportWarning(
          tree,
          "prefer.map.and.orelse",
          receiver,
          // The literal "ENCLOSINGCLASS::" is gross.
          // TODO: add this to the error message.
          // ElementUtils.getQualifiedClassName(ele);
          ele.getSimpleName(),
          falseExpr);
    }
  }

  /**
   * Returns true if the two trees represent the same expression.
   *
   * <p>This method would ideally be in {@link TreeUtils} as a public static method, but this would
   * introduce the {@code dataflow} package as a dependency in {@code javacutil}, which is
   * undesirable.
   *
   * <p>See https://github.com/typetools/checker-framework/pull/6901#discussion_r1889461449 for
   * additional details and discussion.
   *
   * @param tree1 the first tree
   * @param tree2 the second tree
   * @return true if the two trees represent the same expression
   */
  private boolean sameExpression(ExpressionTree tree1, ExpressionTree tree2) {
    JavaExpression r1 = JavaExpression.fromTree(tree1);
    JavaExpression r2 = JavaExpression.fromTree(tree2);
    // What is a better way to do this than string comparison?
    // Use transfer functions and Store entries.
    if (!r1.containsUnknown() && !r2.containsUnknown()) {
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
   * <p>Also matches:
   *
   * <pre>
   *     if (VAR.isPresent()) {
   *        x = METHOD(VAR.get());
   *     } else {
   *        x = OTHER;
   *     }
   * </pre>
   *
   * Where {@code x} is some variable (e.g., a field, a local variable).
   *
   * <p>Prefer: {@code x = VAR.map(METHOD).orElse(OTHER);}
   *
   * @param tree an if statement that can perhaps be simplified
   */
  public void handleConditionalStatementIsPresentGet(IfTree tree) {

    ExpressionTree condExpr = TreeUtils.withoutParens(tree.getCondition());
    IPair<Boolean, ExpressionTree> isPresentCall = isCallToIsPresent(condExpr);
    if (isPresentCall == null) {
      return;
    }

    // `thenStmt` may be null because it may be swapped with `elseStmt`, just below.
    StatementTree thenStmt = skipBlocks(tree.getThenStatement());
    StatementTree elseStmt = skipBlocks(tree.getElseStatement());
    if (!isPresentCall.first) {
      StatementTree tmp = thenStmt;
      thenStmt = elseStmt;
      elseStmt = tmp;
    }

    ExpressionTree isPresentReceiver = TreeUtils.getReceiverTree(condExpr);
    if (isPresentReceiver instanceof MethodInvocationTree) {
      ExecutableElement ele = TreeUtils.elementFromUse((MethodInvocationTree) isPresentReceiver);
      boolean isPure =
          PurityUtils.isDeterministic(atypeFactory, ele)
              && PurityUtils.isSideEffectFree(atypeFactory, ele);
      if (!isPure) {
        return;
      }
    }

    if (thenStmt != null && elseStmt != null) {
      handleAssignmentInConditional(tree, thenStmt, elseStmt);
    }

    if (!(elseStmt == null
        || (elseStmt.getKind() == Tree.Kind.BLOCK
            && ((BlockTree) elseStmt).getStatements().isEmpty()))) {
      // else block is missing or is an empty block: "{}"
      return;
    }

    if (thenStmt != null && thenStmt.getKind() == Tree.Kind.VARIABLE) {
      ExpressionTree initializer = ((VariableTree) thenStmt).getInitializer();
      if (initializer.getKind() == Tree.Kind.METHOD_INVOCATION) {
        checkConditionalStatementIsPresentGetCall(
            tree, (MethodInvocationTree) initializer, isPresentCall, "prefer.map.and.orelse");
        return;
      }
    }

    if (thenStmt == null || thenStmt.getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
      return;
    }
    ExpressionTree thenExpr = ((ExpressionStatementTree) thenStmt).getExpression();
    if (thenExpr.getKind() != Tree.Kind.METHOD_INVOCATION) {
      return;
    }
    checkConditionalStatementIsPresentGetCall(
        tree, (MethodInvocationTree) thenExpr, isPresentCall, "prefer.ifpresent");
  }

  /**
   * Part of rule #3.
   *
   * <p>Pattern match for:
   *
   * <pre>
   *   if (opt.isPresent()) {
   *    x = opt.get().METHOD();
   *   } else {
   *    x = VALUE;
   *   }
   * </pre>
   *
   * Where {@code x} is some variable (e.g., a field, a local variable).
   *
   * <p>Prefer: {@code x = opt.map(METHOD).orElse(VALUE);}
   *
   * @param tree a conditional expression that can perhaps be simplified
   * @param thenStmt the "then" part of {@code tree}
   * @param elseStmt the "else" part of {@code tree}
   */
  private void handleAssignmentInConditional(
      IfTree tree, StatementTree thenStmt, StatementTree elseStmt) {
    AssignmentTree trueAssignment = TreeUtils.asAssignmentTree(thenStmt);
    AssignmentTree falseAssignment = TreeUtils.asAssignmentTree(elseStmt);

    if (trueAssignment == null || falseAssignment == null) {
      return;
    }

    if (sameExpression(trueAssignment.getVariable(), falseAssignment.getVariable())) {
      if (trueAssignment.getExpression().getKind() == Kind.METHOD_INVOCATION) {
        ExecutableElement ele =
            TreeUtils.elementFromUse((MethodInvocationTree) trueAssignment.getExpression());
        checker.reportWarning(
            tree,
            "prefer.map.and.orelse",
            trueAssignment.getVariable(),
            // The literal "ENCLOSINGCLASS::" is gross.
            // TODO: add this to the error message.
            // ElementUtils.getQualifiedClassName(ele);
            ele.getSimpleName(),
            falseAssignment.getExpression());
      }
    }
  }

  /**
   * Helps implement part of rule #3.
   *
   * <p>Pattern match for the following code:
   *
   * <ul>
   *   <li>{@code METHOD(VAR.get());}
   *   <li>{@code OTHER_VAR = METHOD(VAR.get());}
   * </ul>
   *
   * inside the body of the {@code then} block for {@code VAR.isPresent()} or the {@code else} block
   * for {@code VAR.isEmpty()}.
   *
   * @param tree the conditional statement tree
   * @param invok the entire method invocation statement or the initializer of an assignment
   * @param isPresentCall the pair comprising a boolean (indicating whether the expression is a call
   *     to {@code Optional.isPresent} or to {@code Optional.isEmpty}) and its receiver
   * @param messageKey the message key, either "prefer.ifPresent" or "prefer.map.and.orelse"
   */
  private void checkConditionalStatementIsPresentGetCall(
      IfTree tree,
      MethodInvocationTree invok,
      IPair<Boolean, ExpressionTree> isPresentCall,
      @CompilerMessageKey String messageKey) {
    List<? extends ExpressionTree> invokArgs = invok.getArguments();
    if (invokArgs.size() != 1) {
      return;
    }
    ExpressionTree invokArg = TreeUtils.withoutParens(invokArgs.get(0));
    if (!isCallToGet(invokArg)) {
      return;
    }
    ExpressionTree isPresentReceiver = isPresentCall.second;
    ExpressionTree getReceiver = TreeUtils.getReceiverTree(invokArg);
    if (!isPresentReceiver.toString().equals(getReceiver.toString())) {
      return;
    }
    ExpressionTree method = invok.getMethodSelect();

    String methodString = method.toString();
    int dotPos = methodString.lastIndexOf(".");
    if (dotPos != -1) {
      methodString = methodString.substring(0, dotPos) + "::" + methodString.substring(dotPos + 1);
    }

    checker.reportWarning(tree, messageKey, isPresentReceiver, methodString);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    handleCreationElimination(tree);
    handleNestedOptionalCreation(tree);
    updateCalleesToCallers(tree);
    return super.visitMethodInvocation(tree, p);
  }

  /**
   * Updates {@link calleesToCallers} given a method invocation, if the caller of the method is
   * known to rely on the {@link NonEmpty} type system.
   *
   * <p>The caller is the method that <i>encloses</i> the given method invocation. The map is
   * updated if the callee of the invocation is present in the map. That is, the map is updated with
   * the callers of any methods that have programmer-written annotations from the {@link NonEmpty}
   * system.
   *
   * <p>These annotations may appear on the return type, formal parameters, or on the declaration of
   * local variables within method bodies.
   *
   * @param tree a method invocation tree
   */
  private void updateCalleesToCallers(MethodInvocationTree tree) {
    MethodTree caller = TreePathUtil.enclosingMethod(this.getCurrentPath());
    if (caller != null) {
      // Using the names of methods (as opposed to their fully-qualified name or signature) is a
      // safe (but imprecise) over-approximation of all the methods that must be verified with the
      // Non-Empty Checker. Overloads of methods will be included.
      String callee = tree.getMethodSelect().toString();
      boolean isCalleeInMethodsToVerifyWithNonEmptyChecker =
          namesOfMethodsToVerifyWithNonEmptyChecker.stream()
              .anyMatch(nameOfMethodToVerify -> nameOfMethodToVerify.equals(callee));
      if (isCalleeInMethodsToVerifyWithNonEmptyChecker) {
        Set<String> callers = calleesToCallers.computeIfAbsent(callee, (__) -> new HashSet<>());
        callers.add(caller.getName().toString());
      }
    }
  }

  @Override
  public void processMethodTree(String className, MethodTree methodDecl) {
    if (isAnnotatedWithNonEmptyPrecondition(methodDecl)
        || isAnyFormalAnnotatedWithNonEmpty(methodDecl)) {
      addMethodToVerifyWithNonEmptyChecker(methodDecl);
    }
    if (isReturnTypeAnnotatedWithNonEmpty(methodDecl)) {
      namesOfMethodsToVerifyWithNonEmptyChecker.add(methodDecl.getName().toString());
    }
    super.processMethodTree(className, methodDecl);
  }

  /**
   * Updates {@link namesOfMethodsToVerifyWithNonEmptyChecker}.
   *
   * @param methodDecl a method declaration that definitely has a precondition regarding
   *     {@code @NonEmpty}
   */
  private void addMethodToVerifyWithNonEmptyChecker(MethodTree methodDecl) {
    String methodName = methodDecl.getName().toString();
    if (calleesToCallers.containsKey(methodName)) {
      namesOfMethodsToVerifyWithNonEmptyChecker.addAll(calleesToCallers.get(methodName));
    }
    namesOfMethodsToVerifyWithNonEmptyChecker.add(methodDecl.getName().toString());
  }

  /**
   * Returns true if the method is explicitly annotated with {@link RequiresNonEmpty}.
   *
   * @param methodDecl the method declaration
   * @return true if the method is explicitly annotated with {@link RequiresNonEmpty}
   */
  private boolean isAnnotatedWithNonEmptyPrecondition(MethodTree methodDecl) {
    List<? extends AnnotationMirror> annos =
        TreeUtils.annotationsFromTypeAnnotationTrees(methodDecl.getModifiers().getAnnotations());
    return atypeFactory.containsSameByClass(annos, RequiresNonEmpty.class);
  }

  /**
   * Returns true if any formal parameter of the method is explicitly annotated with {@link
   * NonEmpty}.
   *
   * @param methodDecl a method declaration
   * @return true if any formal parameter of the method is explicitly annotated with {@link
   *     NonEmpty}
   */
  private boolean isAnyFormalAnnotatedWithNonEmpty(MethodTree methodDecl) {
    List<? extends VariableTree> params = methodDecl.getParameters();
    for (VariableTree vt : params) {
      List<? extends AnnotationMirror> annos =
          TreeUtils.annotationsFromTypeAnnotationTrees(vt.getModifiers().getAnnotations());
      if (atypeFactory.containsSameByClass(annos, NonEmpty.class)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the return type of the method is explicitly annotated with {@link NonEmpty}.
   *
   * @param methodDecl a method declaration
   * @return true if the return type of the method is explicitly annotated with {@link NonEmpty}
   */
  private boolean isReturnTypeAnnotatedWithNonEmpty(MethodTree methodDecl) {
    Tree returnType = methodDecl.getReturnType();
    if (returnType == null) {
      return false;
    }
    List<? extends AnnotationMirror> annos = TreeUtils.typeOf(returnType).getAnnotationMirrors();
    return atypeFactory.containsSameByClass(annos, NonEmpty.class);
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

    if (leftOp.getKind() == Tree.Kind.NULL_LITERAL && isOptionalType(TreeUtils.typeOf(rightOp))) {
      checker.reportWarning(tree, "optional.null.comparison");
    }
    if (rightOp.getKind() == Tree.Kind.NULL_LITERAL && isOptionalType(TreeUtils.typeOf(leftOp))) {
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
   * <p>{@code CREATION()} wraps a value in an instance of {@link java.util.Optional}, and {@code
   * PROPAGATION()} is a method (or a sequence of methods) that operate on and return {@link
   * java.util.Optional}. {@code ELIMINATION()} is the terminal operation that unwraps the {@link
   * java.util.Optional} value (i.e., {@link Optional#get()}).
   *
   * <p>This creation-propagation-elimination pattern can be eliminated by directly checking whether
   * a value is null before invoking methods on it.
   *
   * @param tree a method invocation that can perhaps be simplified
   */
  @SuppressWarnings("RedundantControlFlow")
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
   * @param tree a method invocation that might create {@code Optional<X>} where X is impermissable:
   *     Optional or Collection
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
    updateMethodsToVerifyWithNonEmptyCheckerGivenNonEmptyVariable(tree);
    VariableElement ve = TreeUtils.elementFromDeclaration(tree);
    TypeMirror tm = ve.asType();
    if (isOptionalType(tm)) {
      ElementKind ekind = TreeUtils.elementFromDeclaration(tree).getKind();
      if (ekind.isField()) {
        checker.reportWarning(tree, "optional.field");
      } else if (ekind == ElementKind.PARAMETER) {
        TreePath paramPath = getCurrentPath();
        Tree parent = paramPath.getParentPath().getLeaf();
        if (parent.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
          // Exception to rule: lambda parameters can have type Optional.
        } else {
          checker.reportWarning(tree, "optional.parameter");
        }
      }
    }
    return super.visitVariable(tree, p);
  }

  /**
   * Given a variable declaration annotated with @{@link NonEmpty}, add the enclosing method in
   * which it is found (if one exists) to the set of methods that must be verified with the
   * Non-Empty Checker.
   *
   * @param tree a variable declaration
   */
  private void updateMethodsToVerifyWithNonEmptyCheckerGivenNonEmptyVariable(VariableTree tree) {
    List<? extends AnnotationMirror> annos =
        TreeUtils.annotationsFromTypeAnnotationTrees(tree.getModifiers().getAnnotations());
    if (atypeFactory.containsSameByClass(annos, NonEmpty.class)) {
      MethodTree enclosingMethod = TreePathUtil.enclosingMethod(this.getCurrentPath());
      if (enclosingMethod != null) {
        namesOfMethodsToVerifyWithNonEmptyChecker.add(enclosingMethod.getName().toString());
      }
    }
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
   * construction of an Optional. Method {@link #handleNestedOptionalCreation} does so.
   */
  private final class OptionalImplTypeValidator extends BaseTypeValidator {

    /**
     * Create an OptionalImplTypeValidator.
     *
     * @param checker the type-checker associated with this type validator
     * @param visitor the visitor associated with this type validator
     * @param atypeFactory the type factory associated with this type validator
     */
    public OptionalImplTypeValidator(
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
          if (isOptionalType(typeArg)) {
            checker.reportWarning(tree, "optional.nesting");
          } else if (isCollectionType(typeArg)) {
            checker.reportWarning(tree, "optional.collection");
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

  /** The fully-qualified names of the 4 optional classes in java.util. */
  private static final Set<String> fqOptionalTypes =
      new HashSet<>(
          Arrays.asList(
              "java.util.Optional",
              "java.util.OptionalDouble",
              "java.util.OptionalInt",
              "java.util.OptionalLong"));

  /**
   * Return true if tm is class Optional, OptionalDouble, OptionalInt, or OptionalLong in java.util.
   *
   * @param tm a type
   * @return true if the given type is Optional, OptionalDouble, OptionalInt, or OptionalLong
   */
  private boolean isOptionalType(TypeMirror tm) {
    return TypesUtils.isDeclaredOfName(tm, fqOptionalTypes);
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

  @Override
  public Void visitMemberReference(MemberReferenceTree tree, Void p) {
    if (isFilterIsPresentMapGet(tree)) {
      // TODO: This is a (sound) workaround until
      // https://github.com/typetools/checker-framework/issues/1345
      // is fixed.
      return null;
    }
    return super.visitMemberReference(tree, p);
  }

  /**
   * Returns true if {@code memberRefTree} is the {@code Optional::get} in {@code
   * Stream.filter(Optional::isPresent).map(Optional::get)}.
   *
   * @param memberRefTree a member reference tree
   * @return true if {@code memberRefTree} the {@code Optional::get} in {@code
   *     Stream.filter(Optional::isPresent).map(Optional::get)}
   */
  private boolean isFilterIsPresentMapGet(MemberReferenceTree memberRefTree) {
    if (!TreeUtils.elementFromUse(memberRefTree).equals(optionalGet)) {
      // The method reference is not Optional::get
      return false;
    }
    // "getPath" means "the path to the node `Optional::get`".
    TreePath getPath = getCurrentPath();
    TreePath getParentPath = getPath.getParentPath();
    // "getParent" means "the parent of the node `Optional::get`".
    Tree getParent = getParentPath.getLeaf();
    if (getParent.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree hasGetAsArgumentTree = (MethodInvocationTree) getParent;
      ExecutableElement hasGetAsArgumentElement = TreeUtils.elementFromUse(hasGetAsArgumentTree);
      if (!hasGetAsArgumentElement.equals(streamMap)) {
        // Optional::get is not an argument to stream#map
        return false;
      }
      // hasGetAsArgumentTree is an invocation of Stream#map(...).
      Tree mapReceiverTree = TreeUtils.getReceiverTree(hasGetAsArgumentTree);
      // Will check whether mapParent is the call `Stream.filter(Optional::isPresent)`.
      if (mapReceiverTree != null && mapReceiverTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
        MethodInvocationTree fluentToMapTree = (MethodInvocationTree) mapReceiverTree;
        ExecutableElement fluentToMapElement = TreeUtils.elementFromUse(fluentToMapTree);
        if (!fluentToMapElement.equals(streamFilter)) {
          // The receiver of map(Optional::get) is not Stream#filter
          return false;
        }
        MethodInvocationTree filterInvocationTree = fluentToMapTree;
        ExpressionTree filterArgTree = filterInvocationTree.getArguments().get(0);
        if (filterArgTree.getKind() == Tree.Kind.MEMBER_REFERENCE) {
          ExecutableElement filterArgElement =
              TreeUtils.elementFromUse((MemberReferenceTree) filterArgTree);
          return filterArgElement.equals(optionalIsPresent);
        }
      }
    }
    return false;
  }
}
