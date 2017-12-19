package org.checkerframework.checker.guieffect;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/** Require that only UI code invokes code with the UI effect. */
public class GuiEffectVisitor extends BaseTypeVisitor<GuiEffectTypeFactory> {

    protected final boolean debugSpew;

    // effStack and currentMethods should always be the same size.
    protected final Stack<Effect> effStack;
    protected final Stack<MethodTree> currentMethods;

    public GuiEffectVisitor(BaseTypeChecker checker) {
        super(checker);
        debugSpew = checker.getLintOption("debugSpew", false);
        if (debugSpew) {
            System.err.println("Running GuiEffectVisitor");
        }
        effStack = new Stack<Effect>();
        currentMethods = new Stack<MethodTree>();
    }

    @Override
    protected GuiEffectTypeFactory createTypeFactory() {
        return new GuiEffectTypeFactory(checker, debugSpew);
    }

    // The issue is that the receiver implicitly receives an @AlwaysSafe anno, so calls on @UI
    // references fail because the framework doesn't implicitly upcast the receiver (which in
    // general wouldn't be sound).
    // TODO: Fix method receiver defaults: method-polymorphic for any polymorphic method, UI
    //       for any UI instantiations, safe otherwise
    @Override
    protected void checkMethodInvocability(
            AnnotatedExecutableType method, MethodInvocationTree node) {
        // The inherited version of this complains about invoking methods of @UI instantiations of
        // classes, which by default are annotated @AlwaysSafe, which for data type qualifiers is
        // reasonable, but it not what we want, since we want .
        // TODO: Undo this hack!
    }

    protected class GuiEffectOverrideChecker extends OverrideChecker {
        /**
         * Extend the receiver part of the method override check. We extend the standard check, to
         * additionally permit narrowing the receiver's permission to {@code @AlwaysSafe} in a safe
         * instantiation of a {@code @PolyUIType}. Returns true if the override is permitted.
         */
        @Override
        protected boolean checkReceiverOverride() {
            // We cannot reuse the inherited method because it directly issues the failure, but we
            // want a more permissive check.  So this is copied down and modified from
            // BaseTypeVisitor.OverrideChecker.checkReceiverOverride.
            // isSubtype() requires its arguments to be actual subtypes with
            // respect to JLS, but the overrider receiver is not a subtype of the
            // overridden receiver.  Hence copying the annotations.
            // TODO: Does this need to be improved for generic receivers?  I.e., do we need to
            // add extra checking to reject the case of also changing qualifiers in type parameters?
            // Such as overriding a {@code @PolyUI C<@UI T>} by {@code @AlwaysSafe C<@AlwaysSafe
            // T>}?  The change to the receiver permission is acceptable, while the change to the
            // parameter should be rejected.
            AnnotatedTypeMirror overriddenReceiver =
                    overrider.getReceiverType().getErased().shallowCopy(false);
            overriddenReceiver.addAnnotations(overridden.getReceiverType().getAnnotations());
            if (!atypeFactory
                    .getTypeHierarchy()
                    .isSubtype(overriddenReceiver, overrider.getReceiverType().getErased())) {
                // This is the point at which the default check would issue an error.
                // We additionally permit overrides to move from @PolyUI receivers to @AlwaysSafe
                // receivers, if it's in a @AlwaysSafe specialization of a @PolyUIType
                boolean safeParent = overriddenType.getAnnotation(AlwaysSafe.class) != null;
                boolean polyParentDecl =
                        atypeFactory.getDeclAnnotation(
                                        overriddenType.getUnderlyingType().asElement(),
                                        PolyUIType.class)
                                != null;
                // TODO: How much validation do I need here?  Do I need to check that the overridden
                // receiver was really @PolyUI and the method is really an @PolyUIEffect?  I don't
                // think so - we know it's a polymorphic parent type, so all receivers would be
                // @PolyUI.
                // Java would already reject before running type annotation processors if the Java
                // types were wrong.
                // The *only* extra leeway we want to permit is overriding @PolyUI receiver to
                // @AlwaysSafe.  But with generics, the tentative check below is inadequate.
                boolean safeReceiverOverride =
                        overrider.getReceiverType().getAnnotation(AlwaysSafe.class) != null;
                if (safeParent && polyParentDecl && safeReceiverOverride) {
                    return true;
                }
                checker.report(
                        Result.failure(
                                "override.receiver.invalid",
                                overriderMeth,
                                overriderTyp,
                                overriddenMeth,
                                overriddenTyp,
                                overrider.getReceiverType(),
                                overridden.getReceiverType()),
                        overriderTree);
                return false;
            }
            return true;
        }

        public GuiEffectOverrideChecker(
                Tree overriderTree,
                AnnotatedTypeMirror.AnnotatedExecutableType overrider,
                AnnotatedTypeMirror overridingType,
                AnnotatedTypeMirror overridingReturnType,
                AnnotatedExecutableType overridden,
                AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType,
                AnnotatedTypeMirror overriddenReturnType) {
            super(
                    overriderTree,
                    overrider,
                    overridingType,
                    overridingReturnType,
                    overridden,
                    overriddenType,
                    overriddenReturnType);
        }
    }

    @Override
    protected OverrideChecker createOverrideChecker(
            Tree overriderTree,
            AnnotatedExecutableType overrider,
            AnnotatedTypeMirror overridingType,
            AnnotatedTypeMirror overridingReturnType,
            AnnotatedExecutableType overridden,
            AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType,
            AnnotatedTypeMirror overriddenReturnType) {
        return new GuiEffectOverrideChecker(
                overriderTree,
                overrider,
                overridingType,
                overridingReturnType,
                overridden,
                overriddenType,
                overriddenReturnType);
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        return Collections.singleton(AnnotationBuilder.fromClass(elements, AlwaysSafe.class));
    }

    @Override
    public boolean isValidUse(
            AnnotatedTypeMirror.AnnotatedDeclaredType declarationType,
            AnnotatedTypeMirror.AnnotatedDeclaredType useType,
            Tree tree) {
        boolean ret =
                useType.hasAnnotation(AlwaysSafe.class)
                        || useType.hasAnnotation(PolyAll.class)
                        || useType.hasAnnotation(PolyUI.class)
                        || atypeFactory.isPolymorphicType(
                                (TypeElement) declarationType.getUnderlyingType().asElement())
                        || (useType.hasAnnotation(UI.class)
                                && declarationType.hasAnnotation(UI.class));
        if (debugSpew && !ret) {
            System.err.println("use: " + useType);
            System.err.println("use safe: " + useType.hasAnnotation(AlwaysSafe.class));
            System.err.println("use poly: " + useType.hasAnnotation(PolyUI.class));
            System.err.println("use ui: " + useType.hasAnnotation(UI.class));
            System.err.println(
                    "declaration safe: " + declarationType.hasAnnotation(AlwaysSafe.class));
            System.err.println(
                    "declaration poly: "
                            + atypeFactory.isPolymorphicType(
                                    (TypeElement) declarationType.getUnderlyingType().asElement()));
            System.err.println("declaration ui: " + declarationType.hasAnnotation(UI.class));
            System.err.println("declaration: " + declarationType);
        }
        return ret;
    }

    // For assignments and local variable initialization:
    // Check for @UI annotations on the lhs, use them to infer @UI types on lambda expressions in the rhs
    private void lambdaAssignmentCheck(
            AnnotatedTypeMirror varType,
            LambdaExpressionTree lambdaExp,
            @CompilerMessageKey String errorKey) {
        AnnotatedTypeMirror lambdaType = atypeFactory.getAnnotatedType(lambdaExp);
        assert lambdaType != null : "null type for expression: " + lambdaExp;
        commonAssignmentCheck(varType, lambdaType, lambdaExp, errorKey);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        Void result = super.visitVariable(node, p);
        if (node.getInitializer() != null
                && node.getInitializer().getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
            lambdaAssignmentCheck(
                    atypeFactory.getAnnotatedType(node),
                    (LambdaExpressionTree) node.getInitializer(),
                    "assignment.type.incompatible");
        }
        return result;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        Void result = super.visitAssignment(node, p);
        if (node.getExpression().getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
            lambdaAssignmentCheck(
                    atypeFactory.getAnnotatedType(node.getVariable()),
                    (LambdaExpressionTree) node.getExpression(),
                    "assignment.type.incompatible");
        }
        return result;
    }

    // Returning a lambda expression also triggers inference based on the method's return type.
    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        Void result = super.visitReturn(node, p);
        if (node.getExpression() != null
                && node.getExpression().getKind() == Tree.Kind.LAMBDA_EXPRESSION) {

            // Unfortunatelly, need to duplicate a fair bit of BaseTypeVisitor.visitReturn after lambdas have been
            // inferred.
            Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
            try {
                Tree enclosing = TreeUtils.enclosingMethodOrLambda(getCurrentPath());
                AnnotatedTypeMirror ret = null;
                if (enclosing.getKind() == Tree.Kind.METHOD) {
                    MethodTree enclosingMethod = (MethodTree) enclosing;
                    boolean valid = validateTypeOf(enclosing);
                    if (valid) {
                        ret = atypeFactory.getMethodReturnType(enclosingMethod, node);
                    }
                } else {
                    ret =
                            atypeFactory
                                    .getFnInterfaceFromTree((LambdaExpressionTree) enclosing)
                                    .second
                                    .getReturnType();
                }

                if (ret != null) {
                    visitorState.setAssignmentContext(Pair.of((Tree) node, ret));
                    lambdaAssignmentCheck(
                            ret,
                            (LambdaExpressionTree) node.getExpression(),
                            "return.type.incompatible");
                }
            } finally {
                visitorState.setAssignmentContext(preAssCtxt);
            }
        }
        return result;
    }

    // Check that the invoked effect is <= permitted effect (effStack.peek())
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (debugSpew) {
            System.err.println("For invocation " + node + " in " + currentMethods.peek().getName());
        }

        // Target method annotations
        ExecutableElement methodElt = TreeUtils.elementFromUse(node);
        if (debugSpew) {
            System.err.println("methodElt found");
        }

        Tree callerTree = TreeUtils.enclosingMethodOrLambda(getCurrentPath());
        if (callerTree == null) {
            // Static initializer; let's assume this is safe to have the UI effect
            if (debugSpew) {
                System.err.println("No enclosing method: likely static initializer");
            }
            return super.visitMethodInvocation(node, p);
        }
        if (debugSpew) {
            System.err.println("callerTree found: " + callerTree.getKind());
        }

        Effect targetEffect =
                atypeFactory.getComputedEffectAtCallsite(
                        node, visitorState.getMethodReceiver(), methodElt);

        Effect callerEffect = null;
        if (callerTree.getKind() == Tree.Kind.METHOD) {
            ExecutableElement callerElt = TreeUtils.elementFromDeclaration((MethodTree) callerTree);
            if (debugSpew) {
                System.err.println("callerElt found");
            }

            callerEffect = atypeFactory.getDeclaredEffect(callerElt);
            // Field initializers inside anonymous inner classes show up with a null current-method ---
            // the traversal goes straight from the class to the initializer.
            assert (currentMethods.peek() == null || callerEffect.equals(effStack.peek()));
        } else if (callerTree.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
            callerEffect =
                    atypeFactory.getInferedEffectForLambdaExpression(
                            (LambdaExpressionTree) callerTree);
            // Perform lambda polymorphic effect inference: @PolyUI lambda, calling @UIEffect => @UI lambda
            if (targetEffect.isUI() && callerEffect.isPoly()) {
                atypeFactory.constrainLambdaToUI((LambdaExpressionTree) callerTree);
                callerEffect = new Effect(UIEffect.class);
            }
        }
        assert callerEffect != null;

        if (!Effect.LE(targetEffect, callerEffect)) {
            checker.report(Result.failure("call.invalid.ui", targetEffect, callerEffect), node);
            if (debugSpew) {
                System.err.println("Issuing error for node: " + node);
            }
        }
        if (debugSpew) {
            System.err.println(
                    "Successfully finished main non-recursive checkinv of invocation " + node);
        }

        Void result = super.visitMethodInvocation(node, p);

        // Check arguments to this method invocation for UI-lambdas, this must be re-checked after visiting the lambda
        // body due to inference.
        List<? extends ExpressionTree> args = node.getArguments();
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =
                atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.first;
        List<? extends VariableElement> parameters = methodElt.getParameters();
        List<AnnotatedTypeMirror> argsTypes =
                AnnotatedTypes.expandVarArgs(atypeFactory, invokedMethod, node.getArguments());
        for (int i = 0; i < args.size(); ++i) {
            if (args.get(i).getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
                lambdaAssignmentCheck(
                        argsTypes.get(i),
                        (LambdaExpressionTree) args.get(i),
                        "argument.type.incompatible");
            }
        }

        return result;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        // TODO: If the type we're in is a polymorphic (over effect qualifiers) type, the receiver
        // must be @PolyUI.  Otherwise a "non-polymorphic" method of a polymorphic type could be
        // called on a UI instance, which then gets a Safe reference to itself (unsound!) that it
        // can then pass off elsewhere (dangerous!).  So all receivers in methods of a @PolyUIType
        // must be @PolyUI.

        // TODO: What do we do then about classes that inherit from a concrete instantiation?  If it
        // subclasses a Safe instantiation, all is well.  If it subclasses a UI instantiation, then
        // the receivers should probably be @UI in both new and override methods, so calls to
        // polymorphic methods of the parent class will work correctly.  In which case for proving
        // anything, the qualifier on sublasses of UI instantiations would always have to be
        // @UI... Need to write down |- t for this system!  And the judgments for method overrides
        // and inheritance!  Those are actually the hardest part of the system.

        ExecutableElement methElt = TreeUtils.elementFromDeclaration(node);
        if (debugSpew) {
            System.err.println("\nVisiting method " + methElt);
        }

        // Check for conflicting (multiple) annotations
        assert (methElt != null);
        // TypeMirror scratch = methElt.getReturnType();
        AnnotationMirror targetUIP = atypeFactory.getDeclAnnotation(methElt, UIEffect.class);
        AnnotationMirror targetSafeP = atypeFactory.getDeclAnnotation(methElt, SafeEffect.class);
        AnnotationMirror targetPolyP = atypeFactory.getDeclAnnotation(methElt, PolyUIEffect.class);
        TypeElement targetClassElt = (TypeElement) methElt.getEnclosingElement();

        if ((targetUIP != null && (targetSafeP != null || targetPolyP != null))
                || (targetSafeP != null && targetPolyP != null)) {
            checker.report(Result.failure("annotations.conflicts"), node);
        }
        if (targetPolyP != null && !atypeFactory.isPolymorphicType(targetClassElt)) {
            checker.report(Result.failure("polymorphism.invalid"), node);
        }
        if (targetUIP != null && atypeFactory.isUIType(targetClassElt)) {
            checker.report(Result.warning("effects.redundant.uitype"), node);
        }

        // TODO: Report an error for polymorphic method bodies??? Until we fix the receiver
        // defaults, it won't really be correct
        @SuppressWarnings("unused") // call has side-effects
        Effect.EffectRange range =
                atypeFactory.findInheritedEffectRange(
                        ((TypeElement) methElt.getEnclosingElement()), methElt, true, node);
        // if (targetUIP == null && targetSafeP == null && targetPolyP == null) {
        // implicitly annotate this method with the LUB of the effects of the methods it
        // overrides
        // atypeFactory.fromElement(methElt).addAnnotation(range != null ? range.min.getAnnot()
        // : (isUIType(((TypeElement)methElt.getEnclosingElement())) ? UI.class :
        // AlwaysSafe.class));
        // TODO: This line does nothing! AnnotatedTypeMirror.addAnnotation
        // silently ignores non-qualifier annotations!
        // System.err.println("ERROR: TREE ANNOTATOR SHOULD HAVE ADDED EXPLICIT ANNOTATION! ("
        //     +node.getName()+")");
        // atypeFactory
        //         .fromElement(methElt)
        //         .addAnnotation(atypeFactory.getDeclaredEffect(methElt).getAnnot());
        // }

        // We hang onto the current method here for ease.  We back up the old
        // current method because this code is reentrant when we traverse methods of an inner class
        currentMethods.push(node);
        // effStack.push(targetSafeP != null ? new Effect(AlwaysSafe.class) :
        //                (targetPolyP != null ? new Effect(PolyUI.class) :
        //                   (targetUIP != null ? new Effect(UI.class) :
        //                      (range != null ? range.min :
        // (isUIType(((TypeElement)methElt.getEnclosingElement())) ? new Effect(UI.class) : new
        // Effect(AlwaysSafe.class))))));
        effStack.push(atypeFactory.getDeclaredEffect(methElt));
        if (debugSpew) {
            System.err.println(
                    "Pushing " + effStack.peek() + " onto the stack when checking " + methElt);
        }

        Void ret = super.visitMethod(node, p);
        currentMethods.pop();
        effStack.pop();
        return ret;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        // TODO: Same effect checks as for methods
        return super.visitMemberSelect(node, p);
    }

    @Override
    public void processClassTree(ClassTree node) {
        // TODO: Check constraints on this class decl vs. parent class decl., and interfaces
        // TODO: This has to wait for now: maybe this will be easier with the isValidUse on the
        // TypeFactory
        // AnnotatedTypeMirror.AnnotatedDeclaredType atype = atypeFactory.fromClass(node);

        // Push a null method and UI effect onto the stack for static field initialization
        // TODO: Figure out if this is safe! For static data, almost certainly,
        // but for statically initialized instance fields, I'm assuming those
        // are implicitly moved into each constructor, which must then be @UI
        currentMethods.push(null);
        effStack.push(new Effect(UIEffect.class));
        super.processClassTree(node);
        currentMethods.pop();
        effStack.pop();
    }
}
