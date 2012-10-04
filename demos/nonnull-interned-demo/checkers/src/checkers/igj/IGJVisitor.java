package checkers.igj;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

import checkers.igj.quals.*;
import checkers.source.*;
import checkers.types.*;
import checkers.util.*;

import static checkers.igj.IGJImmutability.*;

/**
 * A type-checking visitor for the IGJ mutability annotations (
 * {@code @ReadOnly}, {@code @Immutable}, {@code @Mutable},
 * {@code @AssignsFields}, and {@code @Assignable})
 * 
 */
public class IGJVisitor extends SourceVisitor<Void, VisitorState> {

    private IGJChecker checker;
    private TypesUtils typesUtils;
    private IGJAnnotatedTypeFactory factory;

    public IGJVisitor(IGJChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.checker = checker;
        this.factory =
            new IGJAnnotatedTypeFactory(checker.getProcessingEnvironment(),
                    root);
        typesUtils = new TypesUtils(checker.getProcessingEnvironment());
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, VisitorState state) {
        commonAssignmentRule(tree.getVariable(), tree.getExpression(), state);
        return super.visitAssignment(tree, state);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree,
            VisitorState state) {
        commonAssignmentRule(tree.getVariable(), tree.getExpression(), state);
        return super.visitCompoundAssignment(tree, state);
    }

    @Override
    public Void visitUnary(UnaryTree tree, VisitorState state) {
        // Check if need to reassign
        switch (tree.getKind()) {
        case POSTFIX_DECREMENT:
        case POSTFIX_INCREMENT:
        case PREFIX_DECREMENT:
        case PREFIX_INCREMENT:
            // Try to assign to oneself?
            commonAssignmentRule(tree.getExpression(), tree.getExpression(),
                    state);
        }
        return super.visitUnary(tree, state);
    }

    @Override
    public Void visitVariable(VariableTree tree, VisitorState state) {
        AnnotatedClassType varType = factory.getClass(tree);
        checkTypeImmutability(varType, tree.getType());

        // Static fields are not assignable
        if (!(varType.getAnnotationData(Assignable.class, true).isEmpty())
                && (ElementUtils.isStatic(varType.getElement()))) {
            checker.report(Result.failure("assignable.invalid"), tree);
        }

        if (tree.getInitializer() != null)
            commonAssignmentRule(tree, tree.getInitializer(), state);
        
        return super.visitVariable(tree, state);
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, VisitorState state) {

        // check that expression is a subtype of type
        AnnotatedClassType classType = factory.getClass(tree);
        checkTypeImmutability(classType, tree.getType());
        if (!isSubtype(factory.getClass(tree.getExpression()), classType))
            checker.report(Result.failure("cast.invalid"), tree.getType());

        return super.visitTypeCast(tree, state);
    }

    @Override
    public Void visitClass(ClassTree tree, VisitorState state) {
        AnnotatedClassType type = factory.getClass(tree);
        
        if (tree.getExtendsClause() != null) {
            checkTypeImmutability(factory.getClass(tree.getExtendsClause()),
                    tree.getExtendsClause());
        }
        
        for (Tree t : tree.getImplementsClause()) {
            checkTypeImmutability(factory.getClass(t), t);
        }
        
        if (state == null) {
            state = new VisitorState();
            factory.setVisitorState(state);
        }

        IGJImmutability temp = state.thisImmutability;

        state.thisImmutability =
            IGJImmutability
                    .getIGJImmutabilityAt(type, AnnotationLocation.RAW);
        try {
            return super.visitClass(tree, state);
        } finally {
            state.thisImmutability = temp;
        }
    }

    @Override
    public Void visitNewClass(NewClassTree tree, VisitorState state) {

        // TODO: Handle new classes within instance
        AnnotatedClassType newClass = factory.getClass(tree);
                
        // Check parameters
        List<? extends ExpressionTree> passedParams = tree.getArguments();
        
        AnnotatedMethodType constructor = factory.getMethod(tree);

        List<AnnotatedClassType> requiredParams = 
            constructor.getAnnotatedParameterTypes();
        
        assert passedParams.size() == requiredParams.size();

        for (int i = 0; i < passedParams.size(); ++i) {
            AnnotatedClassType passedType =
                factory.getClass(passedParams.get(i));
            if (!isSubtype(passedType, requiredParams.get(i))) {
                checker.report(Result.failure("param.invalid",
                        typesUtils.toString(passedType.getUnderlyingType(), passedType.getAnnotationData(true)),
                        typesUtils.toString(requiredParams.get(i).getUnderlyingType(), requiredParams.get(i).getAnnotationData(true)))
                        , passedParams.get(i));
            }
        }

        // Check return value!
        if (!isSubtype(constructor.getAnnotatedReturnType(), newClass)) {
            checker.report(Result.failure("type.invalid", 
                    typesUtils.toString(trees.getTypeMirror(getCurrentPath()), newClass.getAnnotationData(true)),
                    // TODO: Check type!
                    typesUtils.toString(trees.getTypeMirror(getCurrentPath()), constructor.getAnnotatedReturnType().getAnnotationData(true))),
                    tree);
        }
        return super.visitNewClass(tree, state);
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
        // TODO: Handle arrays!
        // FIXME: This is cheating. 

        AnnotatedClassType iteratedType = new AnnotatedClassType(checker.getProcessingEnvironment());
        {
            AnnotationLocation iterableLoc = AnnotationLocation.fromArray(new int[] { 0 });
            AnnotatedClassType iterableType = factory.getClass(tree.getExpression());
        
            for (AnnotationData anno : iterableType.getAnnotationData(true))
                if (anno.getLocation().isSubtreeOf(iterableLoc))
                    iteratedType.include(AnnotationLocation.asSubOf(anno, iterableLoc, checker.getProcessingEnvironment()));
            iteratedType.setUnderlyingType(new GenericsUtils(checker.getProcessingEnvironment(), factory)
            .iteratedType(iterableType.getUnderlyingType()));
        }
        
        AnnotatedClassType variable = factory.getClass(tree.getVariable());
        
        if (!isSubtype(iteratedType, variable)) {
            checker.report(Result.failure("assignment.invalid",
                    typesUtils.toString(iteratedType.getUnderlyingType()
                            , iteratedType.getAnnotationData(true)),
                    typesUtils.toString(variable.getUnderlyingType(), variable.getAnnotationData(true)))
                    , tree.getExpression());
        }
        
        return super.visitEnhancedForLoop(tree, state);
    }
    
    @Override
    public Void visitReturn(ReturnTree tree, VisitorState state) {
        // Check if return for a void method
        if (tree.getExpression() == null)
            return super.visitReturn(tree, state);

        // check if the return value is a subtype of the required
        // return
        AnnotatedClassType requiredReturnType =
            factory.getMethod(TreeUtils.enclosingMethod(this.getCurrentPath()))
            .getAnnotatedReturnType();

        AnnotatedClassType actualReturnType =
            factory.getClass(tree.getExpression());


        if (requiredReturnType.getUnderlyingType().getKind() != TypeKind.TYPEVAR) {
            if (!isSubtype(actualReturnType, requiredReturnType))
                checker.report(Result.failure("assignment.invalid", 
                        typesUtils.toString(actualReturnType.getUnderlyingType(), actualReturnType.getAnnotationData(true)),
                        typesUtils.toString(requiredReturnType.getUnderlyingType(), requiredReturnType.getAnnotationData(true)))
                        , tree.getExpression());
        }

        return super.visitReturn(tree, state);
    }

    protected Result isValidImmutability(AnnotatedClassType classType,
            AnnotationData instanceAnnotation) {

        // Check if it is primitive first
        if (classType.getElement() == null || checker.shouldSkip(classType) ||
                classType.getElement().asType().getKind() == TypeKind.TYPEVAR) {
            // No elements for primitive types!
            return Result.SUCCESS;
        }

        // For now check AnnotationLocation.RAW
        AnnotationData annotation =
            getIGJAnnotationDataAt(classType,
                    AnnotationLocation.RAW);

        if ((annotation != null) && !isWildcard(annotation)) {
            if (IGJImmutability.isSubtype(annotation, instanceAnnotation))
                return Result.SUCCESS;

            String required = 
                typesUtils.toString(classType.getUnderlyingType(),
                        Collections.singleton(getIGJAnnotationDataAt(classType, AnnotationLocation.RAW)));
            String found =
                typesUtils.toString(classType.getUnderlyingType(),
                        Collections.singleton(instanceAnnotation));
            return Result.failure("type.invalid", found, required);
        }
        return Result.SUCCESS;
    }

    /*
     * This method needs significant refactoring and testing.
     * 
     * CAUTION: This method shouldn't be called from visitParametrizedType
     */
    protected void checkTypeImmutability(AnnotatedClassType classType, Tree typeTree) {
        TreePath path = TreePath.getPath(this.root, typeTree);
        TypeMirror fullType = trees.getTypeMirror(path);

        if (fullType.getKind() == TypeKind.ARRAY)
            return;
        
        for (AnnotationData annotation : getIGJAnnotationData(classType)) {
         // Check that it is a valid type
            
            TypeMirror typeForAnno = annotation.getLocation().getTypeFrom(fullType);
            if (typeForAnno == null || typeForAnno.getKind() != TypeKind.DECLARED)
                // Somehow we can have an annotation on non existing type!
                continue;
            
            Element elem = ((DeclaredType)typeForAnno).asElement();
            if (!isValidImmutability(factory.getClass(elem), annotation).isSuccess())
                checker.report(isValidImmutability(factory.getClass(elem), annotation),
                        annotation.getLocation().getTypeFrom(typeTree));
        }
    }
    
    @Override
    public Void visitMethod(MethodTree tree, VisitorState state) {
        AnnotatedMethodType method = factory.getMethod(tree);
        // ignore checking params as they are VariableTrees
        if (tree.getReturnType() != null) {
            checkTypeImmutability(method.getAnnotatedReturnType(),
                    tree.getReturnType());
        }
        
        // Check overriding
        for (ExecutableElement overridenMethod : typesUtils
                .overriddenMethods(method.getElement())) {
            checkOverride(method.getElement(), overridenMethod);
        }

        // Check validity of receiver type
        AnnotatedClassType encClass = factory.getClass(
                TreeUtils.enclosingClass(this.getCurrentPath()));

        AnnotatedClassType receiverType = method.getAnnotatedReceiverType();
        if (!encClass.hasAnnotationAt(I.class, AnnotationLocation.RAW)) {
            if (!isSubtypeAt(encClass, receiverType, AnnotationLocation.RAW))
                checker.report(Result.failure("method.receiver.invalid"), 
                        tree);
        }
        
        IGJImmutability temp = state.thisImmutability;
        state.thisImmutability =
            getIGJImmutabilityAt(receiverType, 
                    AnnotationLocation.RAW);
        try {
            return super.visitMethod(tree, state);
        } finally {
            state.thisImmutability = temp;
        }
    }

    /**
     * Tests if the overrider method overrides the overriden method properly
     * 
     * @param overrider the overrider method
     * @param overriden the overriden method
     * @return  true iff the overrider method <: overriden method
     */
    private boolean checkOverride(ExecutableElement overrider,
            ExecutableElement overriden) {
        // It's harder to implement a sanity check for overriding here
        assert (overrider.getParameters().size() == overriden.getParameters()
                .size());
        {
            String className =
              InternalUtils.getQualifiedName(overriden.getEnclosingElement());
            if (checker.shouldSkip(className))
                return true;
        }

        boolean valid = true;

        AnnotatedMethodType method = factory.getMethod(overrider);
        AnnotatedMethodType superMethod = factory.getMethod(overriden);

        // Check return type
        if (!isSubtype(method.getAnnotatedReturnType(), superMethod
                .getAnnotatedReturnType())) {
            // Report error
            checker.report(Result.failure("override.return.invalid",
                    overrider.toString(),
                    overrider.getEnclosingElement().toString(),
                    overriden.toString(),
                    overriden.getEnclosingElement().toString(),
                    typesUtils.toString(overrider.getReturnType(), method.getAnnotatedReturnType().getAnnotationData(true)),
                    typesUtils.toString(overriden.getReturnType(), superMethod.getAnnotatedReturnType().getAnnotationData(true))),
                    method.getAnnotatedReturnType().getTree());
            valid = false;
        }

        // Check parameters
        List<AnnotatedClassType> methodParams =
            method.getAnnotatedParameterTypes();
        List<AnnotatedClassType> superMethodParams =
            superMethod.getAnnotatedParameterTypes();

        for (int i = 0; i < methodParams.size(); ++i) {

            // The parameters in the OVERRIDEN method need to be
            // subtypes of the OVERRIDING one
            if (!isSubtype(superMethodParams.get(i), methodParams.get(i))) {
                checker.report(Result.failure("override.param.invalid",
                        overrider.toString(),
                        overrider.getEnclosingElement().toString(),
                        overriden.toString(),
                        overriden.getEnclosingElement().toString(),
                        typesUtils.toString(overriden.getParameters().get(i).asType(), 
                                superMethodParams.get(i).getAnnotationData(true)),
                        typesUtils.toString(overrider.getParameters().get(i).asType(), 
                                methodParams.get(i).getAnnotationData(true))),
                        methodParams.get(i).getTree());
                valid = false;
            }
        }
        // Check receiver
        if (!isSubtype(superMethod.getAnnotatedReceiverType(), method
                .getAnnotatedReceiverType())) {
            checker.report(Result.failure("override.receiver.invalid",
                    overrider.toString(),
                    overrider.getEnclosingElement().toString(),
                    overriden.toString(),
                    overriden.getEnclosingElement().toString(),
                    typesUtils.toString(method.getAnnotatedReceiverType().getAnnotationData(true)),
                    typesUtils.toString(superMethod.getAnnotatedReceiverType().getAnnotationData(true))),
                    method.getAnnotatedReceiverType().getTree());
            valid = false;
        }
        return valid;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree,
            VisitorState state) {
        // Test IGJ Method invocation test
        if (!applyMethodRule(tree, state.thisImmutability)) {
            checker.report(Result.failure("methodinvocation.invalid",
                    getIGJImmutabilityAt(factory.getMethod(tree).getAnnotatedReceiverType(), 
                            AnnotationLocation.RAW).toString().toLowerCase(),
                    state.thisImmutability.toString().toLowerCase()), tree);
        }
        // Test the parameters of the method
        List<? extends Tree> passedParams = tree.getArguments();
        AnnotatedMethodType method = factory.getMethod(tree);
        if (checker.shouldSkip(method))
            return super.visitMethodInvocation(tree, state);

        List<AnnotatedClassType> requiredParams =
            method.getAnnotatedParameterTypes();
        assert passedParams.size() == requiredParams.size();

        for (int i = 0; i < passedParams.size(); ++i) {
            AnnotatedClassType passedType =
                factory.getClass(passedParams.get(i));
            if (!isSubtype(passedType, requiredParams.get(i))) {
                checker.report(Result.failure("param.invalid",
                        typesUtils.toString(passedType.getUnderlyingType(), passedType.getAnnotationData(true)),
                        typesUtils.toString(requiredParams.get(i).getUnderlyingType(), requiredParams.get(i).getAnnotationData(true))),
                        passedParams.get(i));
            }
        }
        return super.visitMethodInvocation(tree, state);
    }

    // Rules

    /**
     * 
     * Tests whether the value can be assigned to the variable, given
     * the immutability context represented in state. If any errors
     * are found, it reports them using the checkers framework
     * 
     * The method does immutability context testing, in addition to
     * subtype checking.
     * 
     * @param variableTree
     *            a {@code Tree} for the variable to be assigned
     * @param valueTree
     *            a {@code Tree} for the value to be assigned
     * @param state
     *            a {@VisitorState} that encapsulate the IGJ state of
     *            the context
     */
    protected boolean commonAssignmentRule(Tree variableTree, Tree valueTree,
            VisitorState state) {
        AnnotatedClassType variable = factory.getClass(variableTree);

        if ((variable.getElement().getKind() == ElementKind.FIELD)
                && (!applyFieldAssignmentRule(variable,
                        state.thisImmutability))) {
            checker.report(Result.failure("assignment.invalid.field"),
                    variableTree);
            return false;
        }

        if (variable.getElement().getKind() == ElementKind.METHOD) {
            // Assume it's a method for an annotation
            // method aren't Left assignment value
            return true;
        }

        AnnotatedClassType value = factory.getClass(valueTree);
        if (!isSubtype(value, variable)) {
            checker.report(Result.failure("assignment.invalid",
                    typesUtils.toString(value.getUnderlyingType(), value.getAnnotationData(true)),
                    typesUtils.toString(variable.getUnderlyingType(), variable.getAnnotationData(true)))
                    , valueTree);
            return false;
        }

        // TODO: Test this more
        if (variableTree.getKind() == Tree.Kind.ARRAY_ACCESS) {
            Tree arrayTree = ((ArrayAccessTree)variableTree).getExpression();
            AnnotatedClassType array = factory.getClass(arrayTree);
            if (array.hasAnnotationAt(ReadOnly.class, AnnotationLocation.RAW) ||
                    array.hasAnnotationAt(Immutable.class, AnnotationLocation.RAW)) {
                checker.report(Result.failure("array.mutable.invalid"), arrayTree);
            }
        }
        
        return true;
    }

    /**
     * Checks if the child type is a subtype of the parent type,
     * according to IGJ Rules
     * 
     * @param child
     * @param parent
     * @return true iff child is subtype of parent
     */
    protected boolean isSubtype(AnnotatedClassType child,
            AnnotatedClassType parent) {
        // Check if should skip class

        // TODO: Handle this better
        if (parent.getUnderlyingType().getKind() == TypeKind.TYPEVAR)
            return true;

        for (AnnotationLocation location : child.getAnnotatedLocations()) {
            if (!isSubtypeAt(child, parent, location))
                return false;
        }

        return true;
    }

    /**
     * Checks whether child {@code AnnotatedClassType} is a subtype of
     * parent {@code AnnotatedClassType} in the given location.
     * 
     * @param child
     * @param parent
     * @param location
     * 
     * @return {@code true} if child is a subtype of parent at a given
     *         location
     */
    protected boolean isSubtypeAt(AnnotatedClassType child,
            AnnotatedClassType parent, AnnotationLocation location) {
        // Check if should skip class
        if (child.getElement() != null) {
            Element childElt = child.getElement();

            String className = InternalUtils.getQualifiedName(childElt);
            if (checker.shouldSkip(className))
                return true;
        }

        // TODO : Handle Generics

        AnnotationData childImmutability =
            getIGJAnnotationDataAt(child, location);
        AnnotationData parentImmutability =
            getIGJAnnotationDataAt(parent, location);
        if (childImmutability == null || parentImmutability == null) {
            return false;
        }
        return IGJImmutability.isSubtype(childImmutability,
                parentImmutability);
    }

    // IGJ Rules!!!
    /**
     * Applies the field assignment according to IGJ Rule
     * 
     * o.someField = ... is valid iff I(o) = Mutable or (I(o) =
     * AssignsFields and o = {@code this})
     * 
     * Exceptions: 1. Static Fields may be reassigned. 2. Assignable
     * fields may be reassigned all the time
     * 
     */
    protected boolean applyFieldAssignmentRule(AnnotatedClassType type,
            IGJImmutability immutability) {
        // o.someField = ... is legal iff
        // I(o) = Mutable or (I(o) = AssignsFields and o = this)
        //

        if (!type.getElement().getKind().isField())
            throw new RuntimeException("type passed is not for a field");

        // TODO: Ask about static fields
        if (ElementUtils.isStatic(type.getElement())
                || type.getTree().getKind() == Tree.Kind.VARIABLE
                || type.hasAnnotationAt(Assignable.class, AnnotationLocation.RAW))
            return true;

        if (TreeUtils.isSelfAccess(type.getTree())) {
            // Mutating one-self
            return ((immutability == MUTABLE) || immutability == ASSIGNSFIELDS);
        } else {
            // Mutating another method
            // Trying to mutate a different instance
            assert (type.getTree().getKind() == Tree.Kind.MEMBER_SELECT);
            MemberSelectTree memberSelectTree =
                (MemberSelectTree) type.getTree();
            AnnotatedClassType otherClass =
                factory.getClass(memberSelectTree.getExpression());
            return isMutableAt(otherClass,
                    AnnotationLocation.RAW);
        }
    }

    /**
     * Apply the IGJ method rule for method invocation calls
     * 
     * o.m(...) is legal iff I(o) = Mutable or (I(m) = AssignsFields
     * and o = this)
     * 
     * @param tree
     * @param thisImmutability
     * @return true iff the method invocation is valid within this
     *         scope
     */
    protected boolean applyMethodRule(MethodInvocationTree tree,
            IGJImmutability thisImmutability) {
        // Watch out for Object.super(...) constructor call
        {
            if ((tree.getMethodSelect().getKind() == Tree.Kind.IDENTIFIER)
                    && ((IdentifierTree) tree.getMethodSelect()).getName()
                            .contentEquals("super")) {
                AnnotatedMethodType method =
                    factory.getMethod(tree.getMethodSelect());
                TypeElement elem =
                    (TypeElement) method.getElement().getEnclosingElement();
                if (elem.getQualifiedName().contentEquals(
                        Object.class.getCanonicalName()))
                    return true;
            }
        }
        IGJImmutability receiverImmutability = READONLY;
        if (TreeUtils.isSelfCall(tree)) {
            receiverImmutability = thisImmutability;
        } else {
            assert (tree.getMethodSelect().getKind() == Tree.Kind.MEMBER_SELECT);
            AnnotatedClassType receiverType =
                factory.getClass(((MemberSelectTree) tree.getMethodSelect())
                        .getExpression());

            if (checker.shouldSkip(receiverType))
                return true;

            receiverImmutability =
                getIGJImmutabilityAt(receiverType,
                        AnnotationLocation.RAW);
        }

        AnnotatedMethodType methodType = factory.getMethod(tree);
        if (ElementUtils.isStatic(methodType.getElement()))
            return true;

        IGJImmutability methodImmutability =
            getIGJImmutabilityAt(methodType
                    .getAnnotatedReceiverType(), AnnotationLocation.RAW);
        
        if (methodImmutability == ASSIGNSFIELDS) {
            if (TreeUtils.isSelfCall(tree))
                return (receiverImmutability == ASSIGNSFIELDS ||
                    receiverImmutability == MUTABLE);
            else
                return receiverImmutability == MUTABLE;           
        }
        return (receiverImmutability.isSubtypeOf(methodImmutability));
    }
}
