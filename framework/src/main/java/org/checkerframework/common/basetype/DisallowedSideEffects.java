package org.checkerframework.common.basetype;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.FormalParameter;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionConverter;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.SuperReference;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/**
 * Scanner that collects the expressions a method side-effects, beyond those listed in its {@link
 * SideEffectsOnly} annotation, and reports an error for each one.
 *
 * <p>Clients use the static method {@link #checkSideEffectsOnly}.
 */
public class DisallowedSideEffects extends TreePathScanner<Void, Void> {

  /** Expressions the method side-effects that are not in its {@link SideEffectsOnly} annotation. */
  protected final List<IPair<Tree, JavaExpression>> disallowedSideEffects = new ArrayList<>(2);

  /**
   * List of expressions specified as annotation arguments in the {@link SideEffectsOnly} annotation
   * of the method being checked.
   */
  protected final List<JavaExpression> sideEffectsOnlyExpressionsFromAnnotation;

  /**
   * The local variables that always hold an object that the method being checked created. Modifying
   * such an object is not a side effect that is visible to the caller.
   */
  protected final Set<VariableElement> freshLocals;

  /**
   * The lambdas whose bodies this scanner scans, because they are passed to a call that might
   * invoke them. See {@link #checkCallbackArguments}.
   */
  protected final Set<LambdaExpressionTree> scannedLambdas = new HashSet<>(2);

  /** The checker to use. */
  protected final BaseTypeChecker checker;

  /** True if "-AassumeSideEffectFree" or "-AassumePure" was passed on the command line. */
  protected final boolean assumeSideEffectFree;

  /** True if "-AassumePureGetters" was passed on the command line. */
  protected final boolean assumePureGetters;

  /**
   * Creates a new DisallowedSideEffects.
   *
   * @param sideEffectsOnlyExpressions the arguments/values of the {@link SideEffectsOnly}
   *     annotation of the method being checked
   * @param freshLocals the local variables that always hold an object that the method being checked
   *     created
   * @param checker the checker to use
   * @param assumeSideEffectFree true if every method should be assumed to be side-effect-free
   * @param assumePureGetters true if every getter should be assumed to be side-effect-free
   */
  protected DisallowedSideEffects(
      List<JavaExpression> sideEffectsOnlyExpressions,
      Set<VariableElement> freshLocals,
      BaseTypeChecker checker,
      boolean assumeSideEffectFree,
      boolean assumePureGetters) {
    this.sideEffectsOnlyExpressionsFromAnnotation = sideEffectsOnlyExpressions;
    this.freshLocals = freshLocals;
    this.checker = checker;
    this.assumeSideEffectFree = assumeSideEffectFree;
    this.assumePureGetters = assumePureGetters;
  }

  /**
   * Issues warnings about side effects in {@code statement} beyond the {@code @SideEffectsOnly}
   * annotation.
   *
   * @param statement the statement to check; currently, at the only call site it is a method body
   * @param sideEffectsOnlyExpressions the values in the {@link SideEffectsOnly} annotation
   * @param checker the checker to use
   * @param methodTree the method that contains {@code statement}
   * @param assumeSideEffectFree true if every method should be assumed to be side-effect-free
   * @param assumePureGetters true if every getter should be assumed to be side-effect-free
   */
  public static void checkSideEffectsOnly(
      TreePath statement,
      List<JavaExpression> sideEffectsOnlyExpressions,
      BaseTypeChecker checker,
      MethodTree methodTree,
      boolean assumeSideEffectFree,
      boolean assumePureGetters) {
    if (TreeUtils.isConstructor(methodTree)) {
      checkSideEffectsOnlyConstructor(
          statement,
          sideEffectsOnlyExpressions,
          checker,
          methodTree,
          assumeSideEffectFree,
          assumePureGetters);
    } else {
      checkSideEffectsOnly(
          statement,
          sideEffectsOnlyExpressions,
          checker,
          methodTree.getName(),
          assumeSideEffectFree,
          assumePureGetters);
    }
  }

  /**
   * Issues warnings about side effects in the given constructor beyond the {@code @SideEffectsOnly}
   * annotation.
   *
   * <p>{@code this} is treated as if it appeared in the annotation, whether or not it does. Also,
   * the code that runs before the constructor's body -- the superclass constructor and the
   * enclosing class's instance initializers -- is checked along with the body.
   *
   * @param statement the constructor body to check
   * @param sideEffectsOnlyExpressions the values in the {@link SideEffectsOnly} annotation
   * @param checker the checker to use
   * @param methodTree the constructor that contains {@code statement}
   * @param assumeSideEffectFree true if every method should be assumed to be side-effect-free
   * @param assumePureGetters true if every getter should be assumed to be side-effect-free
   */
  private static void checkSideEffectsOnlyConstructor(
      TreePath statement,
      List<JavaExpression> sideEffectsOnlyExpressions,
      BaseTypeChecker checker,
      MethodTree methodTree,
      boolean assumeSideEffectFree,
      boolean assumePureGetters) {
    // A constructor implicitly side-effects the object under construction, which did not exist
    // before the call, so modifying it is not a side effect that is visible to the caller.  The
    // effect is the same as if the programmer had written `this` in the annotation, which is also
    // permitted.
    ExecutableElement constructorElt = TreeUtils.elementFromDeclaration(methodTree);
    List<JavaExpression> seOnlyExpressions = new ArrayList<>(sideEffectsOnlyExpressions);
    seOnlyExpressions.add(new ThisReference(constructorElt.getEnclosingElement().asType()));

    // Calling constructor c runs:
    // * the superclass's no-argument constructor, unless c contains a constructor call.
    // * the class's instance initializers, unless c delegates to another constructor of the same
    //   class
    // * its body
    // javac does not put the extra code in the constructor's AST until after the Checker Framework
    // has run, so check them here.
    MethodInvocationTree explicitCall = TreeUtils.getExplicitConstructorCall(methodTree);
    List<TreePath> initializers =
        explicitCall != null && TreeUtils.isThisConstructorCall(explicitCall)
            ? Collections.emptyList()
            : TreePathUtil.getInstanceInitializers(statement);

    List<Tree> checkedCodeTrees = new ArrayList<>(initializers.size() + 1);
    for (TreePath initializer : initializers) {
      checkedCodeTrees.add(initializer.getLeaf());
    }
    checkedCodeTrees.add(statement.getLeaf());
    DisallowedSideEffects scanner =
        new DisallowedSideEffects(
            seOnlyExpressions,
            freshLocals(checkedCodeTrees),
            checker,
            assumeSideEffectFree,
            assumePureGetters);
    if (explicitCall == null) {
      scanner.checkImplicitSuperCall(methodTree, constructorElt);
    }
    for (TreePath initializer : initializers) {
      scanner.scan(initializer, null);
    }
    scanner.scan(statement, null);
    scanner.report(ElementUtils.getSimpleDescription(constructorElt));
  }

  /**
   * Checks the call to the superclass's no-argument constructor that the compiler inserts in a
   * constructor that contains no explicit {@code this(...)} or {@code super(...)} call, and records
   * any side effect of it that is beyond what the {@link SideEffectsOnly} annotation of the
   * constructor being checked permits.
   *
   * @param node the tree to report an error at
   * @param constructorElt the constructor being checked
   */
  protected void checkImplicitSuperCall(Tree node, ExecutableElement constructorElt) {
    TypeElement classElt = (TypeElement) constructorElt.getEnclosingElement();
    TypeMirror superclass = classElt.getSuperclass();
    if (superclass.getKind() != TypeKind.DECLARED) {
      // The class has no superclass, so it is `java.lang.Object`, and no call is inserted.
      return;
    }
    ElementKind classKind = classElt.getKind();
    if (classKind == ElementKind.ENUM || classKind == ElementKind.RECORD) {
      // The inserted call is to the constructor of `java.lang.Enum` or of `java.lang.Record`,
      // which modifies only the object under construction.
      return;
    }
    TypeElement superclassElt = (TypeElement) ((DeclaredType) superclass).asElement();
    ExecutableElement superConstructor = ElementUtils.getNoArgumentConstructor(superclassElt);
    if (superConstructor == null) {
      checker.reportError(
          node, "purity.unknown.sideeffectsonly", TypesUtils.simpleTypeName(superclass));
      return;
    }
    checkImplicitCall(node, superConstructor, null);
  }

  /**
   * Issues warnings about side effects beyond the given expressions, which come from a
   * {@code @SideEffectsOnly} annotation.
   *
   * <p>Unlike the other overload, this one does not treat the code as a constructor body: the
   * caller has already put every permitted expression in {@code sideEffectsOnlyExpressions}.
   *
   * @param statement the statement to check; a method body or the body of a lambda
   * @param sideEffectsOnlyExpressions the expressions that the code may side-effect, written in
   *     terms of the code being checked
   * @param checker the checker to use
   * @param methodName the name to use in diagnostics for the code being checked
   * @param assumeSideEffectFree true if every method should be assumed to be side-effect-free
   * @param assumePureGetters true if every getter should be assumed to be side-effect-free
   */
  public static void checkSideEffectsOnly(
      TreePath statement,
      List<JavaExpression> sideEffectsOnlyExpressions,
      BaseTypeChecker checker,
      CharSequence methodName,
      boolean assumeSideEffectFree,
      boolean assumePureGetters) {
    DisallowedSideEffects scanner =
        new DisallowedSideEffects(
            sideEffectsOnlyExpressions,
            freshLocals(Collections.singletonList(statement.getLeaf())),
            checker,
            assumeSideEffectFree,
            assumePureGetters);
    scanner.scan(statement, null);
    scanner.report(methodName);
  }

  /**
   * Reports an error for each side effect that this scanner found and that the {@link
   * SideEffectsOnly} annotation does not permit.
   *
   * @param methodName the name to use in diagnostics for the code that was checked
   */
  protected void report(CharSequence methodName) {
    for (IPair<Tree, JavaExpression> s : disallowedSideEffects) {
      checker.reportError(s.first, "purity.incorrect.sideeffectsonly", methodName, s.second);
    }
  }

  /**
   * Returns the JavaExpression for the given tree, with every use of {@code super} replaced by
   * {@code this}. {@code super.f} and {@code this.f} are the same location, so they must be
   * compared alike against the {@link SideEffectsOnly} annotation, which cannot mention {@code
   * super}.
   *
   * @param tree an expression tree
   * @return the JavaExpression for the tree, written in terms of {@code this} rather than {@code
   *     super}
   */
  protected static JavaExpression expressionFromTree(ExpressionTree tree) {
    return JavaExpression.superToThis(JavaExpression.fromTree(tree));
  }

  // A `this(...)` or `super(...)` call is a MethodInvocationTree, so `visitMethodInvocation`
  // handles it.  A `new` expression is handled by `visitNewClass`.
  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
    checkMethodInvocation(node);
    return super.visitMethodInvocation(node, aVoid);
  }

  /**
   * Records the disallowed side effects of the given method invocation, and reports an error if the
   * callee has no side-effect annotation. Does not scan the subtrees of the invocation.
   *
   * @param node a method invocation
   */
  protected void checkMethodInvocation(MethodInvocationTree node) {
    ExecutableElement invokedElem = TreeUtils.elementFromUse(node);
    if (invokedElem == null || TreeUtils.isEnumSuperCall(node)) {
      return;
    }
    if (modifiesNothing(invokedElem)) {
      return;
    }
    AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
    AnnotationMirror seOnlyAnnotation =
        atypeFactory.getDeclAnnotation(invokedElem, SideEffectsOnly.class);
    if (seOnlyAnnotation == null) {
      // The callee has no side-effect annotation, so it might modify arbitrary state.
      checker.reportError(
          node, "purity.unknown.sideeffectsonly", ElementUtils.getSimpleDescription(invokedElem));
      return;
    }

    // The callee modifies at most the expressions listed in its own `@SideEffectsOnly` annotation.
    for (JavaExpression expr : calleeSideEffectedExpressions(node, invokedElem, seOnlyAnnotation)) {
      if (isDisallowedSideEffectedExpression(expr)) {
        disallowedSideEffects.add(IPair.of(node, expr));
      }
    }
    checkCallbackArguments(node.getArguments());
  }

  /**
   * Checks each argument that is a functional interface, because the callee might invoke as a
   * callback.
   *
   * <p>If the functional interface method is annotated, this method does nothing: {@code
   * BaseTypeVisitor.checkLambdaSideEffectsOnly} checks the lambda's body.
   *
   * <p>Otherwise, a lambda argument's body is scanned as part of the code being checked, because
   * the callee might run it before returning. Any other argument, such as a variable that holds a
   * lambda, is reported: its body is not at hand to be scanned.
   *
   * @param arguments the arguments of a call to a method or constructor that is annotated with
   *     {@link SideEffectsOnly}
   */
  protected void checkCallbackArguments(List<? extends ExpressionTree> arguments) {
    ProcessingEnvironment processingEnv = checker.getProcessingEnvironment();
    AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
    for (ExpressionTree argument : arguments) {
      ExpressionTree arg = TreeUtils.withoutParens(argument);
      TypeMirror argType = TreeUtils.typeOf(arg);
      if (!TypesUtils.isFunctionalInterface(argType, processingEnv)) {
        continue;
      }
      ExecutableElement functionalMethod = TypesUtils.findFunction(argType, processingEnv);
      if (atypeFactory.getDeclAnnotation(functionalMethod, SideEffectsOnly.class) != null
          || modifiesNothing(functionalMethod)) {
        continue;
      }
      if (arg instanceof LambdaExpressionTree lambda) {
        // The body is at hand, so check it as part of the code being checked.
        scannedLambdas.add(lambda);
        continue;
      }
      if (arg instanceof MemberReferenceTree memberReference) {
        ExecutableElement referenced = TreeUtils.elementFromUse(memberReference);
        if (referenced != null && modifiesNothing(referenced)) {
          continue;
        }
      }
      checker.reportError(
          arg,
          "purity.unknown.sideeffectsonly",
          ElementUtils.getSimpleDescription(functionalMethod));
    }
  }

  /**
   * Returns true if the given method promises to modify nothing that existed before it was called.
   *
   * @param elt a method or constructor
   * @return true if the given method modifies nothing
   */
  protected boolean modifiesNothing(ExecutableElement elt) {
    if (assumeSideEffectFree || (assumePureGetters && ElementUtils.isGetter(elt))) {
      // The user asked that the method be assumed to modify nothing.
      return true;
    }
    AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
    return atypeFactory.getDeclAnnotation(elt, Pure.class) != null
        || atypeFactory.getDeclAnnotation(elt, SideEffectFree.class) != null;
  }

  /**
   * Returns the expressions that the invoked method may side-effect: the arguments/elements of its
   * {@link SideEffectsOnly} annotation, viewpoint-adapted to the given call site.
   *
   * @param node a call to a method that is annotated with {@link SideEffectsOnly}
   * @param invokedElem the invoked method
   * @param seOnlyAnnotation the invoked method's {@link SideEffectsOnly} annotation
   * @return the expressions that the invoked method side-effects, viewpoint-adapted to {@code node}
   */
  protected List<JavaExpression> calleeSideEffectedExpressions(
      MethodInvocationTree node, ExecutableElement invokedElem, AnnotationMirror seOnlyAnnotation) {
    List<String> exprStrings =
        checker.getTypeFactory().getSideEffectsOnlyExpressions(seOnlyAnnotation);
    List<JavaExpression> result = new ArrayList<>(exprStrings.size());
    for (String exprString : exprStrings) {
      try {
        // At a call of the form `super.m()`, viewpoint-adapting the callee's `this` yields `super`,
        // which denotes the same object that the caller writes as `this`.  See
        // `expressionFromTree`.
        result.add(
            JavaExpression.superToThis(
                StringToJavaExpression.atMethodInvocation(exprString, node, checker)));
      } catch (JavaExpressionParseException ex) {
        checker.reportError(
            node,
            "purity.unparseable.sideeffectsonly",
            ElementUtils.getSimpleDescription(invokedElem),
            exprString);
        // If an expression cannot be parsed at the call site, the checker cannot tell what the
        // callee modifies, so be conservative.
        return Collections.emptyList();
      }
    }
    return result;
  }

  // An enhanced `for` loop and a try-with-resources statement contain calls that appear only after
  // the compiler desugars them.  `visitEnhancedForLoop` and `visitTry` check those calls.  The
  // methods they call are supposed to side effect at most the receiver, so these checks should
  // rarely report anything.

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void aVoid) {
    if (assumeSideEffectFree) {
      // The user asked that every callee be assumed to modify nothing.
      return super.visitEnhancedForLoop(node, aVoid);
    }
    ExpressionTree iterableExpr = node.getExpression();
    TypeMirror iterableType = TreeUtils.typeOf(iterableExpr);
    if (iterableType.getKind() != TypeKind.ARRAY) {
      // The loop is compiled to
      //   `Iterator<T> i = expr.iterator(); while (i.hasNext()) { T x = i.next(); ... }`.
      // The loop creates the iterator, so no caller can observe a modification of it; only what
      // `iterator()` modifies is visible outside the method.
      ExecutableElement iteratorMethod = noArgumentMethod(iterableType, "iterator");
      if (iteratorMethod == null) {
        checker.reportError(node, "purity.unknown.sideeffectsonly", "iterator");
        return super.visitEnhancedForLoop(node, aVoid);
      }
      checkImplicitCall(node, iteratorMethod, expressionFromTree(iterableExpr));
      TypeMirror iteratorType = iteratorMethod.getReturnType();
      for (String iteratorMethodName : new String[] {"hasNext", "next"}) {
        ExecutableElement iteratorElem = noArgumentMethod(iteratorType, iteratorMethodName);
        if (iteratorElem == null) {
          checker.reportError(node, "purity.unknown.sideeffectsonly", iteratorMethodName);
        } else {
          checkImplicitCall(node, iteratorElem, null);
        }
      }
    }
    return super.visitEnhancedForLoop(node, aVoid);
  }

  @Override
  public Void visitTry(TryTree node, Void aVoid) {
    if (assumeSideEffectFree) {
      // The user asked that every callee be assumed to modify nothing.
      return super.visitTry(node, aVoid);
    }
    for (Tree resource : node.getResources()) {
      // Each resource's `close()` method is called when the block exits.
      JavaExpression resourceExpr;
      if (resource instanceof VariableTree variableTree) {
        resourceExpr = JavaExpression.fromVariableTree(variableTree);
      } else {
        resourceExpr = expressionFromTree((ExpressionTree) resource);
      }
      ExecutableElement closeMethod = noArgumentMethod(TreeUtils.typeOf(resource), "close");
      if (closeMethod == null) {
        checker.reportError(resource, "purity.unknown.sideeffectsonly", "close");
      } else {
        checkImplicitCall(resource, closeMethod, resourceExpr);
      }
    }
    return super.visitTry(node, aVoid);
  }

  /**
   * Checks a call that the compiler introduces when it desugars the source code, and records any
   * side effect of it that is beyond what the {@link SideEffectsOnly} annotation of the method
   * being checked permits.
   *
   * @param node the tree to report an error at
   * @param invokedElem the implicitly invoked method or constructor, which takes no arguments
   * @param receiver the receiver of the call, or null if the receiver is an object that the
   *     desugaring created and that therefore no caller can refer to
   */
  protected void checkImplicitCall(
      Tree node, ExecutableElement invokedElem, @Nullable JavaExpression receiver) {
    if (modifiesNothing(invokedElem)) {
      return;
    }
    AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
    AnnotationMirror seOnlyAnnotation =
        atypeFactory.getDeclAnnotation(invokedElem, SideEffectsOnly.class);
    if (seOnlyAnnotation == null) {
      // The callee has no side-effect annotation, so it might modify arbitrary state.
      checker.reportError(
          node, "purity.unknown.sideeffectsonly", ElementUtils.getSimpleDescription(invokedElem));
      return;
    }

    List<String> exprStrings = atypeFactory.getSideEffectsOnlyExpressions(seOnlyAnnotation);
    for (String exprString : exprStrings) {
      JavaExpression atDeclaration;
      try {
        atDeclaration = StringToJavaExpression.atMethodDecl(exprString, invokedElem, checker);
      } catch (JavaExpressionParseException ex) {
        checker.reportError(
            node,
            "purity.unparseable.sideeffectsonly",
            ElementUtils.getSimpleDescription(invokedElem),
            exprString);
        return;
      }
      if (receiver == null) {
        if (atDeclaration instanceof ThisReference) {
          // The expression is the object that the desugaring created, which no caller can refer
          // to, so modifying it is not a side effect that is visible to the caller.
          continue;
        }
        if (atDeclaration.containedOfClass(ThisReference.class) != null) {
          // The expression is reached through the object that the desugaring created.  That object
          // is not nameable here, so the expression cannot be viewpoint-adapted; and its value may
          // be an
          // object that existed before the call, so it cannot be dismissed as unobservable either.
          checker.reportError(
              node,
              "purity.unknown.sideeffectsonly",
              ElementUtils.getSimpleDescription(invokedElem));
          return;
        }
      } else {
        atDeclaration = withReceiver(atDeclaration, receiver);
      }
      if (isDisallowedSideEffectedExpression(atDeclaration)) {
        disallowedSideEffects.add(IPair.of(node, atDeclaration));
      }
    }
  }

  /**
   * Returns the given expression with every use of {@code this} replaced by the given receiver.
   * This is viewpoint adaptation for a call that takes no arguments, so no formal parameter needs
   * to be replaced.
   *
   * @param expr an expression written at a method's declaration
   * @param receiver the receiver of a call to that method
   * @return the expression, written at the call site
   */
  protected static JavaExpression withReceiver(JavaExpression expr, JavaExpression receiver) {
    if (!expr.containsOfClass(ThisReference.class)) {
      return expr;
    }
    JavaExpressionConverter converter =
        new JavaExpressionConverter() {
          @Override
          protected JavaExpression visitThisReference(ThisReference thisExpr, Void unused) {
            return receiver;
          }
        };
    return converter.convert(expr);
  }

  /**
   * Returns the no-argument method with the given name that a call on an expression of the given
   * type invokes, or null if there is no such method.
   *
   * @param receiverType the type of the receiver of the call
   * @param methodName the name of the method
   * @return the invoked method, or null if the type has no such method
   */
  protected @Nullable ExecutableElement noArgumentMethod(
      TypeMirror receiverType, String methodName) {
    TypeMirror upperBound = TypesUtils.upperBound(receiverType);
    if (upperBound instanceof IntersectionType intersectionType) {
      for (TypeMirror bound : intersectionType.getBounds()) {
        ExecutableElement result = noArgumentMethod(bound, methodName);
        if (result != null) {
          return result;
        }
      }
      return null;
    }
    if (!(upperBound instanceof DeclaredType declaredType)) {
      return null;
    }
    Elements elements = checker.getElementUtils();
    for (ExecutableElement method :
        ElementFilter.methodsIn(elements.getAllMembers((TypeElement) declaredType.asElement()))) {
      // A static method is not a candidate, because the desugared call has a receiver. Neither is a
      // synthetic method: a covariant-return override such as {@code MyIterator iterator()} makes
      // javac add a bridge method {@code Iterator iterator()} to the member closure, and the bridge
      // carries none of the declaration annotations that the method it forwards to does.
      if (method.getParameters().isEmpty()
          && method.getSimpleName().contentEquals(methodName)
          && !method.getModifiers().contains(Modifier.STATIC)
          && !TreeUtils.isSynthetic(method)) {
        return method;
      }
    }
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree node, Void aVoid) {
    ExecutableElement constructorElt = TreeUtils.elementFromUse(node);
    if (constructorElt == null) {
      return super.visitNewClass(node, aVoid);
    }
    if (modifiesNothing(constructorElt)) {
      return super.visitNewClass(node, aVoid);
    }
    AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
    AnnotationMirror seOnlyAnnotation =
        atypeFactory.getDeclAnnotation(constructorElt, SideEffectsOnly.class);
    if (seOnlyAnnotation == null) {
      // The constructor has no side-effect annotation, so it might modify arbitrary state.
      checker.reportError(
          node,
          "purity.unknown.sideeffectsonly",
          ElementUtils.getSimpleDescription(constructorElt));
      return super.visitNewClass(node, aVoid);
    }

    // The constructor modifies at most the expressions listed in its own `@SideEffectsOnly`
    // annotation.
    for (JavaExpression expr :
        constructorSideEffectedExpressions(node, constructorElt, seOnlyAnnotation)) {
      if (isDisallowedSideEffectedExpression(expr)) {
        disallowedSideEffects.add(IPair.of(node, expr));
      }
    }
    checkCallbackArguments(node.getArguments());
    return super.visitNewClass(node, aVoid);
  }

  /**
   * Returns the expressions that the invoked constructor side-effects: the arguments/elements of
   * its {@link SideEffectsOnly} annotation, viewpoint-adapted to the given call site.
   *
   * <p>The expression {@code this} is omitted from the result. In a constructor's annotation,
   * {@code this} is the object being constructed, which did not exist before the call, so modifying
   * it is not a side effect that is visible to the caller. A larger expression that merely
   * <em>contains</em> {@code this}, such as {@code this.f}, gets no such exemption: its value may
   * be an object that existed before the call, as it does for a constructor whose body contains
   * {@code this.f = p;} where {@code p} is a formal parameter.
   *
   * <p>If an expression cannot be parsed, this reports {@code purity.unparseable.sideeffectsonly}
   * and returns an empty list, just as {@link #calleeSideEffectedExpressions} does.
   *
   * @param node a call to a constructor that is annotated with {@link SideEffectsOnly}
   * @param constructorElt the invoked constructor
   * @param seOnlyAnnotation the invoked constructor's {@link SideEffectsOnly} annotation
   * @return the expressions that the invoked constructor side-effects, viewpoint-adapted to {@code
   *     node}
   */
  protected List<JavaExpression> constructorSideEffectedExpressions(
      NewClassTree node, ExecutableElement constructorElt, AnnotationMirror seOnlyAnnotation) {
    List<String> exprStrings =
        checker.getTypeFactory().getSideEffectsOnlyExpressions(seOnlyAnnotation);
    List<JavaExpression> result = new ArrayList<>(exprStrings.size());
    for (String exprString : exprStrings) {
      JavaExpression atDeclaration;
      try {
        atDeclaration = StringToJavaExpression.atMethodDecl(exprString, constructorElt, checker);
      } catch (JavaExpressionParseException ex) {
        checker.reportError(
            node,
            "purity.unparseable.sideeffectsonly",
            ElementUtils.getSimpleDescription(constructorElt),
            exprString);
        return Collections.emptyList();
      }
      if (atDeclaration instanceof ThisReference) {
        // The expression is the object under construction, which no caller can refer to, so
        // modifying it is not a side effect that is visible to the caller.
        continue;
      }
      if (atDeclaration.containedOfClass(ThisReference.class) != null) {
        // The expression is reached through the object under construction.  That object is not
        // nameable at the call site, so the expression cannot be viewpoint-adapted; and its value
        // may be an object that existed before the call, so it cannot be dismissed as unobservable
        // either.
        checker.reportError(
            node,
            "purity.unknown.sideeffectsonly",
            ElementUtils.getSimpleDescription(constructorElt));
        return Collections.emptyList();
      }
      result.add(atDeclaration.atConstructorInvocation(node));
    }
    return result;
  }

  /**
   * Returns true if the given expression is a side-effected expression beyond what is listed in the
   * {@link SideEffectsOnly} annotation. That is, all of the following hold:
   *
   * <ul>
   *   <li>The expression's value is modifiable by other code.
   *   <li>The expression is not an object that the method being checked created, in the sense of
   *       {@link #isFreshlyAllocated}.
   *   <li>The expression is not covered by the {@link SideEffectsOnly} annotation, in the sense of
   *       {@link #isCoveredByAnnotation}.
   * </ul>
   *
   * <p>Use this for an expression whose <em>value</em> is mutated, such as an expression that a
   * callee modifies (e.g., one of the arguments). For an expression that is <em>assigned to</em>,
   * use {@link #isDisallowedAssignmentTarget}.
   *
   * @param expr the expression to check for side-effecting
   * @return true if the given expression is a side-effected expression beyond what is listed in the
   *     {@link SideEffectsOnly} annotation
   */
  protected boolean isDisallowedSideEffectedExpression(JavaExpression expr) {
    return expr.isModifiableByOtherCode()
        && !isFreshlyAllocated(expr)
        && !isCoveredByAnnotation(expr);
  }

  /**
   * Returns true if assigning to the given expression is a side effect beyond what is listed in the
   * {@link SideEffectsOnly} annotation. That is, all of the following hold:
   *
   * <ul>
   *   <li>The expression is assignable by other code; equivalently, the assignment is visible
   *       outside the method being checked. (Assigning to a local variable is not.)
   *   <li>The expression is not part of an object that the method being checked created, in the
   *       sense of {@link #isPartOfFreshlyAllocated}.
   *   <li>The expression is not covered by the {@link SideEffectsOnly} annotation, in the sense of
   *       {@link #isCoveredByAnnotation}.
   * </ul>
   *
   * @param expr the expression that is assigned to
   * @return true if assigning to the given expression is a side effect beyond what is listed in the
   *     {@link SideEffectsOnly} annotation
   */
  protected boolean isDisallowedAssignmentTarget(JavaExpression expr) {
    return expr.isAssignableByOtherCode()
        && !isPartOfFreshlyAllocated(expr)
        && !isCoveredByAnnotation(expr);
  }

  /**
   * Returns true if the given expression is a local variable that always holds an object that the
   * method being checked created. The object did not exist before the call, so modifying it is not
   * a side effect that is visible to the caller.
   *
   * <p>If the object escapes -- if the method stores it into pre-existing state -- then that store
   * is itself a side effect, which is reported unless the annotation covers it. If it is covered,
   * then so is every modification of the object, because the object is then reached through a
   * listed expression.
   *
   * @param expr an expression
   * @return true if the given expression is a local variable holding an object that this method
   *     created
   */
  protected boolean isFreshlyAllocated(JavaExpression expr) {
    return expr instanceof LocalVariable localVariable
        && freshLocals.contains(localVariable.getElement());
  }

  /**
   * Returns true if the given expression is a field or an array element of an object that the
   * method being checked created. Assigning to it is not visible to the caller.
   *
   * <p>Only the object's own fields and elements qualify. A field of a field does not: {@code
   * fresh.f} may be an object that existed before the call, so assigning to {@code fresh.f.g} is
   * visible to the caller.
   *
   * @param expr an expression
   * @return true if the given expression is a field or array element of an object that this method
   *     created
   */
  protected boolean isPartOfFreshlyAllocated(JavaExpression expr) {
    if (expr instanceof FieldAccess fieldAccess) {
      return isFreshlyAllocated(fieldAccess.getReceiver());
    } else if (expr instanceof ArrayAccess arrayAccess) {
      return isFreshlyAllocated(arrayAccess.getArray());
    } else {
      return false;
    }
  }

  /**
   * Returns true if the given expression is listed in the {@link SideEffectsOnly} annotation or is
   * reached through one of the listed expressions.
   *
   * @param expr the expression to look for
   * @return true if the given expression is covered by the {@link SideEffectsOnly} annotation
   */
  protected boolean isCoveredByAnnotation(JavaExpression expr) {
    for (JavaExpression seOnlyExpression : sideEffectsOnlyExpressionsFromAnnotation) {
      if (expr.containsAsReceiver(checker.getTypeFactory(), seOnlyExpression)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if every evaluation of the given expression yields the same location or value.
   *
   * <p>A {@code @Deterministic} method returns the same value every time it is called with the same
   * arguments, so a call to one is deterministic so long as its receiver and its arguments are.
   *
   * @param expression an expression
   * @param provider how to get annotations
   * @return true if every evaluation of the expression yields the same location or value
   */
  protected static boolean isDeterministic(JavaExpression expression, AnnotationProvider provider) {
    if (expression instanceof LocalVariable
        || expression instanceof FormalParameter
        || expression instanceof ThisReference
        || expression instanceof SuperReference
        || expression instanceof ClassName
        || expression instanceof ValueLiteral) {
      return true;
    } else if (expression instanceof FieldAccess fieldAccess) {
      return isDeterministic(fieldAccess.getReceiver(), provider);
    } else if (expression instanceof ArrayAccess arrayAccess) {
      return isDeterministic(arrayAccess.getArray(), provider)
          && isDeterministic(arrayAccess.getIndex(), provider);
    } else if (expression instanceof MethodCall methodCall) {
      ExecutableElement method = methodCall.getElement();
      if (!PurityUtils.isDeterministic(provider, method)
          || !PurityUtils.isSideEffectFree(provider, method)) {
        return false;
      }
      // For a static method, the receiver is a ClassName, which is deterministic.
      if (!isDeterministic(methodCall.getReceiver(), provider)) {
        return false;
      }
      for (JavaExpression argument : methodCall.getArguments()) {
        if (!isDeterministic(argument, provider)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree node, Void aVoid) {
    if (scannedLambdas.contains(node)) {
      // The lambda is passed to a call that might invoke it before it returns, and no annotation
      // constrains what the invocation modifies.  See `checkCallbackArguments`.
      return super.visitLambdaExpression(node, aVoid);
    }
    // The body of a lambda runs when the lambda is invoked, which is not necessarily within the
    // method being checked.  Wherever it is invoked, the invocation is a call to a method of a
    // functional interface, and `visitMethodInvocation` checks that call.  The body itself is
    // checked against that interface method's annotation, by
    // `BaseTypeVisitor.checkLambdaSideEffectsOnly`.
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, Void aVoid) {
    // Likewise for the methods of an anonymous, local, or nested class that is declared within
    // the method being checked.  Their bodies run when those methods are called, and each such
    // call is checked where it appears.  (Instance initializers run when the class is
    // instantiated, and `visitNewClass` checks that.)
    return null;
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, Void aVoid) {
    // An annotation is not code that runs, so it has no side effect.  Its arguments must not be
    // scanned: an argument such as the `value = 1` of `@A(value = 1)` is an AssignmentTree whose
    // left-hand side stands for an annotation element rather than for a variable.
    return null;
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void aVoid) {
    JavaExpression lhs = expressionFromTree(node.getVariable());
    if (isDisallowedAssignmentTarget(lhs)) {
      disallowedSideEffects.add(IPair.of(node, lhs));
    }
    return super.visitAssignment(node, aVoid);
  }

  @Override
  public Void visitUnary(UnaryTree node, Void aVoid) {
    switch (node.getKind()) {
      case POSTFIX_INCREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, PREFIX_DECREMENT -> {
        JavaExpression operand = expressionFromTree(node.getExpression());
        if (isDisallowedAssignmentTarget(operand)) {
          disallowedSideEffects.add(IPair.of(node, operand));
        }
      }
      default -> {}
    }
    return super.visitUnary(node, aVoid);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, Void aVoid) {
    JavaExpression lhs = expressionFromTree(node.getVariable());
    if (isDisallowedAssignmentTarget(lhs)) {
      disallowedSideEffects.add(IPair.of(node, lhs));
    }
    return super.visitCompoundAssignment(node, aVoid);
  }

  /**
   * Returns the local variables that always hold an object that the given code created: those that
   * are assigned only {@code new} expressions.
   *
   * @param trees the code being checked: the body of the method, plus the instance initializers if
   *     the method is a constructor
   * @return the local variables that always hold an object that {@code trees} created
   */
  protected static Set<VariableElement> freshLocals(List<? extends Tree> trees) {
    FreshLocalScanner scanner = new FreshLocalScanner();
    for (Tree tree : trees) {
      scanner.scan(tree, null);
    }
    scanner.freshlyAssigned.removeAll(scanner.otherwiseAssigned);
    return scanner.freshlyAssigned;
  }

  /**
   * Finds the local variables that are assigned only {@code new} expressions. The result is {@link
   * #freshlyAssigned} minus {@link #otherwiseAssigned}.
   *
   * <p>A local variable that a lambda or a local class assigns is not effectively final, so it
   * cannot be captured; therefore, scanning the whole method body -- including such bodies, which
   * {@link DisallowedSideEffects} itself does not always scan -- cannot miss an assignment.
   */
  private static class FreshLocalScanner extends TreeScanner<Void, Void> {

    /** The local variables that are assigned a {@code new} expression. */
    final Set<VariableElement> freshlyAssigned = new HashSet<>(2);

    /** The local variables that are assigned something other than a {@code new} expression. */
    final Set<VariableElement> otherwiseAssigned = new HashSet<>(2);

    /** Creates a FreshLocalScanner. */
    FreshLocalScanner() {}

    @Override
    public Void visitAnnotation(AnnotationTree node, Void aVoid) {
      // An annotation assigns nothing; see `DisallowedSideEffects.visitAnnotation`.
      return null;
    }

    @Override
    public Void visitVariable(VariableTree node, Void aVoid) {
      ExpressionTree initializer = node.getInitializer();
      if (initializer != null) {
        // A declaration with no initializer says nothing about what the variable holds.
        record(TreeUtils.elementFromDeclaration(node), initializer);
      }
      return super.visitVariable(node, aVoid);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void aVoid) {
      record(TreeUtils.elementFromTree(node.getVariable()), node.getExpression());
      return super.visitAssignment(node, aVoid);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void aVoid) {
      // The result of `+=`, the only compound assignment that applies to a reference type, is a
      // new String rather than an object that this code created.
      VariableElement local = localVariable(TreeUtils.elementFromTree(node.getVariable()));
      if (local != null) {
        otherwiseAssigned.add(local);
      }
      return super.visitCompoundAssignment(node, aVoid);
    }

    /**
     * Records one assignment of a value to a variable. Does nothing if the variable is not a local
     * variable.
     *
     * @param element the variable that is assigned to, or null if the assignment target is not a
     *     variable
     * @param value the assigned value
     */
    private void record(@Nullable Element element, ExpressionTree value) {
      VariableElement local = localVariable(element);
      if (local == null) {
        return;
      }
      Tree.Kind valueKind = TreeUtils.withoutParens(value).getKind();
      if (valueKind == Tree.Kind.NEW_CLASS || valueKind == Tree.Kind.NEW_ARRAY) {
        freshlyAssigned.add(local);
      } else {
        otherwiseAssigned.add(local);
      }
    }

    /**
     * Returns the given element if it is a local variable or a try-with-resources resource
     * variable, and null otherwise.
     *
     * @param element an element, or null
     * @return the element if it is a local variable, otherwise null
     */
    private static @Nullable VariableElement localVariable(@Nullable Element element) {
      if (element != null
          && (element.getKind() == ElementKind.LOCAL_VARIABLE
              || element.getKind() == ElementKind.RESOURCE_VARIABLE)) {
        return (VariableElement) element;
      }
      return null;
    }
  }
}
