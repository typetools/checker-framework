package org.checkerframework.common.delegation;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.delegation.qual.Delegate;
import org.checkerframework.common.delegation.qual.InheritableByDelegator;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The visitor for the Delegation Checker. See the {@link DelegationChecker} documentation for the
 * checks that this class performs.
 *
 * @checker_framework.manual #delegation-checker Delegation Checker
 */
public class DelegationVisitor extends SourceVisitor<Void, Void> {

  /** The canonical name of the {@link Delegate} annotation. */
  private static final String DELEGATE_NAME = Delegate.class.getCanonicalName();

  /** The canonical name of the {@link InheritableByDelegator} annotation. */
  private static final String INHERITABLE_BY_DELEGATOR_NAME =
      InheritableByDelegator.class.getCanonicalName();

  /** The checker that is using this visitor. */
  private final DelegationChecker checker;

  /**
   * For each class that encloses the tree currently being visited, its field that is annotated with
   * {@link Delegate}. The innermost class is the last element. An element is null if the
   * corresponding class does not have exactly one field annotated with {@link Delegate}.
   */
  private final List<@Nullable VariableTree> delegates = new ArrayList<>();

  /**
   * Creates a DelegationVisitor.
   *
   * @param checker the associated checker
   */
  public DelegationVisitor(DelegationChecker checker) {
    super(checker);
    this.checker = checker;
  }

  /**
   * Returns the field annotated with {@link Delegate} of the innermost class that encloses the tree
   * currently being visited, or null if that class does not have exactly one such field.
   *
   * @return the delegate field of the class currently being visited, or null
   */
  private @Nullable VariableTree currentDelegate() {
    return delegates.isEmpty() ? null : delegates.get(delegates.size() - 1);
  }

  @Override
  public Void visitClass(ClassTree tree, Void p) {
    if (checker.shouldSkipDefs(tree) || checker.shouldSkipFiles(tree)) {
      return null;
    }
    List<VariableTree> delegateFields = getDelegateFields(tree);
    if (delegateFields.size() > 1) {
      for (VariableTree delegateField : delegateFields) {
        checker.reportError(delegateField, "multiple.delegate.annotations");
      }
    }
    // A class with no delegate field is not a delegator, and a class with multiple delegate fields
    // has already been reported as erroneous.  In neither case is any further check performed on
    // the class's methods.
    VariableTree delegate = delegateFields.size() == 1 ? delegateFields.get(0) : null;
    if (delegate != null) {
      checkInheritedMethods(tree);
    }
    delegates.add(delegate);
    try {
      return super.visitClass(tree, p);
    } finally {
      delegates.remove(delegates.size() - 1);
    }
  }

  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    checkDelegatedCall(tree);
    checkInheritableByDelegator(tree);
    return super.visitMethod(tree, p);
  }

  /**
   * Issues a warning if the given delegator inherits a method that it must override: that is, a
   * method whose inherited implementation might read the delegator's own state rather than the
   * delegate's.
   *
   * @param tree a class that has a field annotated with {@link Delegate}
   */
  private void checkInheritedMethods(ClassTree tree) {
    TypeElement delegator = TreeUtils.elementFromDeclaration(tree);
    if (delegator == null) {
      return;
    }
    List<ExecutableElement> allMethods = ElementFilter.methodsIn(elements.getAllMembers(delegator));
    List<String> notOverridden = new ArrayList<>();
    for (ExecutableElement method : allMethods) {
      if (method.getEnclosingElement().equals(delegator)
          || delegatorNeedNotOverride(method)
          || isOverridden(method, allMethods, delegator)) {
        continue;
      }
      notOverridden.add(method.getEnclosingElement().getSimpleName() + "." + method);
    }
    if (!notOverridden.isEmpty()) {
      Collections.sort(notOverridden);
      checker.reportWarning(
          tree, "delegator.must.override", tree.getSimpleName(), String.join(", ", notOverridden));
    }
  }

  /**
   * Returns true if a delegator need not override the given method: that is, the method's
   * implementation is correct for a delegator, or a delegator cannot override the method.
   *
   * <p>A delegator can inherit a method that is annotated with {@link InheritableByDelegator}, a
   * default method of an interface (which cannot read instance fields), and a method of {@code
   * Object} (which has no fields). A delegator cannot override a method that is final or static,
   * and it need not override an abstract method, because the compiler requires a concrete delegator
   * to implement it.
   *
   * @param method a method that a delegator inherits
   * @return true if a delegator need not override the method
   */
  private boolean delegatorNeedNotOverride(ExecutableElement method) {
    Set<Modifier> modifiers = method.getModifiers();
    return modifiers.contains(Modifier.ABSTRACT)
        || modifiers.contains(Modifier.FINAL)
        || modifiers.contains(Modifier.STATIC)
        || modifiers.contains(Modifier.PRIVATE)
        || method.isDefault()
        || ElementUtils.isObject((TypeElement) method.getEnclosingElement())
        || ElementUtils.hasAnnotation(method, INHERITABLE_BY_DELEGATOR_NAME);
  }

  /**
   * Returns true if some other method in {@code allMethods} overrides the given method, in the
   * given class.
   *
   * @param method a method
   * @param allMethods all the methods of {@code clazz}, including inherited ones
   * @param clazz the class in which overriding is determined
   * @return true if some other method in {@code allMethods} overrides {@code method}
   */
  private boolean isOverridden(
      ExecutableElement method, List<ExecutableElement> allMethods, TypeElement clazz) {
    for (ExecutableElement other : allMethods) {
      if (!other.equals(method) && elements.overrides(other, method, clazz)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Issues a warning if the given method is annotated with {@link InheritableByDelegator} but its
   * body uses its receiver other than to call an overridable method or a method that is annotated
   * with {@link InheritableByDelegator}.
   *
   * <p>This method must be called while {@code getCurrentPath()} is the path to {@code tree}.
   *
   * @param tree a method declaration
   */
  private void checkInheritableByDelegator(MethodTree tree) {
    ExecutableElement methodElt = TreeUtils.elementFromDeclaration(tree);
    BlockTree body = tree.getBody();
    if (methodElt == null
        || body == null
        || !ElementUtils.hasAnnotation(methodElt, INHERITABLE_BY_DELEGATOR_NAME)) {
      return;
    }
    TypeElement receiverClass = (TypeElement) methodElt.getEnclosingElement();
    new ReceiverUseScanner(receiverClass, tree).scan(new TreePath(getCurrentPath(), body), null);
  }

  /**
   * Issues a warning if the given method, which is declared in a delegator, does not delegate.
   * Performs no check if the method's enclosing class is not a delegator or if the method does not
   * override a supertype's method.
   *
   * @param tree a method declaration
   */
  private void checkDelegatedCall(MethodTree tree) {
    VariableTree delegate = currentDelegate();
    BlockTree body = tree.getBody();
    if (delegate == null || body == null || !isOverride(tree)) {
      return;
    }
    // A delegator may refuse to support an operation rather than delegating it.
    if (throwsException(body, UnsupportedOperationException.class)) {
      return;
    }
    MethodInvocationTree candidateDelegateCall = getSoleMethodInvocation(body);
    if (candidateDelegateCall == null
        || !isValidDelegateCall(tree, candidateDelegateCall, delegate)) {
      checker.reportWarning(tree, "invalid.delegate", tree.getName(), delegate.getName());
    }
  }

  /**
   * Returns true if the given method declaration overrides or implements a method of a supertype.
   *
   * <p>This does not depend on the method being annotated with {@link Override}, which is optional.
   *
   * @param tree a method declaration
   * @return true if the method overrides or implements a method of a supertype
   */
  private boolean isOverride(MethodTree tree) {
    ExecutableElement methodElt = TreeUtils.elementFromDeclaration(tree);
    return methodElt != null && !ElementUtils.getOverriddenMethods(methodElt, types).isEmpty();
  }

  /**
   * Returns true if the given method call is a valid delegate call for the given enclosing method.
   *
   * <p>A delegate call must fulfill the following properties: its receiver is the given delegate
   * field, the method that it invokes is the enclosing method or is overridden by it, and its
   * arguments are the enclosing method's formal parameters, in order.
   *
   * @param enclosingMethod the method whose body is {@code delegatedMethodCall}
   * @param delegatedMethodCall the delegated method call
   * @param delegate the field annotated with {@link Delegate}
   * @return true if the given method call is a valid delegate call for the enclosing method
   */
  private boolean isValidDelegateCall(
      MethodTree enclosingMethod, MethodInvocationTree delegatedMethodCall, VariableTree delegate) {
    ExpressionTree methodSelectTree = delegatedMethodCall.getMethodSelect();
    if (!(methodSelectTree instanceof MemberSelectTree fieldAccessTree)) {
      return false;
    }
    VariableElement receiverField = TreeUtils.asFieldAccess(fieldAccessTree.getExpression());
    if (!Objects.equals(receiverField, TreeUtils.elementFromDeclaration(delegate))) {
      return false;
    }
    ExecutableElement enclosingMethodElt = TreeUtils.elementFromDeclaration(enclosingMethod);
    ExecutableElement delegatedMethodElt = TreeUtils.elementFromUse(delegatedMethodCall);
    return enclosingMethodElt != null
        && isSameMethod(enclosingMethodElt, delegatedMethodElt)
        && argumentsAreFormalParameters(enclosingMethod, delegatedMethodCall);
  }

  /**
   * Returns true if the two methods are the same method: that is, they are the same element, one
   * overrides the other, or both override some common method.
   *
   * <p>This is more permissive than {@link ElementUtils#isMethod}, which requires the first
   * method's declaring class to be a subtype of the second method's declaring class. That
   * requirement does not hold for a delegator that implements an interface but whose delegate field
   * is declared using a concrete implementation type.
   *
   * @param method1 a method
   * @param method2 a method
   * @return true if the two methods are the same method
   */
  private boolean isSameMethod(ExecutableElement method1, ExecutableElement method2) {
    if (method1.equals(method2)) {
      return true;
    }
    Set<ExecutableElement> method1Supers =
        new HashSet<>(ElementUtils.getOverriddenMethods(method1, types));
    if (method1Supers.contains(method2)) {
      return true;
    }
    for (ExecutableElement method2Super : ElementUtils.getOverriddenMethods(method2, types)) {
      if (method2Super.equals(method1) || method1Supers.contains(method2Super)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the arguments of the given method call are the formal parameters of the given
   * method declaration, in order.
   *
   * @param enclosingMethod a method declaration
   * @param delegatedMethodCall a method call that appears in the body of {@code enclosingMethod}
   * @return true if the call's arguments are the method's formal parameters, in order
   */
  private boolean argumentsAreFormalParameters(
      MethodTree enclosingMethod, MethodInvocationTree delegatedMethodCall) {
    List<? extends VariableTree> parameters = enclosingMethod.getParameters();
    List<? extends ExpressionTree> arguments = delegatedMethodCall.getArguments();
    if (parameters.size() != arguments.size()) {
      return false;
    }
    for (int i = 0; i < parameters.size(); i++) {
      VariableElement parameterElt = TreeUtils.elementFromDeclaration(parameters.get(i));
      Element argumentElt = TreeUtils.elementFromTree(TreeUtils.withoutParens(arguments.get(i)));
      if (parameterElt == null || !Objects.equals(parameterElt, argumentElt)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns all the fields of a class that have a {@link Delegate} annotation.
   *
   * @param tree a class
   * @return all the fields of a class that have a {@link Delegate} annotation
   */
  private List<VariableTree> getDelegateFields(ClassTree tree) {
    List<VariableTree> delegateFields = new ArrayList<>();
    for (VariableTree field : TreeUtils.fieldsFromClassTree(tree)) {
      List<AnnotationMirror> annosOnField =
          TreeUtils.annotationsFromTypeAnnotationTrees(field.getModifiers().getAnnotations());
      if (annosOnField.stream()
          .anyMatch(anno -> AnnotationUtils.areSameByName(anno, DELEGATE_NAME))) {
        delegateFields.add(field);
      }
    }
    return delegateFields;
  }

  /**
   * Returns the method call that constitutes the entirety of a method body: that is, the body
   * consists of exactly one statement, and that statement is a method call or is a return statement
   * whose expression is a method call. Otherwise, returns null.
   *
   * @param tree a method body
   * @return the method call that is the entirety of the method body, or null
   */
  private @Nullable MethodInvocationTree getSoleMethodInvocation(BlockTree tree) {
    List<? extends StatementTree> stmts = tree.getStatements();
    if (stmts.size() != 1) {
      return null;
    }
    StatementTree stmt = stmts.get(0);
    ExpressionTree soleExpression;
    if (stmt instanceof ExpressionStatementTree expressionStatement) {
      soleExpression = expressionStatement.getExpression();
    } else if (stmt instanceof ReturnTree returnStatement) {
      soleExpression = returnStatement.getExpression();
    } else {
      return null;
    }
    if (soleExpression instanceof MethodInvocationTree methodInvocation) {
      return methodInvocation;
    }
    return null;
  }

  /**
   * Returns true if the given method body consists of exactly one statement, which throws an
   * exception of the given class.
   *
   * @param tree a method body
   * @param clazz a class of exception (usually {@link UnsupportedOperationException})
   * @return true if the body's only statement throws an exception of the given class
   */
  private boolean throwsException(BlockTree tree, Class<?> clazz) {
    List<? extends StatementTree> stmts = tree.getStatements();
    if (stmts.size() != 1) {
      return false;
    }
    StatementTree soleStmt = stmts.get(0);
    if (!(soleStmt instanceof ThrowTree throwStmt)) {
      return false;
    }
    Class<?> exceptionClass =
        TypesUtils.getClassFromType(TreeUtils.typeOf(throwStmt.getExpression()));
    return exceptionClass == clazz;
  }

  /**
   * Reports each use, in a method that is annotated with {@link InheritableByDelegator}, of the
   * method's receiver that is not the receiver of a call to an overridable method or to a method
   * that is itself annotated with {@link InheritableByDelegator}. Such a use reads the receiver's
   * state other than through methods that a delegator forwards to its delegate. Examples include
   * reading a field, calling a private or final method, calling a method on {@code super}, and
   * passing {@code this} as an argument.
   */
  private class ReceiverUseScanner extends TreePathScanner<Void, Void> {

    /** The class that declares the method whose body is being scanned. */
    private final TypeElement receiverClass;

    /** The method whose body is being scanned; used for diagnostics. */
    private final MethodTree method;

    /**
     * Creates a ReceiverUseScanner.
     *
     * @param receiverClass the class that declares {@code method}
     * @param method a method whose body will be scanned; used for diagnostics
     */
    ReceiverUseScanner(TypeElement receiverClass, MethodTree method) {
      this.receiverClass = receiverClass;
      this.method = method;
    }

    /**
     * Issues a warning about the given use of the receiver.
     *
     * @param tree a use of the receiver
     */
    private void report(Tree tree) {
      checker.reportWarning(tree, "invalid.inheritable.by.delegator", method.getName());
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
      ExecutableElement callee = TreeUtils.elementFromUse(tree);
      ExpressionTree methodSelect = tree.getMethodSelect();
      boolean receiverIsThis;
      boolean isSuperCall;
      if (methodSelect instanceof IdentifierTree) {
        receiverIsThis = !ElementUtils.isStatic(callee) && isImplicitlyThis(callee);
        isSuperCall = false;
      } else if (methodSelect instanceof MemberSelectTree memberSelect) {
        ExpressionTree receiver = TreeUtils.withoutParens(memberSelect.getExpression());
        receiverIsThis = isThisOrSuper(receiver);
        isSuperCall = receiverIsThis && isSuper(receiver);
      } else {
        return super.visitMethodInvocation(tree, p);
      }
      if (!receiverIsThis) {
        return super.visitMethodInvocation(tree, p);
      }
      // A call on super is not dispatched dynamically, so it does not reach the delegator's
      // overriding implementation.
      boolean calleeIsOk =
          ElementUtils.hasAnnotation(callee, INHERITABLE_BY_DELEGATOR_NAME)
              || (!isSuperCall && isOverridable(callee));
      if (!calleeIsOk) {
        report(tree);
      }
      // Do not scan the receiver, which is a permitted use of `this`.
      scan(tree.getTypeArguments(), p);
      scan(tree.getArguments(), p);
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void p) {
      if (isThisOrSuper(tree)) {
        report(tree);
      } else {
        Element elt = TreeUtils.elementFromUse(tree);
        if (elt.getKind() == ElementKind.FIELD
            && !ElementUtils.isStatic(elt)
            && isImplicitlyThis(elt)) {
          report(tree);
        }
      }
      return super.visitIdentifier(tree, p);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, Void p) {
      if (isThisOrSuper(tree)) {
        report(tree);
        // Do not scan the class name that qualifies `this` or `super`.
        return null;
      }
      return super.visitMemberSelect(tree, p);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void p) {
      // Instantiating an inner class passes `this` as the new object's enclosing instance.
      if (tree.getEnclosingExpression() == null) {
        TypeMirror instantiated = TreeUtils.typeOf(tree.getIdentifier());
        if (instantiated instanceof DeclaredType declaredType) {
          TypeElement instantiatedClass = (TypeElement) declaredType.asElement();
          if (instantiatedClass.getNestingKind() == NestingKind.MEMBER
              && !ElementUtils.isStatic(instantiatedClass)
              && isImplicitlyThis(instantiatedClass)) {
            report(tree);
          }
        }
      }
      return super.visitNewClass(tree, p);
    }

    /**
     * Returns true if the given expression is {@code this} or {@code super}, possibly qualified by
     * a class name, and it refers to the receiver of the method whose body is being scanned. The
     * expression must be the leaf of the current path or a child of that leaf.
     *
     * @param tree an expression
     * @return true if the expression is {@code this} or {@code super} and refers to the receiver
     */
    private boolean isThisOrSuper(ExpressionTree tree) {
      if (tree instanceof IdentifierTree identifier) {
        String name = identifier.getName().toString();
        return (name.equals("this") || name.equals("super")) && innermostClassIsReceiverClass();
      } else if (tree instanceof MemberSelectTree memberSelect) {
        String name = memberSelect.getIdentifier().toString();
        if (!name.equals("this") && !name.equals("super")) {
          return false;
        }
        Element qualifier = TreeUtils.elementFromTree(memberSelect.getExpression());
        if (qualifier == null) {
          return false;
        }
        // In `Interface.super.m()`, the qualifier is a superinterface of the innermost class, and
        // the receiver is that class's instance.
        if (name.equals("super") && qualifier.getKind().isInterface()) {
          return innermostClassIsReceiverClass();
        }
        return qualifier.equals(receiverClass);
      } else {
        return false;
      }
    }

    /**
     * Returns true if the given expression, which satisfies {@link #isThisOrSuper}, is {@code
     * super} rather than {@code this}.
     *
     * @param tree {@code this} or {@code super}, possibly qualified by a class name
     * @return true if the expression is {@code super}
     */
    private boolean isSuper(ExpressionTree tree) {
      if (tree instanceof IdentifierTree identifier) {
        return identifier.getName().contentEquals("super");
      } else {
        return ((MemberSelectTree) tree).getIdentifier().contentEquals("super");
      }
    }

    /**
     * Returns true if the innermost class that encloses the current path is the class that declares
     * the method being scanned, so {@code this} refers to that method's receiver.
     *
     * @return true if {@code this} at the current path refers to the receiver
     */
    private boolean innermostClassIsReceiverClass() {
      ClassTree enclosing = TreePathUtil.enclosingClass(getCurrentPath());
      return enclosing != null && receiverClass.equals(TreeUtils.elementFromDeclaration(enclosing));
    }

    /**
     * Returns true if an unqualified use, at the current path, of the given instance member refers
     * to the member of the receiver of the method being scanned, rather than to the member of an
     * instance of some other enclosing class.
     *
     * @param member an instance member: a field, a method, or a member class
     * @return true if an unqualified use of {@code member} refers to the receiver's member
     */
    private boolean isImplicitlyThis(Element member) {
      TypeMirror owner = member.getEnclosingElement().asType();
      for (TreePath path = getCurrentPath(); path != null; path = path.getParentPath()) {
        if (path.getLeaf() instanceof ClassTree classTree) {
          TypeElement classElt = TreeUtils.elementFromDeclaration(classTree);
          if (classElt == null) {
            continue;
          }
          boolean hasMember = TypesUtils.isErasedSubtype(classElt.asType(), owner, types);
          if (classElt.equals(receiverClass)) {
            return hasMember;
          }
          if (hasMember) {
            // The member is that of a class nested within the receiver's class.
            return false;
          }
        }
      }
      return false;
    }
  }

  /**
   * Returns true if a subclass can override the given method, so a call to it is dispatched to the
   * subclass's implementation. A package-private method is treated as not overridable, because a
   * delegator in a different package cannot override it.
   *
   * @param method a method
   * @return true if a subclass in any package can override the method
   */
  private static boolean isOverridable(ExecutableElement method) {
    Set<Modifier> modifiers = method.getModifiers();
    return !modifiers.contains(Modifier.STATIC)
        && !modifiers.contains(Modifier.PRIVATE)
        && !modifiers.contains(Modifier.FINAL)
        && (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED))
        && !ElementUtils.isFinal(method.getEnclosingElement());
  }
}
