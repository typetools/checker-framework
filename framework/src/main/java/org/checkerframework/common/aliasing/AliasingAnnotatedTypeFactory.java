package org.checkerframework.common.aliasing;

import com.sun.source.tree.NewArrayTree;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.aliasing.qual.LeakedToResult;
import org.checkerframework.common.aliasing.qual.MaybeAliased;
import org.checkerframework.common.aliasing.qual.MaybeLeaked;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;

/** Annotated type factory for the Aliasing Checker. */
public class AliasingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** Aliasing annotations. */
  /** The @{@link MaybeAliased} annotation. */
  protected final AnnotationMirror MAYBE_ALIASED =
      AnnotationBuilder.fromClass(elements, MaybeAliased.class);
  /** The @{@link NonLeaked} annotation. */
  protected final AnnotationMirror NON_LEAKED =
      AnnotationBuilder.fromClass(elements, NonLeaked.class);
  /** The @{@link Unique} annotation. */
  protected final AnnotationMirror UNIQUE = AnnotationBuilder.fromClass(elements, Unique.class);
  /** The @{@link MaybeLeaked} annotation. */
  protected final AnnotationMirror MAYBE_LEAKED =
      AnnotationBuilder.fromClass(elements, MaybeLeaked.class);

  /** Create the type factory. */
  public AliasingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    if (this.getClass() == AliasingAnnotatedTypeFactory.class) {
      this.postInit();
    }
  }

  // @NonLeaked and @LeakedToResult are type qualifiers because of a checker framework limitation
  // (Issue 383). Once the stub parser gets updated to read non-type-qualifiers annotations on stub
  // files, this annotation won't be a type qualifier anymore.
  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return getBundledTypeQualifiers(MaybeLeaked.class);
  }

  @Override
  public CFTransfer createFlowTransferFunction(
      CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    CFTransfer ret = new AliasingTransfer(analysis);
    return ret;
  }

  protected class AliasingTreeAnnotator extends TreeAnnotator {

    public AliasingTreeAnnotator(AliasingAnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror type) {
      type.replaceAnnotation(UNIQUE);
      return super.visitNewArray(node, type);
    }
  }

  @Override
  protected ListTreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(new AliasingTreeAnnotator(this), super.createTreeAnnotator());
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new AliasingQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
  }

  /** AliasingQualifierHierarchy. */
  protected class AliasingQualifierHierarchy extends NoElementQualifierHierarchy {

    /**
     * Create AliasingQualifierHierarchy.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers
     * @param elements element utils
     */
    protected AliasingQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
      super(qualifierClasses, elements);
    }

    /**
     * Returns true is {@code anno} is annotation in the Leaked hierarchy.
     *
     * @param anno an annotation
     * @return true is {@code anno} is annotation in the Leaked hierarchy
     */
    private boolean isLeakedQualifier(AnnotationMirror anno) {
      return areSameByClass(anno, MaybeLeaked.class)
          || areSameByClass(anno, NonLeaked.class)
          || areSameByClass(anno, LeakedToResult.class);
    }

    @Override
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
      if (isLeakedQualifier(superAnno) && isLeakedQualifier(subAnno)) {
        // @LeakedToResult and @NonLeaked were supposed to be non-type-qualifiers annotations.
        // Currently the stub parser does not support non-type-qualifier annotations on receiver
        // parameters (Issue 383), therefore these annotations are implemented as type qualifiers
        // but the warnings related to the hierarchy are ignored.
        return true;
      }
      return super.isSubtype(subAnno, superAnno);
    }
  }
}
