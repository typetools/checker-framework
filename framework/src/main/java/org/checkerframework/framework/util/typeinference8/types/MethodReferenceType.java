package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;

/** A type for a method reference */
public class MethodReferenceType extends InvocationType {

  /**
   * Type of the receiver. This may be different than {@code annotatedExecutableType.getReceiver()}
   */
  AnnotatedTypeMirror receiver;

  /** The method reference tree. */
  MemberReferenceTree methodRef;

  /**
   * Creates an invocation type for a method reference.
   *
   * @param annotatedExecutableType annotated method type
   * @param methodType java method type
   * @param methodRef a method reference
   * @param receiver the type of the receiver for this method reference
   * @param context the context
   */
  public MethodReferenceType(
      AnnotatedExecutableType annotatedExecutableType,
      ExecutableType methodType,
      MemberReferenceTree methodRef,
      AnnotatedTypeMirror receiver,
      Java8InferenceContext context) {
    super(annotatedExecutableType, methodType, methodRef, context);
    this.receiver = receiver;
    this.methodRef = methodRef;
  }

  @Override
  public List<AbstractType> getParameterTypes(Theta map, int size) {
    List<AnnotatedTypeMirror> params = new ArrayList<>(annotatedExecutableType.getParameterTypes());
    List<TypeMirror> paramsJava = new ArrayList<>(methodType.getParameterTypes());

    if (MemberReferenceKind.getMemberReferenceKind(methodRef).isUnbound()) {
      params.add(0, receiver);
      paramsJava.add(0, receiver.getUnderlyingType());
    }

    if (TreeUtils.isVarargsCall(methodRef)) {
      AnnotatedArrayType vararg = (AnnotatedArrayType) params.remove(params.size() - 1);
      for (int i = params.size(); i < size; i++) {
        params.add(vararg.getComponentType());
      }
      ArrayType varargTM = (ArrayType) paramsJava.remove(paramsJava.size() - 1);
      for (int i = paramsJava.size(); i < size; i++) {
        paramsJava.add(varargTM.getComponentType());
      }
    }

    return InferenceType.create(params, paramsJava, map, qualifierVars, context);
  }

  @Override
  public AbstractType getReturnType(Theta map) {
    TypeMirror returnTypeJava;
    AnnotatedTypeMirror returnType;

    if (invocation instanceof MemberReferenceTree mrt && mrt.getMode() == ReferenceMode.NEW) {
      returnType =
          context.typeFactory.getResultingTypeOfConstructorMemberReference(
              mrt, annotatedExecutableType);
      returnTypeJava = returnType.getUnderlyingType();
    } else {
      returnTypeJava = methodType.getReturnType();
      returnType = annotatedExecutableType.getReturnType();
    }

    if (map == null) {
      return new ProperType(returnType, returnTypeJava, context);
    }
    return InferenceType.create(returnType, returnTypeJava, map, context);
  }
}
