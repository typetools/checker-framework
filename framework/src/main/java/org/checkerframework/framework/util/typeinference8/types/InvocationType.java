package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;

/** A method type for an invocation of a method or constructor. */
public class InvocationType {

  /** A method or constructor invocation. */
  private final ExpressionTree invocation;

  /** The annotated method type. */
  private final AnnotatedExecutableType annotatedExecutableType;

  /** The Java method type. */
  private final ExecutableType methodType;

  /** The context. */
  private final Java8InferenceContext context;

  /** The annotated type factory. */
  private final AnnotatedTypeFactory typeFactory;

  /** A mapping from polymorphic annotation to {@link QualifierVar}. */
  private final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Creates an invocation type.
   *
   * @param annotatedExecutableType annotated method type
   * @param methodType java method type
   * @param invocation a method or constructor invocation
   * @param context the context
   */
  public InvocationType(
      AnnotatedExecutableType annotatedExecutableType,
      ExecutableType methodType,
      ExpressionTree invocation,
      Java8InferenceContext context) {
    assert annotatedExecutableType != null && methodType != null;
    this.annotatedExecutableType = annotatedExecutableType;
    this.methodType = methodType;
    this.invocation = invocation;
    this.context = context;
    this.typeFactory = context.typeFactory;

    SimpleAnnotatedTypeScanner<Void, Set<AnnotationMirror>> s =
        new SimpleAnnotatedTypeScanner<>(
            (type, polys) -> {
              for (AnnotationMirror a : type.getPrimaryAnnotations()) {
                if (typeFactory.getQualifierHierarchy().isPolymorphicQualifier(a)) {
                  polys.add(a);
                }
              }
              return null;
            });
    Set<AnnotationMirror> polys = new AnnotationMirrorSet();
    s.visit(annotatedExecutableType, polys);
    AnnotationMirrorMap<QualifierVar> qualifierVars = new AnnotationMirrorMap<>();
    for (AnnotationMirror poly : polys) {
      qualifierVars.put(poly, new QualifierVar(invocation, poly, context));
    }
    this.qualifierVars = qualifierVars;
  }

  /**
   * Returns the method or constructor invocation.
   *
   * @return the method or constructor invocation
   */
  public ExpressionTree getInvocation() {
    return invocation;
  }

  /**
   * Returns the java method type.
   *
   * @return the java method type
   */
  public ExecutableType getJavaType() {
    return annotatedExecutableType.getUnderlyingType();
  }

  /**
   * Returns the thrown types.
   *
   * @param map a mapping from type variable to inference variable
   * @return the thrown types
   */
  public List<? extends AbstractType> getThrownTypes(Theta map) {
    List<AbstractType> thrown = new ArrayList<>();
    Iterator<? extends TypeMirror> iter = methodType.getThrownTypes().iterator();
    for (AnnotatedTypeMirror t : annotatedExecutableType.getThrownTypes()) {
      thrown.add(InferenceType.create(t, iter.next(), map, context));
    }
    return thrown;
  }

  /**
   * Returns the return type.
   *
   * @param map a mapping from type variable to inference variable
   * @return the return type
   */
  public AbstractType getReturnType(Theta map) {
    TypeMirror returnTypeJava;
    AnnotatedTypeMirror returnType;

    if (TreeUtils.isDiamondTree(invocation)) {
      Element e = ElementUtils.enclosingTypeElement(TreeUtils.elementFromUse(invocation));
      returnTypeJava = e.asType();
      returnType = typeFactory.getAnnotatedType(e);
    } else if (invocation.getKind() == Tree.Kind.METHOD_INVOCATION
        || invocation.getKind() == Tree.Kind.MEMBER_REFERENCE) {
      if (invocation.getKind() == Kind.MEMBER_REFERENCE
          && ((MemberReferenceTree) invocation).getMode() == ReferenceMode.NEW) {
        returnType =
            context.typeFactory.getResultingTypeOfConstructorMemberReference(
                (MemberReferenceTree) invocation, annotatedExecutableType);
        returnTypeJava = returnType.getUnderlyingType();
      } else {
        returnTypeJava = methodType.getReturnType();
        returnType = annotatedExecutableType.getReturnType();
      }

    } else {
      returnTypeJava = TreeUtils.typeOf(invocation);
      returnType = typeFactory.getAnnotatedType(invocation);
    }

    if (map == null) {
      return new ProperType(returnType, returnTypeJava, context);
    }
    return InferenceType.create(returnType, returnTypeJava, map, context);
  }

  /**
   * Returns a list of the parameter types of {@code InvocationType} where the vararg parameter has
   * been modified to match the arguments in {@code expression}.
   *
   * @param map a mapping from type variable to inference variable
   * @param size the number of parameters to return; used to expand the vararg
   * @return a list of the parameter types of {@code InvocationType} where the vararg parameter has
   *     been modified to match the arguments in {@code expression}
   */
  public List<AbstractType> getParameterTypes(Theta map, int size) {
    List<AnnotatedTypeMirror> params = new ArrayList<>(annotatedExecutableType.getParameterTypes());

    if (TreeUtils.isVarargsCall(invocation)) {
      AnnotatedArrayType vararg = (AnnotatedArrayType) params.remove(params.size() - 1);
      for (int i = params.size(); i < size; i++) {
        params.add(vararg.getComponentType());
      }
    }

    List<TypeMirror> paramsJava = new ArrayList<>(methodType.getParameterTypes());

    if (TreeUtils.isVarargsCall(invocation)) {
      ArrayType vararg = (ArrayType) paramsJava.remove(paramsJava.size() - 1);
      for (int i = paramsJava.size(); i < size; i++) {
        paramsJava.add(vararg.getComponentType());
      }
    }
    if (invocation.getKind() == Kind.MEMBER_REFERENCE
        && MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) invocation)
            .isUnbound()) {
      params.add(0, annotatedExecutableType.getReceiverType());
      paramsJava.add(0, annotatedExecutableType.getReceiverType().getUnderlyingType());
    }
    return InferenceType.create(params, paramsJava, map, qualifierVars, context);
  }

  /**
   * Returns the parameter types. (Varags are not expanded.)
   *
   * @param map a mapping from type variable to inference variable
   * @return the parameter types
   */
  public List<AbstractType> getParameterTypes(Theta map) {
    return getParameterTypes(map, annotatedExecutableType.getParameterTypes().size());
  }

  /**
   * Whether this method has type variables.
   *
   * @return whether this method has type variables
   */
  public boolean hasTypeVariables() {
    return !annotatedExecutableType.getTypeVariables().isEmpty();
  }

  /**
   * Returns the annotated type variables.
   *
   * @return the annotated type variables
   */
  public List<? extends AnnotatedTypeVariable> getAnnotatedTypeVariables() {
    return annotatedExecutableType.getTypeVariables();
  }

  /**
   * Returns the type variables.
   *
   * @return the type variables
   */
  public List<? extends TypeVariable> getTypeVariables() {
    return methodType.getTypeVariables();
  }

  /**
   * Whether this method is void.
   *
   * @return whether this method is void
   */
  public boolean isVoid() {
    return annotatedExecutableType.getReturnType().getKind() == TypeKind.VOID;
  }

  /**
   * Returns the annotated method type.
   *
   * @return the annotated method type
   */
  public AnnotatedExecutableType getAnnotatedType() {
    return annotatedExecutableType;
  }
}
