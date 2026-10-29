package org.checkerframework.checker.confidential;

import com.sun.source.tree.MethodInvocationTree;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.confidential.qual.BottomConfidential;
import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;
import org.checkerframework.checker.confidential.qual.UnknownConfidential;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;

/** Annotated type factory for the Confidential Checker. */
public class ConfidentialAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link NonConfidential} annotation mirror. */
  protected final AnnotationMirror NONCONFIDENTIAL;

  /** The {@code @}{@link Confidential} annotation mirror. */
  protected final AnnotationMirror CONFIDENTIAL;

  /** The {@code @}{@link UnknownConfidential} annotation mirror. */
  protected final AnnotationMirror UNKNOWN_CONFIDENTIAL;

  /** The {@code @}{@link BottomConfidential} annotation mirror. */
  protected final AnnotationMirror BOTTOM_CONFIDENTIAL;

  /** A singleton set containing the {@code @}{@link NonConfidential} annotation mirror. */
  private final AnnotationMirrorSet setOfNonConfidential;

  /** The Object.toString method. */
  private final ExecutableElement objectToString =
      TreeUtils.getMethod("java.lang.Object", "toString", 0, processingEnv);

  /**
   * Creates a {@link ConfidentialAnnotatedTypeFactory}.
   *
   * @param checker the confidential checker
   */
  @SuppressWarnings("this-escape")
  public ConfidentialAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.NONCONFIDENTIAL = AnnotationBuilder.fromClass(getElementUtils(), NonConfidential.class);
    this.CONFIDENTIAL = AnnotationBuilder.fromClass(getElementUtils(), Confidential.class);
    this.UNKNOWN_CONFIDENTIAL =
        AnnotationBuilder.fromClass(getElementUtils(), UnknownConfidential.class);
    this.BOTTOM_CONFIDENTIAL =
        AnnotationBuilder.fromClass(getElementUtils(), BottomConfidential.class);
    this.setOfNonConfidential = AnnotationMirrorSet.singleton(NONCONFIDENTIAL);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfNonConfidential;
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(),
        new ConfidentialAnnotatedTypeFactory.ConfidentialTreeAnnotator(this));
  }

  @Override
  public CFTransfer createFlowTransferFunction(
      CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    return new ConfidentialTransfer(analysis);
  }

  /**
   * A TreeAnnotator to enforce certain toString return type rules:
   *
   * <ul>
   *   <li>toString(@NonConfidential this) can return @NonConfidential or @Confidential String
   *   <li>toString(@Confidential this) can return @Confidential String
   * </ul>
   */
  private class ConfidentialTreeAnnotator extends TreeAnnotator {
    /**
     * Creates a {@link ConfidentialAnnotatedTypeFactory.ConfidentialTreeAnnotator}.
     *
     * @param atypeFactory the annotated type factory
     */
    public ConfidentialTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    /**
     * Visits a method invocation node. Enforces specific type-checking rules for Object.toString()
     * that allow a @NonConfidential Object to return a @NonConfidential String.
     *
     * <p>Supplements the @Confidential String return in Object.toString() to cover all secure use
     * cases, i.e. all cases covered by a @PolyConfidential receiver and return excepting
     * a @NonConfidential String from @Confidential receivers.
     *
     * <p>This rule also applies to every override of {@code Object.toString()}, and it takes
     * precedence over the override's declared return type. That is necessary for soundness: without
     * it, an override that is declared to return {@code @NonConfidential String} would let a
     * confidential receiver leak through {@code toString()}. A method that deliberately sanitizes a
     * confidential receiver must suppress the resulting warning, as described in the manual.
     *
     * @param tree an AST node representing a method call
     * @param type the type obtained from tree
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
      if (TreeUtils.isMethodInvocation(tree, objectToString, processingEnv)) {
        AnnotatedTypeMirror receiver = getReceiverType(tree);
        if (receiver == null) {
          // The receiver is unknown, so leave the declared return type unchanged.
        } else if (receiver.hasPrimaryAnnotation(BOTTOM_CONFIDENTIAL)) {
          // Bottom is the most precise result, and it is a subtype of every other qualifier.
          type.replaceAnnotation(BOTTOM_CONFIDENTIAL);
        } else if (receiver.hasPrimaryAnnotation(NONCONFIDENTIAL)) {
          type.replaceAnnotation(NONCONFIDENTIAL);
        } else {
          type.replaceAnnotation(CONFIDENTIAL);
        }
      }
      return super.visitMethodInvocation(tree, type);
    }
  }
}
