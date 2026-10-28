package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnionType;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

/** Util for checked exception constraints. */
public final class CheckedExceptionsUtil {

  /** Don't use. */
  private CheckedExceptionsUtil() {}

  /**
   * A checked exception that a functional expression can throw, viewed both as a {@link TypeMirror}
   * and as an {@link AnnotatedTypeMirror}. For a lambda, these are the checked exceptions that the
   * lambda body can throw; for a method reference, they are the checked exceptions in the throws
   * clause of the compile-time declaration.
   *
   * <p>For an exception thrown by a method invocation, {@code javaType} is the exception type as
   * declared by the invoked method, whereas {@code annotatedType} is the exception type of the
   * invocation, in which the method's type variables have been substituted.
   *
   * @param javaType the exception type
   * @param annotatedType the exception type, with annotations
   */
  public record ThrownCheckedException(TypeMirror javaType, AnnotatedTypeMirror annotatedType) {}

  /**
   * Returns the checked exception types that can be thrown by the lambda.
   *
   * @param lambda an expression
   * @param context inference context
   * @return the checked exception types that can be thrown by the lambda
   */
  public static List<ThrownCheckedException> thrownCheckedExceptions(
      LambdaExpressionTree lambda, Java8InferenceContext context) {
    // Scan the body rather than the lambda itself, because the visitor stops at a nested lambda.
    return nullToEmptyList(new CheckedExceptionVisitor(context).scan(lambda.getBody(), null));
  }

  /**
   * Helper class for gathering the types of checked exceptions that a lambda body can throw. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html#jls-11.2.2">JLS section
   * 11.2.2</a>.
   *
   * <p>Apply this visitor to the body of the lambda, not to the lambda itself. The visitor does not
   * descend into a nested lambda or class body, because an exception thrown there is attributed to
   * that construct rather than to the lambda being scanned.
   */
  private static final class CheckedExceptionVisitor
      extends TreeScanner<@Nullable List<ThrownCheckedException>, Void> {

    /** The context. */
    private final Java8InferenceContext context;

    /**
     * Creates the visitor.
     *
     * @param context the context
     */
    private CheckedExceptionVisitor(Java8InferenceContext context) {
      this.context = context;
    }

    @Override
    public List<ThrownCheckedException> reduce(
        List<ThrownCheckedException> r1, List<ThrownCheckedException> r2) {
      if (r1 == null) {
        return r2;
      }
      if (r2 == null) {
        return r1;
      }
      r1.addAll(r2);
      return r1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>An exception thrown by the body of a nested lambda is attributed to the nested lambda, so
     * this method returns null without scanning the nested lambda.
     */
    @Override
    public @Nullable List<ThrownCheckedException> visitLambdaExpression(
        LambdaExpressionTree node, Void aVoid) {
      return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>An exception thrown within a class body is attributed to the method, constructor, or
     * initializer that throws it, so this method returns null without scanning the class body.
     *
     * <p>JLS 11.2.1 does attribute to a class instance creation expression the exceptions that the
     * instance initializers of its anonymous class body throw. That does not matter here, because
     * javac's inference does not do so either: javac rejects a lambda whose only checked exception
     * comes from the instance initializer of an anonymous class, unless the type argument is given
     * explicitly.
     */
    @Override
    public @Nullable List<ThrownCheckedException> visitClass(ClassTree node, Void aVoid) {
      return null;
    }

    @Override
    public List<ThrownCheckedException> visitTry(TryTree node, Void aVoid) {
      List<ThrownCheckedException> results = scan(node.getBlock(), aVoid);
      if (results == null) {
        results = new ArrayList<>();
      }
      // The catch clauses catch exceptions thrown by the resource expressions, so collect those
      // before removing the caught exceptions.
      results.addAll(nullToEmptyList(scan(node.getResources(), aVoid)));

      if (!results.isEmpty()) {
        for (CatchTree catchTree : node.getCatches()) {
          // Remove any type that would be caught.
          removeAssignable(TreeUtils.typeOf(catchTree.getParameter()), results);
        }
      }
      // The catch clauses do not catch exceptions thrown by the catch blocks or the finally block.
      results.addAll(nullToEmptyList(scan(node.getCatches(), aVoid)));
      results.addAll(nullToEmptyList(scan(node.getFinallyBlock(), aVoid)));

      return results;
    }

    /**
     * If any exception in {@code thrownExceptions} is assignable to {@code type}, then remove it
     * from the list.
     *
     * @param type an exception type
     * @param thrownExceptions the thrown exceptions; side-effected by this method
     */
    private void removeAssignable(TypeMirror type, List<ThrownCheckedException> thrownExceptions) {
      if (thrownExceptions.isEmpty()) {
        return;
      }
      if (type.getKind() == TypeKind.UNION) {
        for (TypeMirror altern : ((UnionType) type).getAlternatives()) {
          removeAssignable(altern, thrownExceptions);
        }
      } else {
        thrownExceptions.removeIf(
            thrown -> context.env.getTypeUtils().isAssignable(thrown.javaType(), type));
      }
    }

    @Override
    public List<ThrownCheckedException> visitThrow(ThrowTree node, Void aVoid) {
      List<ThrownCheckedException> result = super.visitThrow(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      TypeMirror type = TreeUtils.typeOf(node.getExpression());
      if (isCheckedException(type, context)) {
        result.add(
            new ThrownCheckedException(
                type, context.typeFactory.getAnnotatedType(node.getExpression())));
      }
      return result;
    }

    @Override
    public List<ThrownCheckedException> visitMethodInvocation(
        MethodInvocationTree node, Void aVoid) {
      List<ThrownCheckedException> result = super.visitMethodInvocation(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      AnnotatedExecutableType method = context.typeFactory.methodFromUse(node).executableType();
      addCheckedExceptions(TreeUtils.elementFromUse(node).getThrownTypes(), method, result);
      return result;
    }

    @Override
    public List<ThrownCheckedException> visitNewClass(NewClassTree node, Void aVoid) {
      List<ThrownCheckedException> result = super.visitNewClass(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      AnnotatedExecutableType constructor =
          context.typeFactory.constructorFromUse(node).executableType();
      addCheckedExceptions(TreeUtils.elementFromUse(node).getThrownTypes(), constructor, result);
      return result;
    }

    /**
     * Adds to {@code result} each of {@code declaredThrownTypes} that is a checked exception,
     * paired with the corresponding thrown type of {@code invokedType}.
     *
     * @param declaredThrownTypes the thrown types as declared by the invoked method or constructor
     * @param invokedType the type of the invoked method or constructor
     * @param result the list to add to; side-effected by this method
     */
    private void addCheckedExceptions(
        List<? extends TypeMirror> declaredThrownTypes,
        AnnotatedExecutableType invokedType,
        List<ThrownCheckedException> result) {
      Iterator<AnnotatedTypeMirror> annotatedThrownTypes = invokedType.getThrownTypes().iterator();
      for (TypeMirror declaredThrownType : declaredThrownTypes) {
        AnnotatedTypeMirror annotatedThrownType = annotatedThrownTypes.next();
        if (isCheckedException(declaredThrownType, context)) {
          result.add(new ThrownCheckedException(declaredThrownType, annotatedThrownType));
        }
      }
    }
  }

  /**
   * Returns true iff {@code type} is a checked exception.
   *
   * @param type an exception type to check (that is, Throwable or a subtype of it)
   * @param context the context
   * @return true iff {@code type} is a checked exception
   */
  public static boolean isCheckedException(TypeMirror type, Java8InferenceContext context) {
    Types types = context.env.getTypeUtils();
    return !types.isSubtype(type, context.runtimeException)
        && !types.isSubtype(type, context.error);
  }

  /**
   * Returns {@code list}, or an empty list if {@code list} is null.
   *
   * @param list a possibly-null list
   * @param <T> the element type of {@code list}
   * @return {@code list}, or an empty list if {@code list} is null
   */
  private static <T> List<T> nullToEmptyList(@Nullable List<T> list) {
    return list != null ? list : Collections.emptyList();
  }
}
