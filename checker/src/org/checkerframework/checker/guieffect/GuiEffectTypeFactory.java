package org.checkerframework.checker.guieffect;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
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
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

/** Annotated type factory for the GUI Effect Checker. */
public class GuiEffectTypeFactory extends BaseAnnotatedTypeFactory {

    protected final boolean debugSpew;

    public GuiEffectTypeFactory(BaseTypeChecker checker, boolean spew) {
        // use true to enable flow inference, false to disable it
        super(checker, false);

        debugSpew = spew;
        this.postInit();
    }

    // Could move this to a public method on the checker class
    public ExecutableElement findJavaOverride(ExecutableElement overrider, TypeMirror parentType) {
        if (parentType.getKind() != TypeKind.NONE) {
            if (debugSpew) {
                System.err.println("Searching for overridden methods from " + parentType);
            }

            TypeElement overriderClass = (TypeElement) overrider.getEnclosingElement();
            TypeElement elem = (TypeElement) ((DeclaredType) parentType).asElement();
            if (debugSpew) {
                System.err.println("necessary TypeElements acquired: " + elem);
            }

            for (Element e : elem.getEnclosedElements()) {
                if (debugSpew) {
                    System.err.println("Considering element " + e);
                }
                if (e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement ex = (ExecutableElement) e;
                    boolean overrides = elements.overrides(overrider, ex, overriderClass);
                    if (overrides) {
                        return ex;
                    }
                }
            }
            if (debugSpew) {
                System.err.println("Done considering elements of " + parentType);
            }
        }
        return null;
    }

    public boolean isPolymorphicType(TypeElement cls) {
        assert (cls != null);
        return getDeclAnnotation(cls, PolyUIType.class) != null
                || fromElement(cls).hasAnnotation(PolyUI.class);
    }

    public boolean isUIType(TypeElement cls) {
        if (debugSpew) {
            System.err.println(" isUIType(" + cls + ")");
        }
        boolean targetClassUIP = fromElement(cls).hasAnnotation(UI.class);
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
        // they're so often used for closures to run async on background
        // threads.
        if (isAnonymousType(cls)) {
            return false;
        }

        // We don't check polymorphic annos so we can make a couple methods of
        // an @UIType polymorphic explicitly
        // AnnotationMirror targetClassPolyP = getDeclAnnotation(cls, PolyUI.class);
        // AnnotationMirror targetClassPolyTypeP = getDeclAnnotation(cls, PolyUIType.class);
        boolean targetClassSafeP = fromElement(cls).hasAnnotation(AlwaysSafe.class);
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
     * Calling context annotations
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
     *       method is not annotated @AlwaysSafe, and this method resides in an anonymous inner
     *       class (named classes still require a package/class/method annotation to make it UI,
     *       only anon inner classes have this inheritance-by-default)
     *       <ul>
     *         <li>A method must be *annotated* UI if the method it overrides is *annotated* UI
     *         <li>A method must be *annotated* UI if it overrides a UI method and the enclosing
     *             class is not UI
     *       </ul>
     *   <li>It is an error if a method is UI but the same method in a super-type is not UI
     *   <li>It is an error if two super-types specify the same method, where one type says it's UI
     *       and one says it's not (it's possible to simply enforce the weaker (safe) effect, but
     *       this seems more principled, it's easier --- backwards-compatible --- to change our
     *       minds about this later)
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

        if (debugSpew) {
            System.err.println("targetClassElt found");
        }

        // Short-circuit if the method is explicitly annotated
        if (targetSafeP != null) {
            if (debugSpew) {
                System.err.println("Method marked @SafeEffect");
            }
            return new Effect(SafeEffect.class);
        } else if (targetUIP != null) {
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

        // Anonymous inner types should just get the effect of the parent by
        // default, rather than annotating every instance. Unless it's
        // implementing a polymorphic supertype, in which case we still want the
        // developer to be explicit.
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
                Effect.EffectRange r = findInheritedEffectRange(targetClassElt, methodElt);
                return (r != null ? Effect.min(r.min, r.max) : new Effect(SafeEffect.class));
            }
        }

        return new Effect(SafeEffect.class);
    }

    // Only the visitMethod call should pass true for warnings
    public Effect.EffectRange findInheritedEffectRange(
            TypeElement declaringType, ExecutableElement overridingMethod) {
        return findInheritedEffectRange(declaringType, overridingMethod, false, null);
    }

    public Effect.EffectRange findInheritedEffectRange(
            TypeElement declaringType,
            ExecutableElement overridingMethod,
            boolean issueConflictWarning,
            Tree errorNode) {
        assert (declaringType != null);
        ExecutableElement ui_override = null;
        ExecutableElement safe_override = null;
        ExecutableElement poly_override = null;

        // We must account for explicit annotation, type declaration annotations, and package annotations
        boolean isUI =
                (getDeclAnnotation(overridingMethod, UIEffect.class) != null
                                || isUIType(declaringType))
                        && getDeclAnnotation(overridingMethod, SafeEffect.class) == null;
        boolean isPolyUI = getDeclAnnotation(overridingMethod, PolyUIEffect.class) != null;

        // TODO: We must account for @UI and @AlwaysSafe annotations for extends
        // and implements clauses, and do the proper substitution of @Poly effects and quals!
        // List<? extends TypeMirror> interfaces = declaringType.getInterfaces();
        TypeMirror superclass = declaringType.getSuperclass();
        while (superclass != null && superclass.getKind() != TypeKind.NONE) {
            ExecutableElement overrides = findJavaOverride(overridingMethod, superclass);
            if (overrides != null) {
                Effect eff = getDeclaredEffect(overrides);
                assert (eff != null);
                if (eff.isSafe()) {
                    // found a safe override
                    safe_override = overrides;
                    if (isUI && issueConflictWarning) {
                        checker.report(
                                Result.failure(
                                        "override.effect.invalid",
                                        overridingMethod,
                                        declaringType,
                                        safe_override,
                                        superclass),
                                errorNode);
                    }
                    if (isPolyUI && issueConflictWarning) {
                        checker.report(
                                Result.failure(
                                        "override.effect.invalid.polymorphic",
                                        overridingMethod,
                                        declaringType,
                                        safe_override,
                                        superclass),
                                errorNode);
                    }
                } else if (eff.isUI()) {
                    // found a ui override
                    ui_override = overrides;
                } else {
                    assert (eff.isPoly());
                    poly_override = overrides;
                    // TODO: Is this right? is the supertype covered by the
                    // directSuperTypes() method all I need? Or should I be
                    // using that utility method that returns a set of
                    // annodecl-method pairs given a method that overrides stuff
                    // if (isUI && issueConflictWarning) {
                    //    AnnotatedTypeMirror.AnnotatedDeclaredType supdecl = fromElement((TypeElement)(((DeclaredType)superclass).asElement()));//((DeclaredType)superclass).asElement());
                    //    // Need to special case an anonymous class with @UI on the decl, because "new @UI Runnable {...}" parses as @UI on an anon class decl extending Runnable
                    //    boolean isAnonInstantiation = TypesUtils.isAnonymousType(ElementUtils.getType(declaringType)) && getDeclAnnotation(declaringType, UI.class) != null;
                    //    if (!isAnonInstantiation && !hasAnnotationByName(supdecl, UI.class)) {
                    //        checker.report(Result.failure("override.effect.invalid", "non-UI instantiation of "+supdecl), errorNode);
                    //        If uncommenting this, change the above line to match other calls of Result.failure("override.effect.invalid", ...)
                    //    }
                    //}
                }
            }
            DeclaredType decl = (DeclaredType) superclass;
            superclass = ((TypeElement) decl.asElement()).getSuperclass();
        }

        AnnotatedTypeMirror.AnnotatedDeclaredType annoDecl = fromElement(declaringType);
        for (AnnotatedTypeMirror.AnnotatedDeclaredType ty : annoDecl.directSuperTypes()) {
            ExecutableElement overrides =
                    findJavaOverride(overridingMethod, ty.getUnderlyingType());
            if (overrides != null) {
                Effect eff = getDeclaredEffect(overrides);
                if (eff.isSafe()) {
                    // found a safe override
                    safe_override = overrides;
                    if (isUI && issueConflictWarning) {
                        checker.report(
                                Result.failure(
                                        "override.effect.invalid",
                                        overridingMethod,
                                        declaringType,
                                        safe_override,
                                        ty),
                                errorNode);
                    }
                    if (isPolyUI && issueConflictWarning) {
                        checker.report(
                                Result.failure(
                                        "override.effect.invalid.polymorphic",
                                        overridingMethod,
                                        declaringType,
                                        safe_override,
                                        ty),
                                errorNode);
                    }
                } else if (eff.isUI()) {
                    // found a ui override
                    ui_override = overrides;
                } else {
                    assert (eff.isPoly());
                    poly_override = overrides;
                    if (isUI && issueConflictWarning) {
                        AnnotatedTypeMirror.AnnotatedDeclaredType supdecl = ty;
                        // Need to special case an anonymous class with @UI on
                        // the decl, because "new @UI Runnable {...}" parses as
                        // @UI on an anon class decl extending Runnable
                        boolean isAnonInstantiation =
                                isAnonymousType(declaringType)
                                        && fromElement(declaringType).hasAnnotation(UI.class);
                        if (!isAnonInstantiation && !supdecl.hasAnnotation(UI.class)) {
                            checker.report(
                                    Result.failure(
                                            "override.effect.invalid.nonui",
                                            overridingMethod,
                                            declaringType,
                                            poly_override,
                                            supdecl),
                                    errorNode);
                        }
                    }
                }
            }
        }

        // We don't need to issue warnings for inheriting from poly and a concrete effect.
        if (ui_override != null && safe_override != null && issueConflictWarning) {
            // There may be more than two parent methods, but for now it's
            // enough to know there are at least 2 in conflict
            checker.report(
                    Result.warning(
                            "override.effect.warning.inheritance",
                            overridingMethod,
                            declaringType,
                            ui_override.toString(),
                            ui_override.getEnclosingElement().asType().toString(),
                            safe_override.toString(),
                            safe_override.getEnclosingElement().asType().toString()),
                    errorNode);
        }

        Effect min =
                (safe_override != null
                        ? new Effect(SafeEffect.class)
                        : (poly_override != null
                                ? new Effect(PolyUIEffect.class)
                                : (ui_override != null ? new Effect(UIEffect.class) : null)));
        Effect max =
                (ui_override != null
                        ? new Effect(UIEffect.class)
                        : (poly_override != null
                                ? new Effect(PolyUIEffect.class)
                                : (safe_override != null ? new Effect(SafeEffect.class) : null)));
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
            return new Effect.EffectRange(min, max);
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new GuiEffectTreeAnnotator());
    }

    /** A class for adding annotations based on tree. */
    private class GuiEffectTreeAnnotator extends TreeAnnotator {

        GuiEffectTreeAnnotator() {
            super(GuiEffectTypeFactory.this);
        }

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

        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror.AnnotatedExecutableType methType =
                    (AnnotatedTypeMirror.AnnotatedExecutableType) type;
            Effect e = getDeclaredEffect(methType.getElement());
            TypeElement cls = (TypeElement) methType.getElement().getEnclosingElement();

            // STEP 1: Get the method effect annotation
            if (!hasExplicitEffect(methType.getElement())) {
                // TODO: This line does nothing!
                // AnnotatedTypeMirror.addAnnotation silently ignores non-qualifier annotations!
                // We should be digging up the /declaration/ of the method, and annotating that.
                methType.addAnnotation(e.getAnnot());
            }

            // STEP 2: Fix up the method receiver annotation
            AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = methType.getReceiverType();
            if (receiverType != null
                    && !receiverType.isAnnotatedInHierarchy(
                            AnnotationBuilder.fromClass(elements, UI.class))) {
                receiverType.addAnnotation(
                        isPolymorphicType(cls)
                                ? PolyUI.class
                                : fromElement(cls).hasAnnotation(UI.class)
                                        ? UI.class
                                        : AlwaysSafe.class);
            }
            return super.visitMethod(node, type);
        }
    }
}
