package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

public class CheckedExceptionsUtil {

  /**
   * Returns a list of checked exception types that can be thrown by the lambda.
   *
   * @param lambda an expression
   * @param context inference context
   * @return a list of types of checked exceptions that can be thrown by the lambda
   */
  public static List<TypeMirror> thrownCheckedExceptions(
      LambdaExpressionTree lambda, Java8InferenceContext context) {
    return new CheckedExceptionVisitor(context).scan(lambda, null);
  }

  /**
   * Helper class for gathering the types of checked exceptions in a lambda. See
   * https://docs.oracle.com/javase/specs/jls/se9/html/jls-11.html#jls-11.2.2
   */
  private static class CheckedExceptionVisitor extends TreeScanner<List<TypeMirror>, Void> {

    private final Java8InferenceContext context;

    private CheckedExceptionVisitor(Java8InferenceContext context) {
      this.context = context;
    }

    @Override
    public List<TypeMirror> reduce(List<TypeMirror> r1, List<TypeMirror> r2) {
      if (r1 == null) {
        return r2;
      }
      if (r2 == null) {
        return r1;
      }
      r1.addAll(r2);
      return r1;
    }

    @Override
    public List<TypeMirror> visitThrow(ThrowTree node, Void aVoid) {
      List<TypeMirror> result = super.visitThrow(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      TypeMirror type = TreeUtils.typeOf(node);
      if (isCheckedException(type, context)) {
        result.add(type);
      }
      return result;
    }

    @Override
    public List<TypeMirror> visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      List<TypeMirror> result = super.visitMethodInvocation(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      for (TypeMirror type : TreeUtils.elementFromUse(node).getThrownTypes()) {
        if (isCheckedException(type, context)) {
          result.add(type);
        }
      }
      return result;
    }

    @Override
    public List<TypeMirror> visitNewClass(NewClassTree node, Void aVoid) {
      List<TypeMirror> result = super.visitNewClass(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      for (TypeMirror type : TreeUtils.elementFromUse(node).getThrownTypes()) {
        if (isCheckedException(type, context)) {
          result.add(type);
        }
      }
      return result;
    }

    @Override
    public List<TypeMirror> visitTry(TryTree node, Void aVoid) {
      List<TypeMirror> results = scan(node.getBlock(), aVoid);
      if (results == null) {
        results = new ArrayList<>();
      }

      if (!results.isEmpty()) {
        for (CatchTree catchTree : node.getCatches()) {
          // Remove any type that would be caught.
          removeAssignable(TreeUtils.typeOf(catchTree.getParameter()), results);
        }
      }
      results.addAll(scan(node.getResources(), aVoid));
      results.addAll(scan(node.getCatches(), aVoid));
      results.addAll(scan(node.getFinallyBlock(), aVoid));

      return results;
    }

    /**
     * If any type in {@code thrownExceptionTypes} is assignable to {@code type}, then remove it
     * from the list.
     */
    private void removeAssignable(TypeMirror type, List<TypeMirror> thrownExceptionTypes) {
      if (thrownExceptionTypes.isEmpty()) {
        return;
      }
      if (type.getKind() == TypeKind.UNION) {
        for (TypeMirror altern : ((UnionType) type).getAlternatives()) {
          removeAssignable(altern, thrownExceptionTypes);
        }
      } else {
        for (TypeMirror thrownType : new ArrayList<>(thrownExceptionTypes)) {
          if (context.env.getTypeUtils().isAssignable(thrownType, type)) {
            thrownExceptionTypes.remove(thrownType);
          }
        }
      }
    }
  }

  /** Returns true iff {@code type} is a checked exception. */
  private static boolean isCheckedException(TypeMirror type, Java8InferenceContext context) {
    TypeMirror runtimeEx = context.runtimeEx;
    return context.env.getTypeUtils().isSubtype(type, runtimeEx);
  }

  /**
   * Returns a list of checked exception types that can be thrown by the lambda.
   *
   * @param lambda an expression
   * @param context inference context
   * @return a list of types of checked exceptions that can be thrown by the lambda
   */
  public static List<AnnotatedTypeMirror> thrownCheckedExceptionsATM(
      LambdaExpressionTree lambda, Java8InferenceContext context) {
    return new CheckedExceptionATMVisitor(context).scan(lambda, null);
  }

  /**
   * Helper class for gathering the types of checked exceptions in a lambda. See
   * https://docs.oracle.com/javase/specs/jls/se9/html/jls-11.html#jls-11.2.2
   */
  private static class CheckedExceptionATMVisitor
      extends TreeScanner<List<AnnotatedTypeMirror>, Void> {

    private final Java8InferenceContext context;

    private CheckedExceptionATMVisitor(Java8InferenceContext context) {
      this.context = context;
    }

    @Override
    public List<AnnotatedTypeMirror> reduce(
        List<AnnotatedTypeMirror> r1, List<AnnotatedTypeMirror> r2) {
      if (r1 == null) {
        return r2;
      }
      if (r2 == null) {
        return r1;
      }
      r1.addAll(r2);
      return r1;
    }

    @Override
    public List<AnnotatedTypeMirror> visitThrow(ThrowTree node, Void aVoid) {
      List<AnnotatedTypeMirror> result = super.visitThrow(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      AnnotatedTypeMirror type = context.typeFactory.getAnnotatedType(node);
      if (isCheckedException(type, context)) {
        result.add(type);
      }
      return result;
    }

    @Override
    public List<AnnotatedTypeMirror> visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      List<AnnotatedTypeMirror> result = super.visitMethodInvocation(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      AnnotatedExecutableType method = context.typeFactory.methodFromUse(node).executableType;
      for (AnnotatedTypeMirror type : method.getThrownTypes()) {
        if (isCheckedException(type, context)) {
          result.add(type);
        }
      }
      return result;
    }

    @Override
    public List<AnnotatedTypeMirror> visitNewClass(NewClassTree node, Void aVoid) {
      List<AnnotatedTypeMirror> result = super.visitNewClass(node, aVoid);
      if (result == null) {
        result = new ArrayList<>();
      }
      AnnotatedExecutableType method = context.typeFactory.constructorFromUse(node).executableType;

      for (AnnotatedTypeMirror type : method.getThrownTypes()) {
        if (isCheckedException(type, context)) {
          result.add(type);
        }
      }
      return result;
    }

    @Override
    public List<AnnotatedTypeMirror> visitTry(TryTree node, Void aVoid) {
      List<AnnotatedTypeMirror> results = scan(node.getBlock(), aVoid);
      if (results == null) {
        results = new ArrayList<>();
      }

      if (!results.isEmpty()) {
        for (CatchTree catchTree : node.getCatches()) {
          // Remove any type that would be caught.
          removeAssignable(TreeUtils.typeOf(catchTree.getParameter()), results);
        }
      }
      results.addAll(scan(node.getResources(), aVoid));
      results.addAll(scan(node.getCatches(), aVoid));
      results.addAll(scan(node.getFinallyBlock(), aVoid));

      return results;
    }

    /**
     * If any type in {@code thrownExceptionTypes} is assignable to {@code type}, then remove it
     * from the list.
     */
    private void removeAssignable(TypeMirror type, List<AnnotatedTypeMirror> thrownExceptionTypes) {
      if (thrownExceptionTypes.isEmpty()) {
        return;
      }
      if (type.getKind() == TypeKind.UNION) {
        for (TypeMirror altern : ((UnionType) type).getAlternatives()) {
          removeAssignable(altern, thrownExceptionTypes);
        }
      } else {
        for (AnnotatedTypeMirror thrownType : new ArrayList<>(thrownExceptionTypes)) {
          if (context.env.getTypeUtils().isAssignable(thrownType.getUnderlyingType(), type)) {
            thrownExceptionTypes.remove(thrownType);
          }
        }
      }
    }
  }

  /** Returns true iff {@code type} is a checked exception. */
  private static boolean isCheckedException(
      AnnotatedTypeMirror type, Java8InferenceContext context) {
    TypeMirror runtimeEx = context.runtimeEx;
    return context.env.getTypeUtils().isSubtype(type.getUnderlyingType(), runtimeEx);
  }
}
