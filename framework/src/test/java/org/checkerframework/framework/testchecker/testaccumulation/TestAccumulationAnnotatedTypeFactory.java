package org.checkerframework.framework.testchecker.testaccumulation;

import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.testaccumulation.qual.TestAccumulation;
import org.checkerframework.framework.testchecker.testaccumulation.qual.TestAccumulationBottom;
import org.checkerframework.framework.testchecker.testaccumulation.qual.TestAccumulationPredicate;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The annotated type factory for a test accumulation checker, which implements a basic called
 * methods checker.
 */
public class TestAccumulationAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {
  /**
   * Create a new accumulation checker's annotated type factory.
   *
   * @param checker the checker
   */
  public TestAccumulationAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(
        checker,
        TestAccumulation.class,
        TestAccumulationBottom.class,
        TestAccumulationPredicate.class);
    this.postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new TestAccumulationTreeAnnotator(this));
  }

  /**
   * Necessary for the type rule for called methods described below. A new accumulation analysis
   * might have other type rules here, or none at all.
   */
  private class TestAccumulationTreeAnnotator extends AccumulationTreeAnnotator {
    /**
     * Creates an instance of this tree annotator for the given type factory.
     *
     * @param factory the type factory
     */
    public TestAccumulationTreeAnnotator(AccumulationAnnotatedTypeFactory factory) {
      super(factory);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
      // CalledMethods requires special treatment of the return values of methods that return
      // their receiver: the default return type must include the method being invoked.
      //
      // The basic accumulation analysis cannot handle this case - it can use the RR checker
      // to transfer an annotation from the receiver to the return type, but because
      // accumulation
      // (has to) happen in dataflow, the correct annotation may not yet be available. The
      // basic
      // accumulation analysis therefore only supports "pass-through" returns receiver
      // methods;
      // it does not support automatically accumulating at the same time.
      if (returnsThis(tree)) {
        String methodName = TreeUtils.getMethodName(tree.getMethodSelect());
        AnnotationMirror oldAnno = type.getAnnotationInHierarchy(top);
        type.replaceAnnotation(
            qualHierarchy.greatestLowerBound(oldAnno, createAccumulatorAnnotation(methodName)));
      }
      return super.visitMethodInvocation(tree, type);
    }
  }
}
