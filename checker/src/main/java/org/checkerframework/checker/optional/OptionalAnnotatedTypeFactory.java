package org.checkerframework.checker.optional;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Collection;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
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

/** OptionalAnnotatedTypeFactory for the Optional Checker. */
public class OptionalAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The element for java.util.Optional.map(). */
  private final ExecutableElement optionalMap;

  /** The @{@link Present} annotation. */
  protected final AnnotationMirror PRESENT = AnnotationBuilder.fromClass(elements, Present.class);

  /**
   * Creates an OptionalAnnotatedTypeFactory.
   *
   * @param checker the Optional Checker associated with this type factory
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
   * method reference, then this method adds {@code @Present} to {@code type} if the following is
   * true:
   *
   * <ul>
   *   <li>The type of the receiver to {@link java.util.Optional#map(Function)} is {@code @Present},
   *       and
   *   <li>{@link #returnNotNullable(MemberReferenceTree)} returns true.
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
    if (argTree.getKind() == Kind.MEMBER_REFERENCE) {
      MemberReferenceTree memberReferenceTree = (MemberReferenceTree) argTree;
      AnnotatedTypeMirror optType = getReceiverType(mapTree);
      if (optType == null || !optType.hasEffectiveAnnotation(Present.class)) {
        return;
      }
      if (returnNotNullable(memberReferenceTree)) {
        type.replaceAnnotation(PRESENT);
      }
    }
  }

  /**
   * Returns true if the return type of the function type of {@code memberReferenceTree} is not
   * annotation with a nullable annotation.
   *
   * @param memberReferenceTree a member reference
   * @return true if the return type of the function type of {@code memberReferenceTree} is not
   *     annotation with a nullable annotation
   */
  private boolean returnNotNullable(MemberReferenceTree memberReferenceTree) {
    if (TreeUtils.MemberReferenceKind.getMemberReferenceKind(memberReferenceTree)
        .isConstructorReference()) {
      return true;
    }
    if (!checker.hasOption("optionalMapAssumeNonNull")) {
      return false;
    }
    ExecutableElement memberReferenceFuncType = TreeUtils.elementFromUse(memberReferenceTree);
    return !containsNullable(memberReferenceFuncType.getAnnotationMirrors())
        && !containsNullable(memberReferenceFuncType.getReturnType().getAnnotationMirrors());
  }

  /**
   * Returns true if {@code annos} contains a nullable annotation.
   *
   * @param annos a collection of annotations
   * @return true if {@code annos} contains a nullable annotation
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
    return new OptionalTransfer(analysis);
  }
}
