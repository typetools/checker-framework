package org.checkerframework.checker.optional;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/** OptionalAnnotatedTypeFactory for the Optional Checker. */
public class OptionalAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The element for java.util.Optional.map(). */
  private final ExecutableElement optionalMap;

  /** The @{@link Present} annotation. */
  protected final AnnotationMirror PRESENT = AnnotationBuilder.fromClass(elements, Present.class);

  /**
   * Creates an OptionalAnnotatedTypeFactory.
   *
   * @param checker a checker
   */
  public OptionalAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    postInit();
    optionalMap = TreeUtils.getMethodOrNull("java.util.Optional", "map", 1, getProcessingEnv());
  }

  @Override
  public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
    AnnotatedTypeMirror result = super.getAnnotatedType(tree);
    optionalMapNonNull(tree, result);
    return result;
  }

  /**
   * If {@code tree} is a call to {@link java.util.Optional#map(Function)} whose argument is a
   * method reference, and if the option {@code "optionalMapAssumeNonNull"} is passed, then this
   * method adds {@code @Present} to {@code type} if the follow is true:
   *
   * <p>The type of the receiver to {@link java.util.Optional#map(Function)} is {@code @Present}
   *
   * <p>And the return type of the function type represented by the method reference that is an
   * argument to {@code map}, is not explicitly annotated with a {@code Nullable} annotation.
   *
   * @param tree a tree
   * @param type a type that is side-effected by this method
   */
  private void optionalMapNonNull(Tree tree, AnnotatedTypeMirror type) {
    if (!TreeUtils.isMethodInvocation(tree, optionalMap, processingEnv)) {
      return;
    }
    MethodInvocationTree mapTree = (MethodInvocationTree) tree;
    ExpressionTree argTree = mapTree.getArguments().get(0);
    if (argTree.getKind() == Kind.MEMBER_REFERENCE) {
      MemberReferenceTree memberReferenceTree = (MemberReferenceTree) argTree;
      AnnotatedTypeMirror optType = getReceiverType(mapTree);
      if (optType == null || !optType.hasEffectiveAnnotation(Present.class)) {
        return;
      }
      if (TreeUtils.MemberReferenceKind.getMemberReferenceKind(memberReferenceTree)
          .isConstructorReference()) {
        // Constructors are always non-null.
        type.replaceAnnotation(PRESENT);
        return;
      }
      if (!checker.hasOption("optionalMapAssumeNonNull")) {
        return;
      }
      ExecutableElement memberReferenceFuncType = TreeUtils.elementFromUse(memberReferenceTree);
      if (!containsNullable(memberReferenceFuncType.getAnnotationMirrors())
          && !containsNullable(memberReferenceFuncType.getReturnType().getAnnotationMirrors())) {
        type.replaceAnnotation(PRESENT);
      }
    }
  }

  /** A partial list of Nullable aliases. */
  private static final List<@FullyQualifiedName String> NULLABLE_ALIASES =
      Arrays.asList(
          "com.hivemq.extension.sdk.api.annotations.Nullable",
          "io.getunleash.lang.Nullable",
          "io.reactivex.annotations.Nullable",
          "javax.annotation.Nullable",
          "org.checkerframework.checker.nullness.qual.Nullable",
          "org.jetbrains.annotations.Nullable",
          "org.springframework.lang.Nullable",
          "se.llbit.util.annotation.Nullable");

  /**
   * Returns true if {@code annos} contains a nullable annotation.
   *
   * @param annos a collection of annotations
   * @return true if {@code annos} contains a nullable annotation
   */
  private boolean containsNullable(Collection<? extends AnnotationMirror> annos) {
    for (AnnotationMirror anno : annos) {
      String annoName = AnnotationUtils.annotationName(anno);
      if (NULLABLE_ALIASES.contains(annoName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public CFTransfer createFlowTransferFunction(
      CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    return new OptionalTransfer(analysis);
  }
}
