package org.checkerframework.checker.optional;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Collection;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/** Type factory for the OptionalImplChecker. */
public class OptionalImplAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The element for java.util.Optional.map(). */
  private final ExecutableElement optionalMap;

  /** The @{@link Present} annotation. */
  protected final AnnotationMirror PRESENT = AnnotationBuilder.fromClass(elements, Present.class);

  /**
   * Creates an OptionalImplAnnotatedTypeFactory.
   *
   * @param checker the {@link org.checkerframework.checker.optional.OptionalImplChecker} associated
   *     with this type factory
   */
  public OptionalImplAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    postInit();
    optionalMap = TreeUtils.getMethodOrNull("java.util.Optional", "map", 1, getProcessingEnv());
  }

  @Override
  protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    super.addComputedTypeAnnotations(tree, type, iUseFlow);
    optionalMapNonNull(tree, type);
  }

  /**
   * If {@code tree} is a call to {@link java.util.Optional#map(Function)} whose argument is a
   * method reference m, then this method adds {@code @Present} to {@code type} if the following is
   * true:
   *
   * <ul>
   *   <li>The type of the receiver to {@link java.util.Optional#map(Function)} is {@code @Present},
   *       and
   *   <li>{@link #isReturnTypeNullable(MemberReferenceTree)} returns false for m.
   * </ul>
   *
   * @param tree a tree
   * @param type the type of the tree, which may be side-effected by this method
   */
  private void optionalMapNonNull(Tree tree, AnnotatedTypeMirror type) {
    if (!TreeUtils.isMethodInvocation(tree, optionalMap, processingEnv)) {
      return;
    }
    MethodInvocationTree mapTree = (MethodInvocationTree) tree;
    ExpressionTree argTree = mapTree.getArguments().get(0);
    if (argTree.getKind() != Kind.MEMBER_REFERENCE) {
      return;
    }
    AnnotatedTypeMirror mapReceiver = getReceiverType(mapTree);
    if (mapReceiver == null || !mapReceiver.hasEffectiveAnnotation(Present.class)) {
      return;
    }
    MemberReferenceTree memberReferenceTree = (MemberReferenceTree) argTree;
    if (!isReturnTypeNullable(memberReferenceTree)) {
      // The method still could have a @PolyNull on the return and might return null.
      // If @PolyNull is the primary annotation on the parameter and not on any type
      // arguments or array elements, then it is still safe to mark the optional type as
      // present.
      // TODO: Add the check for @PolyNull on arguments.
      type.replaceAnnotation(PRESENT);
    }
  }

  /**
   * Returns true if the return type of the function type of {@code memberReferenceTree} is
   * annotated with {@code @Nullable}.
   *
   * @param memberReferenceTree a member reference (which is to a method or constructor)
   * @return true if the return type of the function type of {@code memberReferenceTree} is
   *     annotated with {@code @Nullable}
   */
  private boolean isReturnTypeNullable(MemberReferenceTree memberReferenceTree) {
    // A member reference refers to either a method or a constructor.
    if (TreeUtils.MemberReferenceKind.getMemberReferenceKind(memberReferenceTree)
        .isConstructorReference()) {
      return false;
    }
    ExecutableElement memberReferenceFuncType = TreeUtils.elementFromUse(memberReferenceTree);
    if (memberReferenceFuncType.getEnclosingElement().getKind() == ElementKind.ANNOTATION_TYPE) {
      // Annotation element accessors are always non-null;
      return false;
    }

    if (!checker.hasOption("optionalMapAssumeNonNull")) {
      return true;
    }
    return containsNullable(memberReferenceFuncType.getAnnotationMirrors())
        || containsNullable(memberReferenceFuncType.getReturnType().getAnnotationMirrors());
  }

  /**
   * Returns true if {@code annos} contains an annotation named "Nullable".
   *
   * @param annos a collection of annotations
   * @return true if {@code annos} contains an annotation named "Nullable".
   */
  private boolean containsNullable(Collection<? extends AnnotationMirror> annos) {
    for (AnnotationMirror anno : annos) {
      if (anno.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public CFTransfer createFlowTransferFunction(
      CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    return new OptionalImplTransfer(analysis);
  }
}
