package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.ExecutableType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * An inference type for a method or constructor invocation. This is a wrapper around {@link
 * AnnotatedExecutableType} whose methods return {@link AbstractType}.
 */
public class AbstractInvocationType extends AbstractExecutableType {

  /** The {@code NewClassTree} or {@code MethodInvocationTree} whose type this is. */
  private final ExpressionTree invocation;

  /**
   * Creates an invocation type for a method or constructor invocation.
   *
   * @param annotatedExecutableType annotated method or constructor type
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
   * @param map a mapping from type variable to inference variable, or null to treat no type
   *     variable as an inference variable
   * @return the return type of this
   */
  @Override
  public AbstractType getReturnType(@Nullable Theta map) {
    AnnotatedTypeMirror annotatedReturnType;

    if (TreeUtils.isDiamondTree(invocation)) {
      Element e = ElementUtils.enclosingTypeElement(TreeUtils.elementFromUse(invocation));
      annotatedReturnType = typeFactory.getAnnotatedType(e);
    } else if (invocation instanceof MethodInvocationTree) {
      annotatedReturnType = annotatedExecutableType.getReturnType();
    } else {
      annotatedReturnType = typeFactory.getAnnotatedType(invocation);
    }

    if (map == null) {
      return new ProperType(annotatedReturnType, context);
    } else {
      return InferenceType.create(annotatedReturnType, map, context);
    }
  }

  @Override
  public List<AbstractType> getParameterTypes(@Nullable Theta map, int size) {
    return getParameterTypes(map, size, null, TreeUtils.isVarargsCall(invocation));
  }
}
