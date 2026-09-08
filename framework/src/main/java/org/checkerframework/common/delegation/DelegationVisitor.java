package org.checkerframework.common.delegation;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.delegation.qual.Delegate;
import org.checkerframework.common.delegation.qual.DelegatorMustOverride;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
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

  /** The checker that is using this visitor. */
  private final DelegationChecker checker;

  /** The processing environment. */
  private final ProcessingEnvironment processingEnv;

  /** Reads declaration annotations. */
  private final AnnotationProvider annotationProvider;

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
    this.processingEnv = checker.getProcessingEnvironment();
    this.annotationProvider = checker.getAnnotationProvider();
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
    delegates.add(delegate);
    try {
      if (delegate != null) {
        checkDelegatorMustOverrideMethods(tree);
      }
      return super.visitClass(tree, p);
    } finally {
      delegates.remove(delegates.size() - 1);
    }
  }

  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    checkMustOverrideIsOverridable(tree);
    checkDelegatedCall(tree);
    return super.visitMethod(tree, p);
  }

  /**
   * Issues an error if the given method is annotated with {@link DelegatorMustOverride} but cannot
   * be overridden. No delegator could satisfy such a requirement.
   *
   * @param tree a method declaration
   */
  private void checkMustOverrideIsOverridable(MethodTree tree) {
    ExecutableElement methodElt = TreeUtils.elementFromDeclaration(tree);
    if (annotationProvider.getDeclAnnotation(methodElt, DelegatorMustOverride.class) != null
        && !isOverridable(methodElt)) {
      checker.reportError(tree, "mustoverride.not.overridable", tree.getName());
    }
  }

  /**
   * Returns true if the given method can be overridden by a method in a subclass.
   *
   * @param methodElt a method
   * @return true if the given method can be overridden by a method in a subclass
   */
  private static boolean isOverridable(ExecutableElement methodElt) {
    Set<Modifier> modifiers = methodElt.getModifiers();
    return !modifiers.contains(Modifier.STATIC)
        && !modifiers.contains(Modifier.PRIVATE)
        && !modifiers.contains(Modifier.FINAL);
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
    return !ElementUtils.getOverriddenMethods(methodElt, types).isEmpty();
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
    return isSameMethod(enclosingMethodElt, delegatedMethodElt)
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
      if (!parameterElt.equals(argumentElt)) {
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
   * Issues a warning if the given class does not override every method of a supertype that is
   * annotated with {@link DelegatorMustOverride}.
   *
   * <p>A delegator need not override every method of a supertype: for many methods, the inherited
   * implementation is correct for the delegator as well. A method is annotated with {@link
   * DelegatorMustOverride} if its specification would not be satisfied by an inherited
   * implementation.
   *
   * @param tree a class that has a field annotated with {@link Delegate}
   */
  private void checkDelegatorMustOverrideMethods(ClassTree tree) {
    TypeElement classElt = TreeUtils.elementFromDeclaration(tree);
    if (classElt == null) {
      return;
    }
    List<ExecutableElement> declaredMethods =
        ElementFilter.methodsIn(classElt.getEnclosedElements());
    List<ExecutableElement> notOverridden = new ArrayList<>();
    for (TypeElement supertypeElt : ElementUtils.getAllSupertypes(classElt, processingEnv)) {
      if (supertypeElt.equals(classElt)) {
        continue;
      }
      for (ExecutableElement supertypeMethod :
          ElementFilter.methodsIn(supertypeElt.getEnclosedElements())) {
        if (annotationProvider.getDeclAnnotation(supertypeMethod, DelegatorMustOverride.class)
                == null
            || !isOverridable(supertypeMethod)) {
          continue;
        }
        boolean isOverridden =
            declaredMethods.stream()
                .anyMatch(m -> ElementUtils.isMethod(m, supertypeMethod, processingEnv));
        if (!isOverridden) {
          notOverridden.add(supertypeMethod);
        }
      }
    }
    // A supertype method and a method that overrides it may both be annotated with
    // @DelegatorMustOverride.  They are one method as far as the delegator is concerned, so list
    // only the most specific one.
    StringJoiner mostSpecific = new StringJoiner(", ");
    for (ExecutableElement method : notOverridden) {
      boolean hasMoreSpecific =
          notOverridden.stream()
              .anyMatch(m -> !m.equals(method) && ElementUtils.isMethod(m, method, processingEnv));
      if (!hasMoreSpecific) {
        mostSpecific.add(ElementUtils.getSimpleDescription(method));
      }
    }
    if (mostSpecific.length() != 0) {
      checker.reportWarning(
          tree, "delegate.override", tree.getSimpleName(), mostSpecific.toString());
    }
  }
}
