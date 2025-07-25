package org.checkerframework.checker.guieffect;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.guieffect.Effect.EffectRange;
import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.SafeType;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIPackage;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

/** Annotated type factory for the GUI Effect Checker. */
public class GuiEffectTypeFactory extends BaseAnnotatedTypeFactory {

  protected final boolean debugSpew;

  /**
   * Keeps track of all lambda expressions with inferred UIEffect.
   *
   * <p>{@link #constrainLambdaToUI(LambdaExpressionTree) constrainLambdaToUI} adds lambda
   * expressions to this set, and is called from GuiEffectVisitor whenever a lambda expression calls
   * a @UIEffect method. Afterwards {@link
   * #getInferedEffectForLambdaExpression(LambdaExpressionTree) getInferedEffectForLambdaExpression}
   * uses this set and the type annotations of the functional interface of the lambda to figure out
   * if it can affect the UI or not.
   */
  protected final Set<LambdaExpressionTree> uiLambdas = new HashSet<>();

  /**
   * Keeps track of all anonymous inner classes with inferred UIEffect.
   *
   * <p>{@link #constrainAnonymousClassToUI(TypeElement) constrainAnonymousClassToUI} adds anonymous
   * inner classes to this set, and is called from GuiEffectVisitor whenever an anonymous inner
   * class calls a @UIEffect method. Afterwards {@link #isUIType(TypeElement) isUIType} and {@link
   * #getAnnotatedType(Tree) getAnnotatedType} will treat this inner class as if it had been
   * annotated with @UI.
   */
  protected final Set<TypeElement> uiAnonClasses = new HashSet<>();

  /** The @{@link AlwaysSafe} annotation. */
  protected final AnnotationMirror ALWAYSSAFE =
      AnnotationBuilder.fromClass(elements, AlwaysSafe.class);

  /** The @{@link PolyUI} annotation. */
  protected final AnnotationMirror POLYUI = AnnotationBuilder.fromClass(elements, PolyUI.class);

  /** The @{@link UI} annotation. */
  protected final AnnotationMirror UI = AnnotationBuilder.fromClass(elements, UI.class);

  @SuppressWarnings("this-escape")
  public GuiEffectTypeFactory(BaseTypeChecker checker, boolean spew) {
    // use true to enable flow inference, false to disable it
    super(checker, false);

    debugSpew = spew;
    this.postInit();
  }

  /**
   * Returns true if the given type is polymorphic.
   *
   * @param cls the type to test
   * @return true if the given type is polymorphic
   */
  public boolean isPolymorphicType(TypeElement cls) {
    assert (cls != null);
    return getDeclAnnotation(cls, PolyUIType.class) != null
        || fromElement(cls).hasPrimaryAnnotation(PolyUI.class);
  }

  public boolean isUIType(TypeElement cls) {
    if (debugSpew) {
      System.err.println(" isUIType(" + cls + ")");
    }
    boolean targetClassUIP = fromElement(cls).hasPrimaryAnnotation(UI.class);
    AnnotationMirror targetClassUITypeP = getDeclAnnotation(cls, UIType.class);
    AnnotationMirror targetClassSafeTypeP = getDeclAnnotation(cls, SafeType.class);

    if (targetClassSafeTypeP != null) {
      return false; // explicitly marked not a UI type
    }

    boolean hasUITypeDirectly = (targetClassUIP || targetClassUITypeP != null);

    if (hasUITypeDirectly) {
      return true;
    }

    // Anon inner classes should not inherit the package annotation, since
    // they're so often used for closures to run async on background threads.
    if (isAnonymousType(cls)) {
      // However, we need to look into Anonymous class effect inference
      if (uiAnonClasses.contains(cls)) {
        return true;
      }
      return false;
    }

    // We don't check polymorphic annos so we can make a couple methods of
    // an @UIType polymorphic explicitly
    // AnnotationMirror targetClassPolyP = getDeclAnnotation(cls, PolyUI.class);
    // AnnotationMirror targetClassPolyTypeP = getDeclAnnotation(cls, PolyUIType.class);
    boolean targetClassSafeP = fromElement(cls).hasPrimaryAnnotation(AlwaysSafe.class);
    if (targetClassSafeP) {
      return false; // explicitly annotated otherwise
    }

    // Look for the package
    Element packageP = ElementUtils.enclosingPackage(cls);

    if (packageP != null) {
      if (debugSpew) {
        System.err.println("Found package " + packageP);
      }
      if (getDeclAnnotation(packageP, UIPackage.class) != null) {
        if (debugSpew) {
          System.err.println("Package " + packageP + " is annotated @UIPackage");
        }
        return true;
      }
    }

    return false;
  }

  // TODO: is there a framework method for this?
  private static boolean isAnonymousType(TypeElement elem) {
    return elem.getSimpleName().length() == 0;
  }

  /**
   * Calling context annotations.
   *
   * <p>To make anon-inner-classes work, I need to climb the inheritance DAG, until I:
   *
   * <ul>
   *   <li>find the class/interface that declares this calling method (an anon inner class is a
   *       separate class that implements an interface)
   *   <li>check whether *that* declaration specifies @UI on either the type or method
   * </ul>
   *
   * A method has the UI effect when:
   *
   * <ol>
   *   <li>A method is UI if annotated @UIEffect
   *   <li>A method is UI if the enclosing class is annotated @UI or @UIType and the method is not
   *       annotated @AlwaysSafe
   *   <li>A method is UI if the corresponding method in the super-class/interface is UI, and this
   *       method is not annotated @AlwaysSafe, and this method resides in an anonymous inner class
   *       (named classes still require a package/class/method annotation to make it UI, only anon
   *       inner classes have this inheritance-by-default)
   *       <ul>
   *         <li>A method must be *annotated* UI if the method it overrides is *annotated* UI
   *         <li>A method must be *annotated* UI if it overrides a UI method and the enclosing class
   *             is not UI
   *       </ul>
   *   <li>It is an error if a method is UI but the same method in a super-type is not UI
   *   <li>It is an error if two super-types specify the same method, where one type says it's UI
   *       and one says it's not (it's possible to simply enforce the weaker (safe) effect, but this
   *       seems more principled, it's easier --- backwards-compatible --- to change our minds about
   *       this later)
   * </ol>
   */
  public Effect getDeclaredEffect(ExecutableElement methodElt) {
    if (debugSpew) {
      System.err.println("begin mayHaveUIEffect(" + methodElt + ")");
    }
    AnnotationMirror targetUIP = getDeclAnnotation(methodElt, UIEffect.class);
    AnnotationMirror targetSafeP = getDeclAnnotation(methodElt, SafeEffect.class);
    AnnotationMirror targetPolyP = getDeclAnnotation(methodElt, PolyUIEffect.class);
    TypeElement targetClassElt = (TypeElement) methodElt.getEnclosingElement();
    boolean hasMainThreadAnnot =
        getDeclAnnotations(methodElt).toString().contains("@android.support.annotation.MainThread");

    if (debugSpew) {
      System.err.println("targetClassElt found");
    }

    // Short-circuit if the method is explicitly annotated
    if (targetSafeP != null) {
      if (debugSpew) {
        System.err.println("Method marked @SafeEffect");
      }
      return new Effect(SafeEffect.class);
    } else if (targetUIP != null || hasMainThreadAnnot) {
      if (debugSpew) {
        System.err.println("Method marked @UIEffect");
      }
      return new Effect(UIEffect.class);
    } else if (targetPolyP != null) {
      if (debugSpew) {
        System.err.println("Method marked @PolyUIEffect");
      }
      return new Effect(PolyUIEffect.class);
    }

    // The method is not explicitly annotated, so check class and package annotations,
    // and supertype effects if in an anonymous inner class

    if (isUIType(targetClassElt)) {
      // Already checked, no explicit @SafeEffect annotation
      return new Effect(UIEffect.class);
    }

    // Anonymous inner types should just get the effect of the parent by default, rather than
    // annotating every instance. Unless it's implementing a polymorphic supertype, in which
    // case we still want the developer to be explicit.
    if (isAnonymousType(targetClassElt)) {
      boolean canInheritParentEffects = true; // Refine this for polymorphic parents
      DeclaredType directSuper = (DeclaredType) targetClassElt.getSuperclass();
      TypeElement superElt = (TypeElement) directSuper.asElement();
      // Anonymous subtypes of polymorphic classes other than object can't inherit
      if (getDeclAnnotation(superElt, PolyUIType.class) != null
          && !TypesUtils.isObject(directSuper)) {
        canInheritParentEffects = false;
      } else {
        for (TypeMirror ifaceM : targetClassElt.getInterfaces()) {
          DeclaredType iface = (DeclaredType) ifaceM;
          TypeElement ifaceElt = (TypeElement) iface.asElement();
          if (getDeclAnnotation(ifaceElt, PolyUIType.class) != null) {
            canInheritParentEffects = false;
          }
        }
      }

      if (canInheritParentEffects) {
        EffectRange r = findInheritedEffectRange(targetClassElt, methodElt);
        return (r != null ? Effect.min(r.min, r.max) : new Effect(SafeEffect.class));
      }
    }

    return new Effect(SafeEffect.class);
  }

  /**
   * Get the effect of a method call at its callsite, acknowledging polymorphic instantiation using
   * type use annotations.
   *
   * @param tree the method invocation as an AST node
   * @param callerReceiver the type of the receiver object if available. Used to resolve direct
   *     calls like "super()"
   * @param methodElt the element of the callee method
   * @return the computed effect (SafeEffect or UIEffect) for the method call
   */
  public Effect getComputedEffectAtCallsite(
      MethodInvocationTree tree,
      AnnotatedTypeMirror.AnnotatedDeclaredType callerReceiver,
      ExecutableElement methodElt) {
    Effect targetEffect = getDeclaredEffect(methodElt);
    if (targetEffect.isPoly()) {
      AnnotatedTypeMirror srcType = null;
      ExpressionTree methodSelect = tree.getMethodSelect();
      if (methodSelect instanceof MemberSelectTree) {
        ExpressionTree src = ((MemberSelectTree) methodSelect).getExpression();
        srcType = getAnnotatedType(src);
      } else if (methodSelect instanceof IdentifierTree) {
        // Tree.Kind.IDENTIFIER, e.g. a direct call like "super()"
        if (callerReceiver == null) {
          // Not enought information provided to instantiate this type-polymorphic effects
          return targetEffect;
        }
        srcType = callerReceiver;
      } else {
        throw new TypeSystemError("Unexpected getMethodSelect() kind at callsite " + tree);
      }

      // Instantiate type-polymorphic effects
      if (srcType.hasPrimaryAnnotation(AlwaysSafe.class)) {
        targetEffect = new Effect(SafeEffect.class);
      } else if (srcType.hasPrimaryAnnotation(UI.class)) {
        targetEffect = new Effect(UIEffect.class);
      }
      // Poly substitution would be a noop.
    }
    return targetEffect;
  }

  /**
   * Get the inferred effect of a lambda expression based on the type annotations of its functional
   * interface and the effects of the calls in its body.
   *
   * <p>This relies on GuiEffectVisitor to perform the actual inference step and mark lambdas
   * with @PolyUIEffect functional interfaces as being explicitly UI-affecting using the {@link
   * #constrainLambdaToUI(LambdaExpressionTree) constrainLambdaToUI} method.
   *
   * @param lambdaTree a lambda expression's AST node
   * @return the inferred effect of the lambda
   */
  public Effect getInferedEffectForLambdaExpression(LambdaExpressionTree lambdaTree) {
    // @UI type if annotated on the lambda expression explicitly
    if (uiLambdas.contains(lambdaTree)) {
      return new Effect(UIEffect.class);
    }
    ExecutableElement functionalInterfaceMethodElt =
        TreeUtils.findFunction(lambdaTree, checker.getProcessingEnvironment());
    if (debugSpew) {
      System.err.println("functionalInterfaceMethodElt found for lambda");
    }
    return getDeclaredEffect(functionalInterfaceMethodElt);
  }

  /**
   * Test if this tree corresponds to a lambda expression or new class marked as UI affecting by
   * either {@link #constrainLambdaToUI(LambdaExpressionTree) constrainLambdaToUI}} or {@link
   * #constrainAnonymousClassToUI(TypeElement)}. Only explicit markings due to inference are
   * considered here, for the properly computed type of the expression, use {@link
   * #getAnnotatedType(Tree)} instead.
   *
   * @param tree the tree to check
   * @return true if it is a lambda expression or new class marked as UI by inference
   */
  public boolean isDirectlyMarkedUIThroughInference(Tree tree) {
    if (tree instanceof LambdaExpressionTree) {
      return uiLambdas.contains((LambdaExpressionTree) tree);
    } else if (tree instanceof NewClassTree) {
      AnnotatedTypeMirror typeMirror = super.getAnnotatedType(tree);
      if (typeMirror.getKind() == TypeKind.DECLARED) {
        return uiAnonClasses.contains(((DeclaredType) typeMirror.getUnderlyingType()).asElement());
      }
    }
    return false;
  }

  @Override
  public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
    AnnotatedTypeMirror typeMirror = super.getAnnotatedType(tree);
    if (typeMirror.hasPrimaryAnnotation(UI.class)) {
      return typeMirror;
    }
    // Check if this an @UI anonymous class or lambda due to inference, or an expression
    // containing such class/lambda
    if (isDirectlyMarkedUIThroughInference(tree)) {
      typeMirror.replaceAnnotation(AnnotationBuilder.fromClass(elements, UI.class));
    } else if (tree instanceof ParenthesizedTree) {
      ParenthesizedTree parenthesizedTree = (ParenthesizedTree) tree;
      return this.getAnnotatedType(parenthesizedTree.getExpression());
    } else if (tree instanceof ConditionalExpressionTree) {
      ConditionalExpressionTree cet = (ConditionalExpressionTree) tree;
      boolean isTrueOperandUI =
          (cet.getTrueExpression() != null
              && this.getAnnotatedType(cet.getTrueExpression()).hasPrimaryAnnotation(UI.class));
      boolean isFalseOperandUI =
          (cet.getFalseExpression() != null
              && this.getAnnotatedType(cet.getFalseExpression()).hasPrimaryAnnotation(UI.class));
      if (isTrueOperandUI || isFalseOperandUI) {
        typeMirror.replaceAnnotation(AnnotationBuilder.fromClass(elements, UI.class));
      }
    }
    // TODO: Do we need to support other expression here?
    // (i.e. are there any other operators that take new or lambda expressions as operands)
    return typeMirror;
  }

  // Only the visitMethod call should pass true for warnings
  public EffectRange findInheritedEffectRange(
      TypeElement declaringType, ExecutableElement overridingMethod) {
    return findInheritedEffectRange(declaringType, overridingMethod, false, null);
  }

  /**
   * Find the greatest and least effects of methods the specified definition overrides. This method
   * is used for two reasons:
   *
   * <p>1. {@link GuiEffectVisitor#visitMethod(MethodTree,Void) GuiEffectVisitor.visitMethod} calls
   * this to perform an effect override check (that a method's effect is less than or equal to the
   * effect of any method it overrides). This use passes {@code true} for the {@code
   * issueConflictWarning} in order to trigger warning messages.
   *
   * <p>2. {@link #getDeclaredEffect(ExecutableElement) getDeclaredEffect} in this class uses this
   * to infer the default effect of methods in anonymous inner classes. This use passes {@code
   * false} for {@code issueConflictWarning}, because it only needs the return value.
   *
   * @param declaringType the type declaring the override
   * @param overridingMethod the method override itself
   * @param issueConflictWarning if true, issue warnings
   * @param errorTree the method declaration AST node; used for reporting errors
   * @return the min and max inherited effects, or null if none were discovered
   */
  public @Nullable EffectRange findInheritedEffectRange(
      TypeElement declaringType,
      ExecutableElement overridingMethod,
      boolean issueConflictWarning,
      Tree errorTree) {
    assert (declaringType != null);
    ExecutableElement uiOverridden = null;
    ExecutableElement safeOverridden = null;
    ExecutableElement polyOverridden = null;

    // We must account for explicit annotation, type declaration annotations, and package
    // annotations.
    boolean isUI =
        (getDeclAnnotation(overridingMethod, UIEffect.class) != null || isUIType(declaringType))
            && getDeclAnnotation(overridingMethod, SafeEffect.class) == null;
    boolean isPolyUI = getDeclAnnotation(overridingMethod, PolyUIEffect.class) != null;

    // Check for invalid overrides.
    // AnnotatedTypes.overriddenMethods retrieves all transitive definitions overridden by this
    // declaration.
    Map<AnnotatedTypeMirror.AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
        AnnotatedTypes.overriddenMethods(elements, this, overridingMethod);

    for (Map.Entry<AnnotatedTypeMirror.AnnotatedDeclaredType, ExecutableElement> pair :
        overriddenMethods.entrySet()) {
      AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType = pair.getKey();
      AnnotatedTypeMirror.AnnotatedExecutableType overriddenMethod =
          AnnotatedTypes.asMemberOf(types, this, overriddenType, pair.getValue());
      ExecutableElement overriddenMethodElt = pair.getValue();
      if (debugSpew) {
        System.err.println(
            "Found "
                + declaringType
                + "::"
                + overridingMethod
                + " overrides "
                + overriddenType
                + "::"
                + overriddenMethod);
      }
      Effect eff = getDeclaredEffect(overriddenMethodElt);
      if (eff.isSafe()) {
        safeOverridden = overriddenMethodElt;
        if (isUI) {
          checker.reportError(
              errorTree,
              "override.effect",
              declaringType,
              overridingMethod,
              overriddenType,
              safeOverridden);
        } else if (isPolyUI) {
          checker.reportError(
              errorTree,
              "override.effect.polymorphic",
              declaringType,
              overridingMethod,
              overriddenType,
              safeOverridden);
        }
      } else if (eff.isUI()) {
        uiOverridden = overriddenMethodElt;
      } else {
        assert eff.isPoly();
        polyOverridden = overriddenMethodElt;
        if (isUI) {
          // Need to special case an anonymous class with @UI on the decl, because
          //   "new @UI Runnable {...}"
          // parses as @UI on an anon class decl extending Runnable
          boolean isAnonInstantiation =
              isAnonymousType(declaringType)
                  && (fromElement(declaringType).hasPrimaryAnnotation(UI.class)
                      || uiAnonClasses.contains(declaringType));
          if (!isAnonInstantiation && !overriddenType.hasPrimaryAnnotation(UI.class)) {
            checker.reportError(
                errorTree,
                "override.effect.nonui",
                declaringType,
                overridingMethod,
                overriddenType,
                polyOverridden);
          }
        }
      }
    }

    // We don't need to issue warnings for overriding both poly and a concrete effect.
    if (uiOverridden != null && safeOverridden != null && issueConflictWarning) {
      // There may be more than two parent methods, but for now it's
      // enough to know there are at least 2 in conflict.
      checker.reportWarning(
          errorTree,
          "override.effect.warning.inheritance",
          declaringType,
          overridingMethod,
          uiOverridden.getEnclosingElement().asType(),
          uiOverridden,
          safeOverridden.getEnclosingElement().asType(),
          safeOverridden);
    }

    Effect min =
        (safeOverridden != null
            ? new Effect(SafeEffect.class)
            : (polyOverridden != null
                ? new Effect(PolyUIEffect.class)
                : (uiOverridden != null ? new Effect(UIEffect.class) : null)));
    Effect max =
        (uiOverridden != null
            ? new Effect(UIEffect.class)
            : (polyOverridden != null
                ? new Effect(PolyUIEffect.class)
                : (safeOverridden != null ? new Effect(SafeEffect.class) : null)));
    if (debugSpew) {
      System.err.println(
          "Found "
              + declaringType
              + "."
              + overridingMethod
              + " to have inheritance pair ("
              + min
              + ","
              + max
              + ")");
    }

    if (min == null && max == null) {
      return null;
    } else {
      return new EffectRange(min, max);
    }
  }

  @Override
  protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
    return qualHierarchy.getBottomAnnotations();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new GuiEffectTreeAnnotator());
  }

  /**
   * Force the given lambda expression to have UIEffect.
   *
   * <p>Used by GuiEffectVisitor to mark as UIEffect all lambdas that perform UIEffect calls inside
   * their bodies.
   *
   * @param lambdaExpressionTree a lambda expression's AST node
   */
  public void constrainLambdaToUI(LambdaExpressionTree lambdaExpressionTree) {
    uiLambdas.add(lambdaExpressionTree);
  }

  /**
   * Force the given anonymous inner class to be an @UI instantiation of its base class.
   *
   * <p>Used by GuiEffectVisitor to mark as @UI all anonymous inner classes which: inherit from a
   * PolyUIType annotated superclass, override a PolyUIEffect method from said superclass, and
   * perform UIEffect calls inside the body of this method.
   *
   * @param classElt the TypeElement corresponding to the anonymous inner class to mark as an @UI
   *     instantiation of an UI-polymorphic superclass.
   */
  public void constrainAnonymousClassToUI(TypeElement classElt) {
    assert TypesUtils.isAnonymous(classElt.asType());
    uiAnonClasses.add(classElt);
  }

  /** A class for adding annotations based on tree. */
  private class GuiEffectTreeAnnotator extends TreeAnnotator {

    GuiEffectTreeAnnotator() {
      super(GuiEffectTypeFactory.this);
    }

    /*
    public boolean hasExplicitUIEffect(ExecutableElement methElt) {
        return GuiEffectTypeFactory.this.getDeclAnnotation(methElt, UIEffect.class) != null;
    }

    public boolean hasExplicitSafeEffect(ExecutableElement methElt) {
        return GuiEffectTypeFactory.this.getDeclAnnotation(methElt, SafeEffect.class) != null;
    }

    public boolean hasExplicitPolyUIEffect(ExecutableElement methElt) {
        return GuiEffectTypeFactory.this.getDeclAnnotation(methElt, PolyUIEffect.class) != null;
    }

    public boolean hasExplicitEffect(ExecutableElement methElt) {
        return hasExplicitUIEffect(methElt)
                || hasExplicitSafeEffect(methElt)
                || hasExplicitPolyUIEffect(methElt);
    }
    */

    @Override
    public Void visitMethod(MethodTree tree, AnnotatedTypeMirror type) {
      AnnotatedTypeMirror.AnnotatedExecutableType methType =
          (AnnotatedTypeMirror.AnnotatedExecutableType) type;
      // Effect e = getDeclaredEffect(methType.getElement());
      TypeElement cls = (TypeElement) methType.getElement().getEnclosingElement();

      // STEP 1: Get the method effect annotation
      // if (!hasExplicitEffect(methType.getElement())) {
      // TODO: This line does nothing!
      // AnnotatedTypeMirror.addAnnotation silently ignores non-qualifier annotations!
      // We should be digging up the /declaration/ of the method, and annotating that.
      // methType.addAnnotation(e.getAnnot());
      // }

      // STEP 2: Fix up the method receiver annotation
      AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = methType.getReceiverType();
      if (receiverType != null && !receiverType.hasPrimaryAnnotationInHierarchy(UI)) {
        receiverType.addAnnotation(
            isPolymorphicType(cls)
                ? POLYUI
                : fromElement(cls).hasPrimaryAnnotation(UI.class) ? UI : ALWAYSSAFE);
      }
      return super.visitMethod(tree, type);
    }
  }
}
