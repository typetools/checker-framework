package org.checkerframework.common.delegation;

import com.sun.source.tree.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.delegation.qual.Delegate;
import org.checkerframework.common.delegation.qual.DelegatorMustOverride;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * This class enforces checks for the {@link Delegate} annotation.
 *
 * <p>It is not a checker for a type system. It enforces the following syntactic checks:
 *
 * <ul>
 *   <li>A class may have up to exactly one field marked with the {@link Delegate} annotation.
 *   <li>An overridden method's implementation must be exactly a call to the delegate field.
 *   <li>A class overrides <i>all</i> methods declared in its superclass.
 * </ul>
 */
public class DelegationChecker extends BaseTypeChecker {

  @Override
  protected BaseTypeVisitor<BaseAnnotatedTypeFactory> createSourceVisitor() {
    return new Visitor(this);
  }

  static class Visitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

    /** The maximum number of fields marked with {@link Delegate} permitted in a class. */
    private final int MAX_NUM_DELEGATE_FIELDS = 1;

    /** The field marked with {@link Delegate} for the current class. */
    private @Nullable VariableTree delegate;

    public Visitor(DelegationChecker checker) {
      super(checker);
    }

    @Override
    @SuppressWarnings("UnusedVariable")
    public void processClassTree(ClassTree tree) {
      delegate = null; // Unset the previous delegate whenever a new class is visited
      // TODO: what about inner classes?
      List<VariableTree> delegates = getDelegateFields(tree);
      if (delegates.size() > MAX_NUM_DELEGATE_FIELDS) {
        VariableTree latestDelegate = delegates.get(delegates.size() - 1);
        checker.reportError(latestDelegate, "multiple.delegate.annotations");
      } else if (delegates.size() == 1) {
        delegate = delegates.get(0);
        checkSuperClassOverrides(tree);
      }
      // Do nothing if no delegate field is found
      super.processClassTree(tree);
    }

    @Override
    public void processMethodTree(MethodTree tree) {
      super.processMethodTree(tree);
      if (delegate == null || !isMarkedWithOverride(tree)) {
        return;
      }
      MethodInvocationTree candidateDelegateCall = getLastExpression(tree.getBody());
      boolean hasExceptionalExit =
          hasExceptionalExit(tree.getBody(), UnsupportedOperationException.class);
      if (hasExceptionalExit) {
        return;
      }
      if (candidateDelegateCall == null) {
        checker.reportWarning(tree, "invalid.delegate", tree.getName(), delegate.getName());
        return;
      }
      Name enclosingMethodName = tree.getName();
      if (!isValidDelegateCall(enclosingMethodName, candidateDelegateCall)) {
        checker.reportWarning(tree, "invalid.delegate", tree.getName(), delegate.getName());
      }
    }

    /**
     * Return true if the given method call is a valid delegate call for the enclosing method.
     *
     * <p>A delegate method call must fulfill the following properties: its receiver must be the
     * current field marked with {@link Delegate} in the class, and the name of the method call must
     * match that of the enclosing method.
     *
     * @param enclosingMethodName the name of the enclosing method
     * @param delegatedMethodCall the delegated method call
     * @return true if the given method call is a valid delegate call for the enclosing method
     */
    private boolean isValidDelegateCall(
        Name enclosingMethodName, MethodInvocationTree delegatedMethodCall) {
      assert delegate != null; // This method should only be invoked when delegate is non-null
      ExpressionTree methodSelectTree = delegatedMethodCall.getMethodSelect();
      MemberSelectTree fieldAccessTree = (MemberSelectTree) methodSelectTree;
      VariableElement delegatedField = TreeUtils.asFieldAccess(fieldAccessTree.getExpression());
      Name delegatedMethodName = TreeUtils.methodName(delegatedMethodCall);
      // TODO: is there a better way to check? Comparing names seems fragile.
      return enclosingMethodName.equals(delegatedMethodName)
          && delegatedField != null
          && delegatedField.getSimpleName().equals(delegate.getName());
    }

    /**
     * Returns the fields of a class marked with a {@link Delegate} annotation.
     *
     * @param tree a class
     * @return the fields of a class marked with a {@link Delegate} annotation
     */
    private List<VariableTree> getDelegateFields(ClassTree tree) {
      List<VariableTree> delegateFields = new ArrayList<>();
      for (VariableTree field : TreeUtils.fieldsFromTree(tree)) {
        List<AnnotationMirror> annosOnField =
            TreeUtils.annotationsFromTypeAnnotationTrees(field.getModifiers().getAnnotations());
        if (annosOnField.stream()
            .anyMatch(anno -> atypeFactory.areSameByClass(anno, Delegate.class))) {
          delegateFields.add(field);
        }
      }
      return delegateFields;
    }

    /**
     * Returns the last expression in a method body.
     *
     * <p>This method is used to identify a possible delegate method call. It will check whether a
     * method has only one statement (a method invocation or a return statement), and return the
     * expression that is associated with it. Otherwise, it will return null.
     *
     * @param tree the method body
     * @return the last expression in the method body
     */
    private @Nullable MethodInvocationTree getLastExpression(BlockTree tree) {
      List<? extends StatementTree> stmts = tree.getStatements();
      if (stmts.size() != 1) {
        return null;
      }
      StatementTree stmt = stmts.get(0);
      ExpressionTree lastExprInMethod = null;
      if (stmt instanceof ExpressionStatementTree) {
        lastExprInMethod = ((ExpressionStatementTree) stmt).getExpression();
      } else if (stmt instanceof ReturnTree) {
        lastExprInMethod = ((ReturnTree) stmt).getExpression();
      }
      if (!(lastExprInMethod instanceof MethodInvocationTree)) {
        return null;
      }
      return (MethodInvocationTree) lastExprInMethod;
    }

    /**
     * Return true if the last (and only) statement of the block throws an exception of the given
     * class.
     *
     * @param tree a block tree
     * @param clazz a class of exception (usually {@link UnsupportedOperationException})
     * @return true if the last and only statement throws an exception of the given class
     */
    private boolean hasExceptionalExit(BlockTree tree, Class<?> clazz) {
      List<? extends StatementTree> stmts = tree.getStatements();
      if (stmts.size() != 1) {
        return false;
      }
      StatementTree lastStmt = stmts.get(0);
      if (!(lastStmt instanceof ThrowTree)) {
        return false;
      }
      ThrowTree throwStmt = (ThrowTree) lastStmt;
      AnnotatedTypeMirror throwType = atypeFactory.getAnnotatedType(throwStmt.getExpression());
      Class<?> exceptionClass = TypesUtils.getClassFromType(throwType.getUnderlyingType());
      return exceptionClass.equals(clazz);
    }

    /**
     * Validate whether a class overrides all declared methods in its superclass.
     *
     * <p>This is a basic implementation that naively checks whether all the superclass methods have
     * been overridden by the subclass. It is unlikely in practice that a delegating subclass needs
     * to override <i>all</i> the methods in a superclass for postconditions to hold.
     *
     * @param tree a class tree
     */
    private void checkSuperClassOverrides(ClassTree tree) {
      TypeElement classTreeElt = TreeUtils.elementFromDeclaration(tree);
      if (classTreeElt == null || classTreeElt.getSuperclass() == null) {
        return;
      }
      DeclaredType superClassMirror = (DeclaredType) classTreeElt.getSuperclass();
      if (superClassMirror == null || superClassMirror.getKind() == TypeKind.NONE) {
        return;
      }
      Set<Name> overriddenMethods =
          getOverriddenMethods(tree).stream()
              .map(ExecutableElement::getSimpleName)
              .collect(Collectors.toSet());
      Set<ExecutableElement> methodsDeclaredInSuperClass =
          new HashSet<>(
              ElementFilter.methodsIn(superClassMirror.asElement().getEnclosedElements()));
      Set<Name> methodsThatMustBeOverriden =
          methodsDeclaredInSuperClass.stream()
              .filter(e -> atypeFactory.getDeclAnnotation(e, DelegatorMustOverride.class) != null)
              .map(ExecutableElement::getSimpleName)
              .collect(Collectors.toSet());

      // TODO: comparing a set of names isn't ideal, what about overloading?
      if (!overriddenMethods.containsAll(methodsThatMustBeOverriden)) {
        checker.reportWarning(
            tree,
            "delegate.override",
            tree.getSimpleName(),
            TypesUtils.getQualifiedName(superClassMirror));
      }
    }

    /**
     * Return a set of all methods in the class that are marked with {@link Override}.
     *
     * @param tree the class tree
     * @return a set of all methods in the class that are marked with {@link Override}
     */
    private Set<ExecutableElement> getOverriddenMethods(ClassTree tree) {
      Set<ExecutableElement> overriddenMethods = new HashSet<>();
      for (Tree member : tree.getMembers()) {
        if (!(member instanceof MethodTree)) {
          continue;
        }
        MethodTree method = (MethodTree) member;
        if (isMarkedWithOverride(method)) {
          overriddenMethods.add(TreeUtils.elementFromDeclaration(method));
        }
      }
      return overriddenMethods;
    }

    /**
     * Return true if a method is marked with {@link Override}.
     *
     * @param tree the method declaration
     * @return true if the given method declaration is annotated with {@link Override}
     */
    private boolean isMarkedWithOverride(MethodTree tree) {
      Element method = TreeUtils.elementFromDeclaration(tree);
      return atypeFactory.getDeclAnnotation(method, Override.class) != null;
    }
  }
}
