package org.checkerframework.checker.calledmethodsonelements;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethodsonelements.*;
import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElements;
import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElementsBottom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
import org.checkerframework.javacutil.*;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElements;
import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElementsBottom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
import org.checkerframework.javacutil.*;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the Called Methods Checker. */
public class CalledMethodsOnElementsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
  /** The {@code @}{@link MustCallOnElements()} annotation. */
  public final AnnotationMirror TOP;

  /** The {@code @}{@link MustCallOnElements()} annotation. */
  public final AnnotationMirror BOTTOM;

  /**
   * Fetches the store from the results of dataflow for {@code first}. If {@code afterFirstStore} is
   * true, then the store after {@code first} is returned; if {@code afterFirstStore} is false, the
   * store before {@code succ} is returned.
   *
   * @param afterFirstStore whether to use the store after the first block or the store before its
   *     successor, succ
   * @param first a block
   * @param succ first's successor
   * @return the appropriate CFStore, populated with MustCall annotations, from the results of
   *     running dataflow
   */
  public CFStore getStoreForBlock(boolean afterFirstStore, Block first, Block succ) {
    return afterFirstStore ? flowResult.getStoreAfter(first) : flowResult.getStoreBefore(succ);
  }

  public CFStore getStoreForBlock(Block block) {
    return flowResult.getStoreBefore(block);
  }

  public CFStore getStoreForTree(Tree tree) {
    return flowResult.getStoreBefore(tree);
  }

  /**
   * Create a new CalledMethodsOnElementsAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  public CalledMethodsOnElementsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    TOP = createCMOEAnnotation(Collections.emptyList());
    BOTTOM = AnnotationBuilder.fromClass(elements, CalledMethodsOnElementsBottom.class);
    // Don't call postInit() for subclasses.
    if (this.getClass() == CalledMethodsOnElementsAnnotatedTypeFactory.class) {
      this.postInit();
    }
  }

  private AnnotationMirror createCMOEAnnotation(List<String> methodList) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, CalledMethodsOnElements.class);
    String[] methodArray = methodList.toArray(new String[methodList.size()]);
    Arrays.sort(methodArray);
    builder.setValue("value", methodArray);
    return builder.build();
  }

  /** The {@link CalledMethodsOnElements#value} element/argument. */
  /*package-private*/ final ExecutableElement calledMethodsOnElementsValueElement =
     TreeUtils.getMethod(CalledMethodsOnElements.class, "value", 0, processingEnv);

   @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new CalledMethodsOnElementsQualifierHierarchy(
        this.getSupportedTypeQualifiers(), this.getProcessingEnv(), this);
  }

  /** Qualifier hierarchy for the Must Call Checker. */
  class CalledMethodsOnElementsQualifierHierarchy extends SubtypeIsSubsetQualifierHierarchy {

    /**
     * Creates a SubtypeIsSubsetQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param processingEnv processing environment
     * @param atypeFactory the associated type factory
     */
    public CalledMethodsOnElementsQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses,
        ProcessingEnvironment processingEnv,
        GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
      super(qualifierClasses, processingEnv, atypeFactory);
    }

    @Override
    public boolean isSubtypeShallow(
        AnnotationMirror subQualifier,
        TypeMirror subType,
        AnnotationMirror superQualifier,
        TypeMirror superType) {
      if (shouldHaveNoCalledMethodsOnElementsObligation(subType)
          || shouldHaveNoCalledMethodsOnElementsObligation(superType)) {
        return true;
      }
      return super.isSubtypeShallow(subQualifier, subType, superQualifier, superType);
    }

    @Override
    public @Nullable AnnotationMirror leastUpperBoundShallow(
        AnnotationMirror qualifier1, TypeMirror tm1, AnnotationMirror qualifier2, TypeMirror tm2) {
      boolean tm1NoCalledMethodsOnElements = shouldHaveNoCalledMethodsOnElementsObligation(tm1);
      boolean tm2NoCalledMethodsOnElements = shouldHaveNoCalledMethodsOnElementsObligation(tm2);
      if (tm1NoCalledMethodsOnElements == tm2NoCalledMethodsOnElements) {
        return super.leastUpperBoundShallow(qualifier1, tm1, qualifier2, tm2);
      } else if (tm1NoCalledMethodsOnElements) {
        return qualifier1;
      } else { // if (tm2NoCalledMethodsOnElements) {
        return qualifier2;
      }
    }
  }

  /**
   * Returns true if the given type should never have a called-methods-on-elements obligation.
   *
   * @param type the type to check
   * @return true if the given type should never have a called-methods-on-elements obligation
   */
  public boolean shouldHaveNoCalledMethodsOnElementsObligation(TypeMirror type) {
    return type.getKind().isPrimitive() || TypesUtils.isClass(type) || TypesUtils.isString(type);
  }
}
