package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The visitor for the Must Call Checker. This visitor is similar to BaseTypeVisitor, but overrides
 * methods that don't work well with the MustCall type hierarchy because it doesn't use the top type
 * as the default type.
 */
public class MustCallVisitor extends BaseTypeVisitor<MustCallAnnotatedTypeFactory> {

  /** True if -AnoLightweightOwnership was passed on the command line. */
  private final boolean noLightweightOwnership;

  /**
   * Creates a new MustCallVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public MustCallVisitor(BaseTypeChecker checker) {
    super(checker);
    noLightweightOwnership = checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP);
  }

  @Override
  public Void visitReturn(ReturnTree tree, Void p) {
    // Only check return types if ownership is being transferred.
    if (!noLightweightOwnership) {
      MethodTree enclosingMethod = TreePathUtil.enclosingMethod(this.getCurrentPath());
      // enclosingMethod is null if this return site is inside a lambda. TODO: handle lambdas
      // more precisely?
      if (enclosingMethod != null) {
        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(enclosingMethod);
        AnnotationMirror notOwningAnno = atypeFactory.getDeclAnnotation(methodElt, NotOwning.class);
        if (notOwningAnno != null) {
          // Skip return type subtyping check, because not-owning pointer means Object
          // Construction Checker won't check anyway.
          return null;
        }
      }
    }
    return super.visitReturn(tree, p);
  }

  /** An empty string list. */
  private static final List<String> emptyStringList = Collections.emptyList();

  @Override
  protected boolean validateType(Tree tree, AnnotatedTypeMirror type) {
    if (TreeUtils.isClassTree(tree)) {
      TypeElement classEle = TreeUtils.elementFromDeclaration((ClassTree) tree);
      // If no @InheritableMustCall annotation is written here, `getDeclAnnotation()` gets one
      // from stub files and supertypes.
      AnnotationMirror anyInheritableMustCall =
          atypeFactory.getDeclAnnotation(classEle, InheritableMustCall.class);
      // An @InheritableMustCall annotation that is directly present.
      AnnotationMirror directInheritableMustCall =
          AnnotationUtils.getAnnotationByClass(
              classEle.getAnnotationMirrors(), InheritableMustCall.class);
      if (anyInheritableMustCall == null) {
        if (!ElementUtils.isFinal(classEle)) {
          // There is no @InheritableMustCall annotation on this or any superclass and
          // this is a non-final class.
          // If an explicit @MustCall annotation is present, issue a warning suggesting
          // that @InheritableMustCall is probably what the programmer means, for
          // usability.
          if (atypeFactory.getDeclAnnotation(classEle, MustCall.class) != null) {
            checker.reportWarning(
                tree, "mustcall.not.inheritable", ElementUtils.getQualifiedName(classEle));
          }
        }
      } else {
        // There is an @InheritableMustCall annotation on this, on a superclass, or in an
        // annotation file.
        // There are two possible problems:
        //  1. There is an inconsistent @MustCall on this.
        //  2. There is an explicit @InheritableMustCall here, and it is inconsistent with
        //     an @InheritableMustCall annotation on a supertype.

        // Check for problem 1.
        AnnotationMirror explicitMustCall = atypeFactory.fromElement(classEle).getAnnotation();
        if (explicitMustCall != null) {
          // There is a @MustCall annotation here.

          List<String> inheritableMustCallVal =
              AnnotationUtils.getElementValueArray(
                  anyInheritableMustCall,
                  atypeFactory.inheritableMustCallValueElement,
                  String.class,
                  emptyStringList);
          AnnotationMirror inheritedMCAnno = atypeFactory.createMustCall(inheritableMustCallVal);

          // Issue an error if there is an inconsistent, user-written @MustCall annotation
          // here.
          AnnotationMirror effectiveMCAnno = type.getAnnotation();
          if (effectiveMCAnno != null
              && !atypeFactory
                  .getQualifierHierarchy()
                  .isSubtype(inheritedMCAnno, effectiveMCAnno)) {

            checker.reportError(
                tree,
                "inconsistent.mustcall.subtype",
                ElementUtils.getQualifiedName(classEle),
                effectiveMCAnno,
                anyInheritableMustCall);
            return false;
          }
        }

        // Check for problem 2.
        if (directInheritableMustCall != null) {

          // `inheritedImcs` is inherited @InheritableMustCall annotations.
          List<AnnotationMirror> inheritedImcs = new ArrayList<>();
          for (TypeElement elt : ElementUtils.getDirectSuperTypeElements(classEle, elements)) {
            AnnotationMirror imc = atypeFactory.getDeclAnnotation(elt, InheritableMustCall.class);
            if (imc != null) {
              inheritedImcs.add(imc);
            }
          }
          if (!inheritedImcs.isEmpty()) {
            // There is an inherited @InheritableMustCall annotation, in addition to the
            // one written explicitly here.
            List<String> inheritedMustCallVal = new ArrayList<>();
            for (AnnotationMirror inheritedImc : inheritedImcs) {
              inheritedMustCallVal.addAll(
                  AnnotationUtils.getElementValueArray(
                      inheritedImc, atypeFactory.inheritableMustCallValueElement, String.class));
            }
            AnnotationMirror inheritedMCAnno = atypeFactory.createMustCall(inheritedMustCallVal);

            AnnotationMirror effectiveMCAnno = type.getAnnotation();

            if (!atypeFactory.getQualifierHierarchy().isSubtype(inheritedMCAnno, effectiveMCAnno)) {

              checker.reportError(
                  tree,
                  "inconsistent.mustcall.subtype",
                  ElementUtils.getQualifiedName(classEle),
                  effectiveMCAnno,
                  inheritedMCAnno);
              return false;
            }
          }
        }
      }
    }
    return super.validateType(tree, type);
  }

  @Override
  public boolean isValidUse(
      AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
    // MustCallAlias annotations are always permitted on type uses, despite not technically
    // being a part of the type hierarchy. It's necessary to get the annotation from the element
    // because MustCallAlias is aliased to PolyMustCall, which is what useType would contain.
    // Note that isValidUse does not need to consider component types, on which it should be
    // called separately.
    Element elt = TreeUtils.elementFromTree(tree);
    if (elt != null
        && AnnotationUtils.containsSameByClass(elt.getAnnotationMirrors(), MustCallAlias.class)) {
      return true;
    }
    return super.isValidUse(declarationType, useType, tree);
  }

  @Override
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree tree,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    // It does not make sense for receivers to have must-call obligations. If the receiver of a
    // method were to have a non-empty must-call obligation, then actually this method should
    // be part of the must-call annotation on the class declaration! So skipping this check is
    // always sound.
    return true;
  }

  /**
   * This boolean is used to communicate between different levels of the common assignment check
   * whether a given check is being carried out on a (pseudo-)assignment to a resource variable. In
   * those cases, close doesn't need to be considered when doing the check, since close will always
   * be called by Java.
   *
   * <p>The check for whether the LHS is a resource variable can only be carried out on the element,
   * but the effect needs to happen at the stage where the type is available (i.e. close needs to be
   * removed from the type). Thus, this variable is used to communicate that a resource variable was
   * detected on the LHS.
   */
  private boolean commonAssignmentCheckOnResourceVariable = false;

  /**
   * Mark (using the {@code #commonAssignmentCheckOnResourceVariable} field of this class) any
   * assignments where the LHS is a resource variable, so that close doesn't need to be considered.
   * See {@link #commonAssignmentCheck(AnnotatedTypeMirror, AnnotatedTypeMirror, Tree, String,
   * Object...)} for the code that uses and removes the mark.
   */
  @Override
  protected void commonAssignmentCheck(
      Tree varTree,
      ExpressionTree valueExp,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    Element elt = TreeUtils.elementFromTree(varTree);
    if (elt != null && elt.getKind() == ElementKind.RESOURCE_VARIABLE) {
      commonAssignmentCheckOnResourceVariable = true;
    }
    super.commonAssignmentCheck(varTree, valueExp, errorKey, extraArgs);
  }

  /**
   * Iff the LHS is a resource variable, then {@code #commonAssignmentCheckOnResourceVariable} will
   * be true. This method guarantees that {@code #commonAssignmentCheckOnResourceVariable} will be
   * false when it returns.
   */
  @Override
  protected void commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    if (noMustCallObligation(varType) || noMustCallObligation(valueType)) {
      return;
    }

    if (commonAssignmentCheckOnResourceVariable) {
      commonAssignmentCheckOnResourceVariable = false;
      // The LHS has been marked as a resource variable.  Skip the standard common assignment
      // check; instead do a check that does not include "close".
      AnnotationMirror varAnno = varType.getAnnotationInHierarchy(atypeFactory.TOP);
      AnnotationMirror valAnno = valueType.getAnnotationInHierarchy(atypeFactory.TOP);
      if (atypeFactory
          .getQualifierHierarchy()
          .isSubtype(atypeFactory.withoutClose(valAnno), atypeFactory.withoutClose(varAnno))) {
        return;
      }
      // Note that in this case, the rest of the common assignment check should fail (barring
      // an exception).  Control falls through here to avoid duplicating error-issuing code.
    }
    // commonAssignmentCheckOnResourceVariable is already false, so no need to set it.
    super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
  }

  /**
   * This method typically issues a warning if the result type of the constructor is not top,
   * because in top-default type systems that indicates a potential problem. The Must Call Checker
   * does not need this warning, because it expects the type of all constructors to be {@code
   * MustCall({})} (by default) or some other {@code MustCall} type, not the top type.
   *
   * <p>Instead, this method checks that the result type of a constructor is a supertype of the
   * declared type on the class, if one exists.
   *
   * @param constructorType AnnotatedExecutableType for the constructor
   * @param constructorElement element that declares the constructor
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    AnnotatedTypeMirror defaultType =
        atypeFactory.getAnnotatedType(ElementUtils.enclosingTypeElement(constructorElement));
    AnnotationMirror defaultAnno = defaultType.getAnnotationInHierarchy(atypeFactory.TOP);
    AnnotationMirror resultAnno =
        constructorType.getReturnType().getAnnotationInHierarchy(atypeFactory.TOP);
    if (!atypeFactory.getQualifierHierarchy().isSubtype(defaultAnno, resultAnno)) {
      checker.reportError(
          constructorElement, "inconsistent.constructor.type", resultAnno, defaultAnno);
    }
  }

  /**
   * Change the default for exception parameter lower bounds to bottom (the default), to prevent
   * false positives. This is unsound; see the discussion on
   * https://github.com/typetools/checker-framework/issues/3839.
   *
   * <p>TODO: change checking of throws clauses to require that the thrown exception
   * is @MustCall({}). This would probably eliminate most of the same false positives, without
   * adding undue false positives.
   *
   * @return a set containing only the @MustCall({}) annotation
   */
  @Override
  protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
    return new AnnotationMirrorSet(atypeFactory.BOTTOM);
  }

  /**
   * Does not issue any warnings.
   *
   * <p>This implementation prevents recursing into annotation arguments. Annotation arguments are
   * literals, which don't have must-call obligations.
   *
   * <p>Annotation arguments are treated as return locations for the purposes of defaulting, rather
   * than parameter locations. This causes them to default incorrectly when the annotation is
   * defined in bytecode. See https://github.com/typetools/checker-framework/issues/3178 for an
   * explanation of why this is necessary to avoid false positives.
   */
  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    return null;
  }

  /**
   * Returns true if the given type should never have a must-call obligation.
   *
   * @param atm the type to check
   * @return true if the given type should never have a must-call obligation
   */
  private boolean noMustCallObligation(AnnotatedTypeMirror atm) {
    if (atm.getKind().isPrimitive()) {
      return true;
    }
    TypeMirror tm = atm.getUnderlyingType();
    if (TypesUtils.isClass(tm) || TypesUtils.isString(tm)) {
      return true;
    }
    return false;
  }

  @Override
  protected boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType) {
    if (noMustCallObligation(castType) || noMustCallObligation(exprType)) {
      return true;
    }

    return super.isTypeCastSafe(castType, exprType);
  }
}
