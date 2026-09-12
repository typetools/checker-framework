package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import java.util.List;
import javax.lang.model.type.ExecutableType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;

/**
 * Represents the type of the compile-time declaration of the method reference. The compile-time
 * declaration is the actual method referenced by the method reference. See <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.13.1">JLS section
 * 15.13.1</a> for a complete definition.
 *
 * <p>The type of a member reference is a functional interface. The function type of a member
 * reference is the type of the single abstract method declared by the functional interface.
 *
 * <p>For example,
 *
 * <pre>{@code
 * class MyClass {
 *    public int compareByField(MyClass other) { ... }
 *  }
 *  Comparator<MyClass> func = MyClass::compareByField;
 * }</pre>
 *
 * <p>The function type is {@code int compare(Comparator<MyClass> this, MyClass o1, MyClass o2)}
 * whereas the type of the compile-time declaration is {@code int compareByField(MyClass this,
 * MyClass other)}.
 */
public class CompileTimeDeclarationType extends AbstractExecutableType {

  /**
   * The type of the receiver. Its value may be different than {@code
   * this.annotatedExecutableType.getReceiver()}.
   */
  private final AnnotatedTypeMirror receiver;

  /** The method reference tree. */
  private final MemberReferenceTree methodRef;

  /**
   * Creates a compile-time declaration type for a method reference.
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
  public AbstractType getReturnType(@Nullable Theta map) {
    AnnotatedTypeMirror annotatedReturnType;

    if (methodRef.getMode() == ReferenceMode.NEW) {
      annotatedReturnType =
          context.typeFactory.getResultingTypeOfConstructorMemberReference(
              methodRef, annotatedExecutableType);
    } else {
      annotatedReturnType = annotatedExecutableType.getReturnType();
    }

    if (map == null) {
      return new ProperType(annotatedReturnType, context);
    } else {
      return InferenceType.create(annotatedReturnType, map, context);
    }
  }

  @Override
  public List<AbstractType> getParameterTypes(@Nullable Theta map, int size) {
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
