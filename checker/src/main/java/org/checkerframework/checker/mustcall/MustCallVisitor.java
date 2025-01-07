package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
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
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
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

  @Override
  public Void visitAssignment(AssignmentTree tree, Void p) {
    // This code implements the following rule:
    //  * It is always safe to assign a MustCallAlias parameter of a constructor
    //    to an owning field of the enclosing class.
    // It is necessary to special case this because MustCallAlias is translated
    // into @PolyMustCall, so the common assignment check will fail when assigning
    // an @MustCallAlias parameter to an owning field: the parameter is polymorphic,
    // but the field is not.
    ExpressionTree lhs = tree.getVariable();
    ExpressionTree rhs = tree.getExpression();
    Element lhsElt = TreeUtils.elementFromTree(lhs);
    Element rhsElt = TreeUtils.elementFromTree(rhs);
    if (lhsElt != null && rhsElt != null) {
      // Note that it is not necessary to check that the assignment is to a field of this,
      // because that is implied by the other conditions:
      // * if the field is final, then the only place it can be assigned to is in the
      //   constructor of the proper object (enforced by javac).
      // * if the field is not final, then it cannot be assigned to in a constructor at all:
      //   the @CreatesMustCallFor annotation cannot be written on a constructor (it has
      //   @Target({ElementType.METHOD})), so this code relies on the standard rules for
      //   non-final owning field reassignment, which prevent it without an
      //   @CreatesMustCallFor annotation except in the constructor of the object containing
      //   the field.
      boolean lhsIsOwningField =
          lhs.getKind() == Tree.Kind.MEMBER_SELECT
              && atypeFactory.getDeclAnnotation(lhsElt, Owning.class) != null;
      boolean rhsIsMCA =
          atypeFactory.containsSameByClass(rhsElt.getAnnotationMirrors(), MustCallAlias.class);
      boolean rhsIsConstructorParam =
          rhsElt.getKind() == ElementKind.PARAMETER
              && rhsElt.getEnclosingElement().getKind() == ElementKind.CONSTRUCTOR;
      if (lhsIsOwningField && rhsIsMCA && rhsIsConstructorParam) {
        // Do not execute common assignment check.
        return null;
      }
    }

    return super.visitAssignment(tree, p);
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
        AnnotationMirror explicitMustCall =
            atypeFactory.fromElement(classEle).getPrimaryAnnotation();
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
          AnnotationMirror effectiveMCAnno = type.getPrimaryAnnotation();
          TypeMirror tm = type.getUnderlyingType();
          if (effectiveMCAnno != null
              && !qualHierarchy.isSubtypeShallow(inheritedMCAnno, effectiveMCAnno, tm)) {

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

            AnnotationMirror effectiveMCAnno = type.getPrimaryAnnotation();

            TypeMirror tm = type.getUnderlyingType();

            if (!qualHierarchy.isSubtypeShallow(inheritedMCAnno, effectiveMCAnno, tm)) {

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
    if (elt != null) {
      if (AnnotationUtils.containsSameByClass(elt.getAnnotationMirrors(), MustCallAlias.class)) {
        return true;
      }
      // Need to check the type mirror for ajava-derived annotations and the element itself
      // for human-written annotations from the source code. Getting to the ajava file
      // directly at this point is impossible, so we approximate "the ajava file has an
      // @MustCallAlias annotation" with "there is an @PolyMustCall annotation on the use
      // type, but not in the source code". This only works because none of our inference
      // techniques infer @PolyMustCall, so if @PolyMustCall is present but wasn't in the
      // source, it must have been derived from an @MustCallAlias annotation (which we do
      // infer).
      boolean ajavaFileHasMustCallAlias =
          useType.hasPrimaryAnnotation(PolyMustCall.class)
              && !atypeFactory.containsSameByClass(elt.getAnnotationMirrors(), PolyMustCall.class);
      if (ajavaFileHasMustCallAlias) {
        return true;
      }
    }
    return super.isValidUse(declarationType, useType, tree);
  }

  @Override
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree tree,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    // If you think of the receiver of the method call as an implicit parameter, it has some
    // MustCall type. For example, consider the method call:
    //   void foo(@MustCall("bar") ThisClass this)
    // If we now call o.foo() where o has @MustCall({"bar, baz"}), the receiver subtype check
    // would throw an error, since o is not a subtype of @MustCall("bar"). However, since foo
    // cannot take ownership of its receiver, it does not matter what it 'thinks' the @MustCall
    // methods of the receiver are. Hence, it is always sound to skip this check.
    return true;
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
}
