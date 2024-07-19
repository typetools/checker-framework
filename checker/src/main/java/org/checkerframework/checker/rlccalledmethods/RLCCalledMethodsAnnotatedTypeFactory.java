package org.checkerframework.checker.rlccalledmethods;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.calledmethods.EnsuresCalledMethodOnExceptionContract;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsBottom;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakAnnotatedTypeFactory;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class RLCCalledMethodsAnnotatedTypeFactory extends CalledMethodsAnnotatedTypeFactory {
  public final boolean permitStaticOwning;
  public final boolean noLightweightOwnership;
  private MustCallAnnotatedTypeFactory mcAtf;
  private ResourceLeakAnnotatedTypeFactory rlAtf;
  private ResourceLeakChecker rlc;

  /** The EnsuresCalledMethods.value element/field. */
  public final ExecutableElement ensuresCalledMethodsValueElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "value", 0, processingEnv);

  /** The EnsuresCalledMethods.methods element/field. */
  public final ExecutableElement ensuresCalledMethodsMethodsElement =
      TreeUtils.getMethod(EnsuresCalledMethods.class, "methods", 0, processingEnv);

  /** The EnsuresCalledMethods.List.value element/field. */
  private final ExecutableElement ensuresCalledMethodsListValueElement =
      TreeUtils.getMethod(EnsuresCalledMethods.List.class, "value", 0, processingEnv);

  public RLCCalledMethodsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.permitStaticOwning = getResourceLeakChecker().hasOption("permitStaticOwning");
    this.noLightweightOwnership = getResourceLeakChecker().hasOption("noLightweightOwnership");
    if (this.getClass() == RLCCalledMethodsAnnotatedTypeFactory.class) {
      this.postInit();
    }
  }

  /**
   * Returns the {@link EnsuresCalledMethods.List#value} element.
   *
   * @return the {@link EnsuresCalledMethods.List#value} element
   */
  public ExecutableElement getEnsuresCalledMethodsListValueElement() {
    return ensuresCalledMethodsListValueElement;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return getBundledTypeQualifiers(
        CalledMethods.class, CalledMethodsBottom.class, CalledMethodsPredicate.class);
  }

  @Override
  protected RLCCalledMethodsAnalysis createFlowAnalysis() {
    return new RLCCalledMethodsAnalysis((RLCCalledMethodsChecker) checker, this);
  }

  public ResourceLeakChecker getResourceLeakChecker() {
    if (rlc == null) {
      rlc = (ResourceLeakChecker) checker.getParentChecker();
    }
    return rlc;
  }

  public ResourceLeakAnnotatedTypeFactory getResourceLeakAnnotatedTypeFactory() {
    if (rlAtf == null) {
      rlAtf = (ResourceLeakAnnotatedTypeFactory) getResourceLeakChecker().getTypeFactory();
    }
    return rlAtf;
  }

  public MustCallAnnotatedTypeFactory getMustCallAnnotatedTypeFactory() {
    if (mcAtf == null) {
      mcAtf =
          getResourceLeakAnnotatedTypeFactory().getTypeFactoryOfSubchecker(MustCallChecker.class);
    }
    return mcAtf;
  }

  /**
   * Returns true if the checker should ignore exceptional control flow due to the given exception
   * type.
   *
   * @param exceptionType exception type
   * @return {@code true} if {@code exceptionType} is a member of {@link
   *     CalledMethodsAnalysis#ignoredExceptionTypes}, {@code false} otherwise
   */
  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    if (exceptionType.getKind() == TypeKind.DECLARED) {
      return ((RLCCalledMethodsAnalysis) analysis).isIgnoredExceptionType(exceptionType);
    }
    return false;
  }

  /**
   * Fetches the store from the results of dataflow for {@code block}. The store after {@code block}
   * is returned.
   *
   * @param block a block
   * @return the appropriate CFStore, populated with CalledMethods annotations, from the results of
   *     running dataflow
   */
  public TransferInput<AccumulationValue, AccumulationStore> getInput(Block block) {
    if (!analysis.isRunning()) {
      return flowResult.getInput(block);
    } else {
      return analysis.getInput(block);
    }
  }

  @Override
  public Set<EnsuresCalledMethodOnExceptionContract> getExceptionalPostconditions(
      ExecutableElement methodOrConstructor) {
    Set<EnsuresCalledMethodOnExceptionContract> result =
        super.getExceptionalPostconditions(methodOrConstructor);

    // This override is a sneaky way to satisfy a few subtle design constraints:
    //   1. The RLC requires destructors to close the class's @Owning fields even on exception
    //      (see ResourceLeakVisitor.checkOwningField).
    //   2. In versions 3.39.0 and earlier, the RLC did not have the annotation
    //      @EnsuresCalledMethodsOnException, meaning that for destructors it had to treat
    //      a simple @EnsuresCalledMethods annotation as serving both purposes.
    //
    // As a result, there is a lot of code that is missing the "correct"
    // @EnsuresCalledMethodsOnException annotations on its destructors.
    //
    // This override treats the @EnsuresCalledMethods annotations on destructors as if they
    // were also @EnsuresCalledMethodsOnException for backwards compatibility.  By overriding
    // this method we get both directions of checking: destructor implementations have to
    // satisfy these implicit contracts, and destructor callers get to benefit from them.
    //
    // It should be possible to remove this override entirely without sacrificing any soundness.
    // However, that is undesirable at this point because it would be a breaking change.
    //
    // TODO: gradually remove this override.
    //   1. When this override adds an implicit annotation, the Checker Framework should issue
    //      a warning along with a suggestion to add the right annotations.
    //   2. After a few months we should remove this override and require proper annotations on
    //      all destructors.

    if (isMustCallMethod(methodOrConstructor)) {
      Set<Contract.Postcondition> normalPostconditions =
          getContractsFromMethod().getPostconditions(methodOrConstructor);
      for (Contract.Postcondition normalPostcondition : normalPostconditions) {
        for (String method : getCalledMethods(normalPostcondition.annotation)) {
          result.add(
              new EnsuresCalledMethodOnExceptionContract(
                  normalPostcondition.expressionString, method));
        }
      }
    }

    return result;
  }

  /**
   * Returns true iff the {@code MustCall} annotation of the class that encloses the methodTree
   * names this method.
   *
   * @param elt a method
   * @return whether that method is one of the must-call methods for its enclosing class
   */
  private boolean isMustCallMethod(ExecutableElement elt) {
    TypeElement enclosingClass = ElementUtils.enclosingTypeElement(elt);
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory = getMustCallAnnotatedTypeFactory();
    AnnotationMirror mcAnno =
        mustCallAnnotatedTypeFactory
            .getAnnotatedType(enclosingClass)
            .getPrimaryAnnotationInHierarchy(mustCallAnnotatedTypeFactory.TOP);
    List<String> mcValues =
        AnnotationUtils.getElementValueArray(
            mcAnno,
            mustCallAnnotatedTypeFactory.getMustCallValueElement(),
            String.class,
            Collections.emptyList());
    String methodName = elt.getSimpleName().toString();
    return mcValues.contains(methodName);
  }
}
