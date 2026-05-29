package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * An inference type for a method or constructor invocation. This is a wrapper around {@link
 * AnnotatedExecutableType} that returns {@link AbstractType}s for the types in the {@link
 * AnnotatedExecutableType}.
 */
public class AbstractInvocationType extends AbstractExecutableType {

  /** The {@code NewClassTree} or {@code MethodInvocationTree} whose type this is. */
  private final ExpressionTree invocation;

  /**
   * Creates an invocation type for a method or constructor invocation.
   *
   * @param annotatedExecutableType annotated method type
   * @param executableType the Java executable type
   * @param invocation a method or constructor invocation
   * @param context the context
   */
  public AbstractInvocationType(
      AnnotatedExecutableType annotatedExecutableType,
      ExecutableType executableType,
      ExpressionTree invocation,
      Java8InferenceContext context) {
    super(annotatedExecutableType, executableType, invocation, context);
    this.invocation = invocation;
    assert invocation instanceof MethodInvocationTree || invocation instanceof NewClassTree;
  }

  /**
   * Returns the return type of this.
   *
   * @param map a mapping from type variable to inference variable
   * @return the return type of this
   */
  @Override
  public AbstractType getReturnType(Theta map) {
    AnnotatedTypeMirror annotatedReturnType;
    TypeMirror returnType;

    if (TreeUtils.isDiamondTree(invocation)) {
      Element e = ElementUtils.enclosingTypeElement(TreeUtils.elementFromUse(invocation));
      annotatedReturnType = typeFactory.getAnnotatedType(e);
      returnType = e.asType();
    } else if (invocation instanceof MethodInvocationTree) {
      annotatedReturnType = annotatedExecutableType.getReturnType();
      returnType = executableType.getReturnType();
    } else {
      annotatedReturnType = typeFactory.getAnnotatedType(invocation);
      returnType = TreeUtils.typeOf(invocation);
    }

    if (map == null) {
      return new ProperType(annotatedReturnType, returnType, context);
    } else {
      return InferenceType.create(annotatedReturnType, returnType, map, context);
    }
  }

  @Override
  public List<AbstractType> getParameterTypes(Theta map, int size) {
    List<AnnotatedTypeMirror> params = new ArrayList<>(annotatedExecutableType.getParameterTypes());
    List<TypeMirror> paramsJava = new ArrayList<>(executableType.getParameterTypes());

    if (TreeUtils.isVarargsCall(invocation)) {
      AnnotatedTypeMirror eltType =
          ((AnnotatedArrayType) params.remove(params.size() - 1)).getComponentType();
      for (int i = params.size(); i < size; i++) {
        params.add(eltType);
      }
    }

    if (TreeUtils.isVarargsCall(invocation)) {
      TypeMirror eltType =
          ((ArrayType) paramsJava.remove(paramsJava.size() - 1)).getComponentType();
      for (int i = paramsJava.size(); i < size; i++) {
        paramsJava.add(eltType);
      }
    }

    return InferenceType.create(params, paramsJava, map, qualifierVars, context);
  }
}
