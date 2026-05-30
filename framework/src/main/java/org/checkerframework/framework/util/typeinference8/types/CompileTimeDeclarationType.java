package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import java.util.List;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;

/**
 * Represents the compile-time declaration type of the method reference that is the method to which
 * the expression refers. See <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.13.1">JLS section
 * 15.13.1</a> for a complete definition.
 *
 * <p>The type of a member reference is a functional interface. The function type of a member
 * reference is the type of the single abstract method declared by the functional interface. The
 * compile-time declaration type is the type of the actual method referenced by the method
 * reference.
 *
 * <p>For example,
 *
 * <pre>{@code
 * static class MyClass {
 *   String field;
 *   public static int compareByField(MyClass a, MyClass b) { ... }
 * }
 * Comparator<MyClass> func = MyClass::compareByField;
 * }</pre>
 *
 * <p>The function type is {@code compare(Comparator<MyClass> this, MyClass o1, MyClass o2)} whereas
 * the compile-time declaration type is {@code compareByField(MyClass a, MyClass b)}.
 */
public class CompileTimeDeclarationType extends InferenceExecutableType {

  /**
   * The type of the receiver. Its value may be different than {@code
   * this.annotatedExecutableType.getReceiver()}.
   */
  AnnotatedTypeMirror receiver;

  /** The method reference tree. */
  MemberReferenceTree methodRef;

  /**
   * Creates an invocation type for a method reference.
   *
   * @param annotatedExecutableType annotated method or constructor type
   * @param executableType a Java method or constructor type
   * @param methodRef a method reference
   * @param receiver the type of the receiver for this method reference
   * @param context the context
   */
  public CompileTimeDeclarationType(
      AnnotatedExecutableType annotatedExecutableType,
      ExecutableType executableType,
      MemberReferenceTree methodRef,
      AnnotatedTypeMirror receiver,
      Java8InferenceContext context) {
    super(annotatedExecutableType, executableType, methodRef, context);
    this.receiver = receiver;
    this.methodRef = methodRef;
  }

  /**
   * Returns the method reference for which this is a compile-time declaration.
   *
   * @return the method reference for which this is a compile-time declaration
   */
  public MemberReferenceTree getMethodRef() {
    return methodRef;
  }

  @Override
  public AbstractType getReturnType(Theta map) {
    AnnotatedTypeMirror annotatedReturnType;
    TypeMirror returnType;

    if (methodRef.getMode() == ReferenceMode.NEW) {
      annotatedReturnType =
          context.typeFactory.getResultingTypeOfConstructorMemberReference(
              methodRef, annotatedExecutableType);
      returnType = annotatedReturnType.getUnderlyingType();
    } else {
      annotatedReturnType = annotatedExecutableType.getReturnType();
      returnType = executableType.getReturnType();
    }

    if (map == null) {
      return new ProperType(annotatedReturnType, returnType, context);
    } else {
      return InferenceType.create(annotatedReturnType, returnType, map, context);
    }
  }

  @Override
  public List<AbstractType> getParameterTypes(Theta map, int size) {
    AnnotatedTypeMirror receiverTM;
    if (MemberReferenceKind.getMemberReferenceKind(methodRef).isUnbound()) {
      // For unbound method references, i.e. Type::instanceMethod, the receiver is treated as the
      // first parameter.
      receiverTM = receiver;
    } else {
      receiverTM = null;
    }

    return getParameterTypes(map, size, receiverTM, TreeUtils.isVarargsCall(methodRef));
  }
}
