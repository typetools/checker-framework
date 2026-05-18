package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import javax.lang.model.element.Element;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/** A method type for an invocation of a method or constructor. */
public class MethodType extends InvocationType {

  /**
   * Creates an invocation type for a method or constructor invocation.
   *
   * @param annotatedExecutableType annotated method type
   * @param methodType java method type
   * @param invocation a method or constructor invocation
   * @param context the context
   */
  public MethodType(
      AnnotatedExecutableType annotatedExecutableType,
      ExecutableType methodType,
      ExpressionTree invocation,
      Java8InferenceContext context) {
    super(annotatedExecutableType, methodType, invocation, context);
    assert invocation instanceof MethodInvocationTree || invocation instanceof NewClassTree;
  }

  /**
   * Returns the return type.
   *
   * @param map a mapping from type variable to inference variable
   * @return the return type
   */
  @Override
  public AbstractType getReturnType(Theta map) {
    TypeMirror returnTypeJava;
    AnnotatedTypeMirror returnType;

    if (TreeUtils.isDiamondTree(invocation)) {
      Element e = ElementUtils.enclosingTypeElement(TreeUtils.elementFromUse(invocation));
      returnTypeJava = e.asType();
      returnType = typeFactory.getAnnotatedType(e);
    } else if (invocation instanceof MethodInvocationTree) {
      returnTypeJava = methodType.getReturnType();
      returnType = annotatedExecutableType.getReturnType();
    } else {
      returnTypeJava = TreeUtils.typeOf(invocation);
      returnType = typeFactory.getAnnotatedType(invocation);
    }

    if (map == null) {
      return new ProperType(returnType, returnTypeJava, context);
    }
    return InferenceType.create(returnType, returnTypeJava, map, context);
  }
}
