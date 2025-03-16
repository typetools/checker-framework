package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.CompilationUnitTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.collectionownership.qual.NotOwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionBottom;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionWithoutObligation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;

/** The annotated type factory for the Collection Ownership Checker. */
public class CollectionOwnershipAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link NotOwningCollection} annotation. */
  public final AnnotationMirror NOTOWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollection} annotation. */
  public final AnnotationMirror OWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollectionWithoutObligation} annotation. */
  public final AnnotationMirror OWNINGCOLLECTIONWITHOUTOBLIGATION;

  /**
   * The {@code @}{@link OwningCollectionBottom}{@code ()} annotation. It is the default in
   * unannotated code.
   */
  public final AnnotationMirror BOTTOM;

  /**
   * Creates a CollectionOwnershipAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public CollectionOwnershipAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    NOTOWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, NotOwningCollection.class);
    OWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, OwningCollection.class);
    OWNINGCOLLECTIONWITHOUTOBLIGATION =
        AnnotationBuilder.fromClass(elements, OwningCollectionWithoutObligation.class);
    BOTTOM = AnnotationBuilder.fromClass(elements, OwningCollectionBottom.class);
    this.postInit();
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree newRoot) {
    super.setRoot(newRoot);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            NotOwningCollection.class,
            OwningCollection.class,
            OwningCollectionWithoutObligation.class,
            OwningCollectionBottom.class));
  }

  // @Override
  // protected TreeAnnotator createTreeAnnotator() {
  //   return new ListTreeAnnotator(super.createTreeAnnotator(), new MustCallTreeAnnotator(this));
  // }

  // @Override
  // protected TypeAnnotator createTypeAnnotator() {
  //   return new ListTypeAnnotator(super.createTypeAnnotator(), new MustCallTypeAnnotator(this));
  // }

  // @Override
  // protected QualifierPolymorphism createQualifierPolymorphism() {
  //   return new MustCallQualifierPolymorphism(processingEnv, this);
  // }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new CollectionOwnershipQualifierHierarchy(
        this.getSupportedTypeQualifiers(), elements, this);
  }

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

  //   /**
  //    * The TreeAnnotator for the MustCall type system.
  //    *
  //    * <p>This tree annotator treats non-owning method parameters as bottom, regardless of their
  //    * declared type, when they appear in the body of the method. Doing so is safe because being
  //    * non-owning means, by definition, that their must-call obligations are only relevant in the
  //    * callee. (This behavior is disabled if the {@code -AnoLightweightOwnership} option is
  // passed
  // to
  //    * the checker.)
  //    *
  //    * <p>The tree annotator also changes the type of resource variables to remove "close" from
  // their
  //    * must-call types, because the try-with-resources statement guarantees that close() is
  // called
  // on
  //    * all such variables.
  //    */
  //   private class MustCallTreeAnnotator extends TreeAnnotator {
  //     /**
  //      * Create a MustCallTreeAnnotator.
  //      *
  //      * @param mustCallAnnotatedTypeFactory the type factory
  //      */
  //     public MustCallTreeAnnotator(MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory) {
  //       super(mustCallAnnotatedTypeFactory);
  //     }

  //     @Override
  //     public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror type) {
  //       Element elt = TreeUtils.elementFromUse(tree);
  //       // The following changes are not desired for RLC _inference_ in unannotated programs,
  //       // where a goal is to infer and add @Owning annotations to formal parameters.
  //       // Therefore, if WPI is enabled, they should not be executed.
  //       if (getWholeProgramInference() == null
  //           && elt.getKind() == ElementKind.PARAMETER
  //           && (noLightweightOwnership || getDeclAnnotation(elt, Owning.class) == null)) {
  //         if (!type.hasPrimaryAnnotation(POLY)) {
  //           // Parameters that are not annotated with @Owning should be treated as bottom
  //           // (to suppress warnings about them). An exception is polymorphic parameters,
  //           // which might be @MustCallAlias (and so wouldn't be annotated with @Owning):
  //           // these are not modified, to support verification of @MustCallAlias
  //           // annotations.
  //           type.replaceAnnotation(BOTTOM);
  //         }
  //       }
  //       return super.visitIdentifier(tree, type);
  //     }
  //   }

  /** CollectionOwnership qualifier hierarchy. */
  private static class CollectionOwnershipQualifierHierarchy extends NoElementQualifierHierarchy {

    /**
     * Creates a NoElementQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     * @param atypeFactory the associated type factory
     */
    public CollectionOwnershipQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses,
        Elements elements,
        GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
      super(qualifierClasses, elements, atypeFactory);
    }
  }
}
