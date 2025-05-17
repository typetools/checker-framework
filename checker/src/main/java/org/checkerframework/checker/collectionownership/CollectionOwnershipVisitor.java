package org.checkerframework.checker.collectionownership;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;

/**
 * The visitor for the Collection Ownership Checker. This visitor is similar to BaseTypeVisitor, but
 * overrides methods that don't work well with the ownership type hierarchy because it doesn't use
 * the top type as the default type.
 */
public class CollectionOwnershipVisitor
    extends BaseTypeVisitor<CollectionOwnershipAnnotatedTypeFactory> {

  /**
   * Creates a new CollectionOwnershipVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public CollectionOwnershipVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * This method typically issues a warning if the result type of the constructor is not top,
   * because in top-default type systems that indicates a potential problem. The Must Call Checker
   * does not need this warning, because it expects the type of all constructors to be {@code
   * OwningCollectionBottom} (by default).
   *
   * <p>Instead, this method checks that the result type of a constructor is a supertype of the
   * declared type on the class, if one exists.
   *
   * @param constructorType an AnnotatedExecutableType for the constructor
   * @param constructorElement element that declares the constructor
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    AnnotatedTypeMirror defaultType =
        atypeFactory.getAnnotatedType(ElementUtils.enclosingTypeElement(constructorElement));
    AnnotationMirror defaultAnno = defaultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    AnnotatedTypeMirror resultType = constructorType.getReturnType();
    AnnotationMirror resultAnno = resultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    if (!qualHierarchy.isSubtypeShallow(
        defaultAnno, defaultType.getUnderlyingType(), resultAnno, resultType.getUnderlyingType())) {
      checker.reportError(
          constructorElement, "inconsistent.constructor.type", resultAnno, defaultAnno);
    }
  }

  /**
   * Change the default for exception parameter lower bounds to bottom (the default), to prevent
   * false positives.
   *
   * @return a set containing only the Bottom annotation
   */
  @Override
  protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
    return new AnnotationMirrorSet(atypeFactory.BOTTOM);
  }

  // TODO maybe check contravariance for parameters and covariance for return types here
  // (and invariance for fields)
  // @Override
  // protected boolean validateType(Tree tree, AnnotatedTypeMirror type) {
  //   if (TreeUtils.isClassTree(tree)) {
  //     TypeElement classEle = TreeUtils.elementFromDeclaration((ClassTree) tree);
  //     // If no @InheritableMustCall annotation is written here, `getDeclAnnotation()` gets one
  //     // from stub files and supertypes.
  //     AnnotationMirror anyInheritableMustCall =
  //         atypeFactory.getDeclAnnotation(classEle, InheritableMustCall.class);
  //     // An @InheritableMustCall annotation that is directly present.
  //     AnnotationMirror directInheritableMustCall =
  //         AnnotationUtils.getAnnotationByClass(
  //             classEle.getAnnotationMirrors(), InheritableMustCall.class);
  //     if (anyInheritableMustCall == null) {
  //       if (!ElementUtils.isFinal(classEle)) {
  //         // There is no @InheritableMustCall annotation on this or any superclass and
  //         // this is a non-final class.
  //         // If an explicit @MustCall annotation is present, issue a warning suggesting
  //         // that @InheritableMustCall is probably what the programmer means, for
  //         // usability.
  //         if (atypeFactory.getDeclAnnotation(classEle, MustCall.class) != null) {
  //           checker.reportWarning(
  //               tree, "mustcall.not.inheritable", ElementUtils.getQualifiedName(classEle));
  //         }
  //       }
  //     } else {
  //       // There is an @InheritableMustCall annotation on this, on a superclass, or in an
  //       // annotation file.
  //       // There are two possible problems:
  //       //  1. There is an inconsistent @MustCall on this.
  //       //  2. There is an explicit @InheritableMustCall here, and it is inconsistent with
  //       //     an @InheritableMustCall annotation on a supertype.

  //       // Check for problem 1.
  //       AnnotationMirror explicitMustCall =
  //           atypeFactory.fromElement(classEle).getPrimaryAnnotation();
  //       if (explicitMustCall != null) {
  //         // There is a @MustCall annotation here.

  //         List<String> inheritableMustCallVal =
  //             AnnotationUtils.getElementValueArray(
  //                 anyInheritableMustCall,
  //                 atypeFactory.inheritableMustCallValueElement,
  //                 String.class,
  //                 emptyStringList);
  //         AnnotationMirror inheritedMCAnno = atypeFactory.createMustCall(inheritableMustCallVal);

  //         // Issue an error if there is an inconsistent, user-written @MustCall annotation
  //         // here.
  //         AnnotationMirror effectiveMCAnno = type.getPrimaryAnnotation();
  //         TypeMirror tm = type.getUnderlyingType();
  //         if (effectiveMCAnno != null
  //             && !qualHierarchy.isSubtypeShallow(inheritedMCAnno, effectiveMCAnno, tm)) {

  //           checker.reportError(
  //               tree,
  //               "inconsistent.mustcall.subtype",
  //               ElementUtils.getQualifiedName(classEle),
  //               effectiveMCAnno,
  //               anyInheritableMustCall);
  //           return false;
  //         }
  //       }

  //       // Check for problem 2.
  //       if (directInheritableMustCall != null) {

  //         // `inheritedImcs` is inherited @InheritableMustCall annotations.
  //         List<AnnotationMirror> inheritedImcs = new ArrayList<>();
  //         for (TypeElement elt : ElementUtils.getDirectSuperTypeElements(classEle, elements)) {
  //           AnnotationMirror imc = atypeFactory.getDeclAnnotation(elt,
  // InheritableMustCall.class);
  //           if (imc != null) {
  //             inheritedImcs.add(imc);
  //           }
  //         }
  //         if (!inheritedImcs.isEmpty()) {
  //           // There is an inherited @InheritableMustCall annotation, in addition to the
  //           // one written explicitly here.
  //           List<String> inheritedMustCallVal = new ArrayList<>();
  //           for (AnnotationMirror inheritedImc : inheritedImcs) {
  //             inheritedMustCallVal.addAll(
  //                 AnnotationUtils.getElementValueArray(
  //                     inheritedImc, atypeFactory.inheritableMustCallValueElement, String.class));
  //           }
  //           AnnotationMirror inheritedMCAnno = atypeFactory.createMustCall(inheritedMustCallVal);

  //           AnnotationMirror effectiveMCAnno = type.getPrimaryAnnotation();

  //           TypeMirror tm = type.getUnderlyingType();

  //           if (!qualHierarchy.isSubtypeShallow(inheritedMCAnno, effectiveMCAnno, tm)) {

  //             checker.reportError(
  //                 tree,
  //                 "inconsistent.mustcall.subtype",
  //                 ElementUtils.getQualifiedName(classEle),
  //                 effectiveMCAnno,
  //                 inheritedMCAnno);
  //             return false;
  //           }
  //         }
  //       }
  //     }
  //   }
  //   return super.validateType(tree, type);
  // }

  // TODO maybe return false if user annotation is @OwningCollectionWithoutObligation
  // @Override
  // public boolean isValidUse(
  //     AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
  //   // MustCallAlias annotations are always permitted on type uses, despite not technically
  //   // being a part of the type hierarchy. It's necessary to get the annotation from the element
  //   // because MustCallAlias is aliased to PolyMustCall, which is what useType would contain.
  //   // Note that isValidUse does not need to consider component types, on which it should be
  //   // called separately.
  //   Element elt = TreeUtils.elementFromTree(tree);
  //   if (elt != null) {
  //     if (AnnotationUtils.containsSameByClass(elt.getAnnotationMirrors(), MustCallAlias.class)) {
  //       return true;
  //     }
  //     // Need to check the type mirror for ajava-derived annotations and the element itself
  //     // for human-written annotations from the source code. Getting to the ajava file
  //     // directly at this point is impossible, so we approximate "the ajava file has an
  //     // @MustCallAlias annotation" with "there is an @PolyMustCall annotation on the use
  //     // type, but not in the source code". This only works because none of our inference
  //     // techniques infer @PolyMustCall, so if @PolyMustCall is present but wasn't in the
  //     // source, it must have been derived from an @MustCallAlias annotation (which we do
  //     // infer).
  //     boolean ajavaFileHasMustCallAlias =
  //         useType.hasPrimaryAnnotation(PolyMustCall.class)
  //             && !atypeFactory.containsSameByClass(elt.getAnnotationMirrors(),
  // PolyMustCall.class);
  //     if (ajavaFileHasMustCallAlias) {
  //       return true;
  //     }
  //   }
  //   return super.isValidUse(declarationType, useType, tree);
  // }

  // TODO this might be important
  // @Override
  // protected boolean skipReceiverSubtypeCheck(
  //     MethodInvocationTree tree,
  //     AnnotatedTypeMirror methodDefinitionReceiver,
  //     AnnotatedTypeMirror methodCallReceiver) {
  //   // If you think of the receiver of the method call as an implicit parameter, it has some
  //   // MustCall type. For example, consider the method call:
  //   //   void foo(@MustCall("bar") ThisClass this)
  //   // If we now call o.foo() where o has @MustCall({"bar, baz"}), the receiver subtype check
  //   // would throw an error, since o is not a subtype of @MustCall("bar"). However, since foo
  //   // cannot take ownership of its receiver, it does not matter what it 'thinks' the @MustCall
  //   // methods of the receiver are. Hence, it is always sound to skip this check.
  //   return true;
  // }

  // /**
  //  * Does not issue any warnings.
  //  *
  //  * <p>This implementation prevents recursing into annotation arguments. Annotation arguments
  // are
  //  * literals, which don't have must-call obligations.
  //  *
  //  * <p>Annotation arguments are treated as return locations for the purposes of defaulting,
  // rather
  //  * than parameter locations. This causes them to default incorrectly when the annotation is
  //  * defined in bytecode. See https://github.com/typetools/checker-framework/issues/3178 for an
  //  * explanation of why this is necessary to avoid false positives.
  //  */
  // @Override
  // public Void visitAnnotation(AnnotationTree tree, Void p) {
  //   return null;
  // }
}
