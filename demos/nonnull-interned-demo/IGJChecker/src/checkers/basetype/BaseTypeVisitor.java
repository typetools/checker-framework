package checkers.basetype;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;

import checkers.nonnull.NonNullChecker;
import checkers.source.*;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.*;

/**
 * A {@link SourceVisitor} that performs assignment and pseudo-assignment
 * checking, method invocation checking, and assignability checking.
 *
 * <p>
 *
 * This implementation uses the {@link AnnotatedTypeFactory} implementation
 * provided by an associated {@link BaseTypeChecker}; its visitor methods will
 * invoke this factory on parts of the AST to determine the "annotated type" of
 * an expression. Then, the visitor methods will check the types in assignments
 * and pseudo-assignments using {@link #commonAssignmentCheck}, which
 * ultimately calls the {@link BaseTypeChecker#isSubtype} method and reports
 * errors that violate Java's rules of assignment.
 *
 * <p>
 *
 * Note that since this implementation only performs assignment and
 * pseudo-assignment checking, other rules for custom type systems must be added
 * in subclasses (e.g., dereference checking in the {@link NonNullChecker} is
 * implemented in the {@link NonNullChecker}'s
 * {@link com.sun.source.util.TreeScanner#visitMemberSelect} method).
 *
 * <p>
 * 
 * This implementation does the following checks:
 * <ul>
 *  <li> <b>Assignment and Pseudo-Assignment Check</b>: 
 *      It verifies that any assignment type check, using 
 *      {@code Checker.isSubtype} method. This includes method invocation and
 *      method overriding checks.</li>
 *      
 *  <li> <b>Type Validity Check</b>:
 *      It verifies that any user-supplied type is a valid type, using
 *      {@code Checker.isValidUse} method.</li>
 *      
 *  <li> <b>(Re-)Assignability Check</b>:
 *      It verifies that any assignment is valid, using 
 *      {@code Checker.isAssignable} method.</li>
 * </ul>
 * 
 * @see "JLS $4"
 * @see BaseTypeChecker#isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)
 * @see AnnotatedTypeFactory
 */
public class BaseTypeVisitor<R, P> extends SourceVisitor<R, P> {

    /** The checker corresponding to this visitor. */
    protected BaseTypeChecker checker;

    /** The annotation factory to use for creating annotations. */
    protected final AnnotationFactory annoFactory;

    /** The options that were provided to the checker using this visitor. */
    private final Map<String, String> options;

    /** For obtaining line numbers in -Ashowchecks debugging output. */
    private final SourcePositions positions;

    private final AnnotatedTypes annoTypes;
    
    /** For storing visitor state**/
    protected final VisitorState visitorState;

    /**
     * @param checker the typechecker associated with this visitor (for
     *        callbacks to {@link BaseTypeChecker#isSubtype})
     * @param root the root of the AST that this visitor operates on
     */
    public BaseTypeVisitor(BaseTypeChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.checker = checker;

        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.annoFactory = new AnnotationFactory(env);
        this.options = env.getOptions();
        this.positions = trees.getSourcePositions();
        this.annoTypes =
            new AnnotatedTypes(checker.getProcessingEnvironment(), factory);
        this.visitorState = factory.getVisitorState();
    }

    @Override
    public R visitAnnotation(AnnotationTree node, P p) {
        // Skip checking inside annotations.
        return null;
    }

    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        commonAssignmentCheck(node.getVariable(), node.getExpression(), "assignment.invalid", p);
        return super.visitAssignment(node, p);
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        AnnotatedDeclaredType preACT = visitorState.getClassType();
        ClassTree preCT = visitorState.getClassTree();

        visitorState.setClassType((AnnotatedDeclaredType)factory.getAnnotatedType(node));
        visitorState.setClassTree(node);

        try { 
            return super.visitClass(node, p);
        } finally {
            this.visitorState.setClassType(preACT);
            this.visitorState.setClassTree(preCT);
        }
    }
    
    @Override
    public R visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        commonAssignmentCheck(node.getVariable(), node.getExpression(), "compound.assignment.invalid", p);
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public R visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        AnnotatedTypeMirror var = factory.getAnnotatedType(node.getVariable());
        AnnotatedTypeMirror iterableType = factory.getAnnotatedType(node.getExpression());
        AnnotatedTypeMirror iteratedType =
            annoTypes.getIteratedType(iterableType);
        validateTypeOf(node.getVariable());
        commonAssignmentCheck(var, iteratedType, node.getExpression(), "type.incompatible", p);
        return super.visitEnhancedForLoop(node, p);
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        
        AnnotatedDeclaredType preMRT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();
        visitorState.setMethodReceiver(
                ((AnnotatedExecutableType)factory.getAnnotatedType(node)).getReceiverType());
        visitorState.setMethodTree(node);
        
        try {
        Element elt = InternalUtils.symbol(node);
        assert elt != null : "no symbol for method";
        // Mahmood: I don't know if this absolutely necessary
        if (InternalUtils.isAnonymousConstructor(node))
            // We shouldn't dig deeper
            return null;

        // TODO: this currently checks for *any* receiver annotation, which is too conservative
        if (ElementUtils.isStatic(elt) &&
                !node.getReceiver().getAnnotations().isEmpty())
            checker.report(Result.failure("missing.this"), node.getReceiver());

        ExecutableElement methodElement = (ExecutableElement) InternalUtils.symbol(node);
        AnnotatedDeclaredType enclosingType = (AnnotatedDeclaredType)factory.getAnnotatedType(methodElement.getEnclosingElement());

        if (!elt.getModifiers().contains(Modifier.STATIC)) {
            // Test validity of the annotation

            // receiver type needs to a supertype of
//            if (!checker.isValidUse(method.getReceiverType(), enclosingType))
//                checker.report(Result.failure("receiver.invalid",
//                        method.getReceiverType(), enclosingType), node);
        }

        // Find which method this overrides!
        for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair: 
                annoTypes.overriddenMethods(methodElement).entrySet()) {
            AnnotatedDeclaredType overriddenType = pair.getKey();
            AnnotatedExecutableType overriddenMethod = (AnnotatedExecutableType)
                annoTypes.asMemberOf(overriddenType, pair.getValue());
            checkOverride(node, enclosingType, overriddenMethod, overriddenType, p);
        }
        return super.visitMethod(node, p);
        } finally {
            visitorState.setMethodReceiver(preMRT);
            visitorState.setMethodTree(preMT);
        }
    }

    // TODO: move me to a utility class
    /**
     * @param node the method invocation to check
     * @return true if this is a super call to the {@link Enum} constructor
     */
    private boolean isEnumSuper(MethodInvocationTree node) {

        AnnotatedExecutableType invokedMethod =
            (AnnotatedExecutableType)factory.getAnnotatedType(node.getMethodSelect());

        ExecutableElement ex = invokedMethod.getElement();

        Element sup = ex.getEnclosingElement();
        if (sup instanceof TypeElement &&
                ((TypeElement)sup).getQualifiedName().contentEquals("java.lang.Enum"))
            return true;

        return false;
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        if (node.getMethodSelect().getKind() == Tree.Kind.IDENTIFIER &&
                ((IdentifierTree)node.getMethodSelect()).getName().contentEquals("super"))
            return super.visitMethodInvocation(node, p);

        // Skip calls to the Enum constructor (they're generated by javac and
        // hard to check).
        if (isEnumSuper(node))
            return super.visitMethodInvocation(node, p);

        AnnotatedExecutableType invokedMethod = factory.methodFromUse(node);
        
        // Get type arguments as passed to the invocation.
        List<AnnotatedTypeMirror> typeargs = new LinkedList<AnnotatedTypeMirror>();
        for (Tree tree : node.getTypeArguments())
            typeargs.add(factory.getAnnotatedTypeFromTypeTree(tree));
        
        checkTypeArguments(invokedMethod.getTypeVariables(), 
                typeargs, node.getTypeArguments(), p);

        List<AnnotatedTypeMirror> params = annoTypes.getMethodParameters(invokedMethod, node.getArguments());
        checkArguments(params, node.getArguments(), p);
        ExecutableElement elt = factory.elementFromUse(node);
        if (elt != null && !ElementUtils.isStatic(elt))
            checkMethodInvocability(invokedMethod, node);

        return super.visitMethodInvocation(node, p);
    }

    /**
     * Checks that the annotations on the type arguments supplied to a type or a
     * method invocation are within the bounds of the type variables as
     * declared, and issues the "generic.argument.invalid" error if they are
     * not.
     * 
     * @param typevars the type variables from a class or method declaration
     * @param typeargs the type arguments from the type or method invocation
     * @param typeargTrees the type arguments as trees, used for error reporting
     * @param p
     */
    protected void checkTypeArguments(
            List<? extends AnnotatedTypeMirror> typevars,
            List<? extends AnnotatedTypeMirror> typeargs,
            List<? extends Tree> typeargTrees, P p) {
        
        // If there are no type arguments, do nothing. 
        if (typeargs.isEmpty()) return;
        
        Iterator<? extends AnnotatedTypeMirror> varIter = typevars.iterator();
        Iterator<? extends AnnotatedTypeMirror> argIter = typeargs.iterator();
        
        while (varIter.hasNext()) {
            
            AnnotatedTypeMirror var = varIter.next();            
            assert var.getKind() == TypeKind.TYPEVAR : var.getKind();
            AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) var;
            
            assert argIter.hasNext() : typevars + " / " + typeargs;
                        
            AnnotatedTypeMirror typearg = argIter.next();
            // TODO skip wildcards for now to prevent a crash
            if (typearg.getKind() == TypeKind.WILDCARD) continue;
            
            if (typeVar.getUpperBound() != null)  {
                if (!(TypesUtils.isObject(typeVar.getUpperBound().getUnderlyingType())
                        && typeVar.getUpperBound().getAnnotations().isEmpty())) {
                    commonAssignmentCheck(typeVar.getUpperBound(), typearg,
                            typeargTrees.get(typeargs.indexOf(typearg)),
                            "generic.argument.invalid", p);
                }
            }
        }
    }

    protected void checkArguments(List<? extends AnnotatedTypeMirror> requiredArgs, 
            List<? extends ExpressionTree> passedArgs, P p) {

        for (int i = 0; i < requiredArgs.size(); ++i)
            commonAssignmentCheck(requiredArgs.get(i),
                    passedArgs.get(i),
                    "argument.invalid", p);
    }

    @Override
    public R visitNewArray(NewArrayTree node, P p) {
        validateTypeOf(node);
        return super.visitNewArray(node, p);
    }

    @Override
    public R visitNewClass(NewClassTree node, P p) {
        List<AnnotatedTypeMirror> params = annoTypes.getConstructorParameters(node);
        checkArguments(params, node.getArguments(), p);
        
        // Get the constructor type.
        AnnotatedTypeMirror type = factory.getAnnotatedType(InternalUtils.constructor(node));
        assert type.getKind() == TypeKind.EXECUTABLE;
        AnnotatedExecutableType ctype = (AnnotatedExecutableType)type;
        
        // Get the type args to the constructor.
        List<AnnotatedTypeMirror> typeargs = new LinkedList<AnnotatedTypeMirror>();
        for (Tree tree : node.getTypeArguments())
            typeargs.add(factory.getAnnotatedTypeFromTypeTree(tree));
        
        checkTypeArguments(ctype.getTypeVariables(), 
                typeargs, node.getTypeArguments(), p);
        
        validateTypeOf(node);
        return super.visitNewClass(node, p);
    }
        
    @Override
    public R visitParameterizedType(ParameterizedTypeTree node, P p) {
        
        AnnotatedTypeMirror type = factory.getAnnotatedTypeFromTypeTree(node);
        
        if (type.getKind() != TypeKind.DECLARED) 
            return super.visitParameterizedType(node, p);
               
        AnnotatedDeclaredType declared = (AnnotatedDeclaredType)type;
        final Element element = declared.getUnderlyingType().asElement();
        
        final AnnotatedTypeMirror elementType = factory.getAnnotatedType(element);
        assert elementType.getKind() == TypeKind.DECLARED;
        AnnotatedDeclaredType generic = (AnnotatedDeclaredType)elementType;
        
        checkTypeArguments(generic.getTypeArguments(), 
                declared.getTypeArguments(), node.getTypeArguments(), p);
            
        return super.visitParameterizedType(node, p);
    }

    @Override
    public R visitReturn(ReturnTree node, P p) {

        // Don't try to check return expressions for void methods.
        if (node.getExpression() == null)
            return super.visitReturn(node, p);

        MethodTree enclosingMethod =
            TreeUtils.enclosingMethod(getCurrentPath());

        AnnotatedExecutableType methodType = (AnnotatedExecutableType) factory.getAnnotatedType(enclosingMethod);
        commonAssignmentCheck(methodType.getReturnType(), node.getExpression(),
                "return.invalid", p);

        return super.visitReturn(node, p);
    }

    @Override
    public R visitTypeCast(TypeCastTree node, P p) {
        validateTypeOf(node.getType());
        return super.visitTypeCast(node, p);
    }

    @Override
    public R visitVariable(VariableTree node, P p) {
        validateTypeOf(node);
        // If there's no assignment in this variable declaration, skip it.
        if (node.getInitializer() == null)
            return super.visitVariable(node, p);

        commonAssignmentCheck(node, node.getInitializer(), "assignment.invalid", p);
        return super.visitVariable(node, p);
    }

    @Override
    public R visitInstanceOf(InstanceOfTree node, P p) {
        validateTypeOf(node.getType());
        return super.visitInstanceOf(node, p);
    }

    @Override
    public R visitUnary(UnaryTree node, P p) {
        // This is to allow isAssignable checks in a signle location
        if ((node.getKind() == Tree.Kind.PREFIX_DECREMENT) ||
                (node.getKind() == Tree.Kind.PREFIX_INCREMENT) ||
                (node.getKind() == Tree.Kind.POSTFIX_DECREMENT) ||
                (node.getKind() == Tree.Kind.POSTFIX_INCREMENT)) {
            commonAssignmentCheck(node.getExpression(),
                    node.getExpression(), "assignment.invalid", p);
        }
        return super.visitUnary(node, p);
    }

    /**
     * Checks that an overriding method's return type, parameter types, and
     * receiver type are correct with respect to the annotations on the
     * overridden method's return type, parameter types, and receiver type.
     *
     * <p>
     *
     * This method returns the result of the check, but also emits error
     * messages as a side effect.
     *
     * @param overriderTree the AST node of the overriding method
     * @param enclosingType the declared type enclosing the overrider method
     * @param overridden the type of the overridden method
     * @param overriddenType the declared type enclosing the overridden method
     * @param p an optional parameter (as supplied to visitor methods)
     * @return true if the override check passed, false otherwise
     */
    protected boolean checkOverride(MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            P p) {

        // Get the type of the overriding method.
        AnnotatedExecutableType overrider =
            (AnnotatedExecutableType) factory.getAnnotatedType(overriderTree);

        boolean result = true;

        String overriderMeth = overrider.getElement().toString();
        String overriderTyp = enclosingType.getUnderlyingType().asElement().toString();
        String overridenMeth = overridden.getElement().toString();
        String overridenTyp = overriddenType.getUnderlyingType().asElement().toString();
        
        // Check the return value.
        if ((overrider.getReturnType().getKind() != TypeKind.VOID)
            && !checker.isSubtype(overridden.getReturnType(),
                overrider.getReturnType())) {
            checker.report(Result.failure("override.return.invalid",
                    overriderMeth, overriderTyp, overridenMeth, overridenTyp,
                    overrider.getReturnType().toString(),
                    overridden.getReturnType().toString()),
                    overriderTree.getReturnType());
            // emit error message
            result = false;
        }

        // Check parameter values. (FIXME varargs)
        List<AnnotatedTypeMirror> overriderParams =
            overrider.getParameterTypes();
        List<AnnotatedTypeMirror> overriddenParams =
            overridden.getParameterTypes();
        for (int i = 0; i < overriderParams.size(); ++i) {
            if (!checker.isSubtype(overriderParams.get(i), overriddenParams.get(i))) {
                checker.report(Result.failure("override.param.invalid",
                        overriderMeth, overriderTyp, overridenMeth, overridenTyp,
                        overriderParams.get(i).toString(),
                        overriddenParams.get(i).toString()
                        ), overriderTree.getParameters().get(i));
                // emit error message
                result = false;
            }
        }

        // Check the receiver type.
        if (!checker.isSubtype(overrider.getReceiverType().getErased(),
                overridden.getReceiverType().getErased())) {
            checker.report(Result.failure("override.receiver.invalid",
                    overriderMeth, overriderTyp, overridenMeth, overridenTyp,
                    overrider.getReceiverType(),
                    overridden.getReceiverType()),
                    overriderTree);
            result = false;
        }
        return result;
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value
     * to a variable and emits an error message (through the compiler's
     * messaging interface) if it does.
     *
     * @param varTree the AST node for the variable
     * @param valueExp the AST node for the value
     * @param errorKey the error message to use if the check fails
     * @param p a checker-specified parameter
     */
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp, String errorKey, P p) {
        AnnotatedTypeMirror var = factory.getAnnotatedType(varTree);
        assert var != null;
        if (varTree.getKind() != Tree.Kind.VARIABLE &&
                !checker.isAssignable(var, varTree)) {
            checker.report(
                    Result.failure("assignability.invalid", 
                            InternalUtils.symbol(varTree),
                            factory.getReceiver((ExpressionTree)varTree)), varTree);
        }
        commonAssignmentCheck(var, valueExp, errorKey, p);
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value
     * to a variable and emits an error message (through the compiler's
     * messaging interface) if it does.
     *
     * @param varType the annotated type of the variable
     * @param valueExp the AST node for the value
     * @param errorKey the error message to use if the check fails
     * @param p a checker-specified parameter
     */
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType, ExpressionTree valueExp, String errorKey, P p) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(valueExp);
        assert valueType != null;
        commonAssignmentCheck(varType, valueType, valueExp, errorKey, p);
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value
     * to a variable and emits an error message (through the compiler's
     * messaging interface) if it does.
     *
     * @param varType the annotated type of the variable
     * @param valueType the annotated type of the value
     * @param valueTree the location to use when reporting the error message
     * @param errorKey the error message to use if the check fails
     * @param p a checker-specified parameter
     */
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, String errorKey, P p) {


        AnnotatedTypeMirror valueBaseType;
        // We shouldn't have any exceptions nor null valueBaseType
        try {
            valueBaseType = annoTypes.asSuper(valueType, varType);
            if (valueBaseType == null) valueBaseType = valueType;
        } catch (Exception e) {
            valueBaseType = valueType;
        }

        boolean success = checker.isSubtype(varType, valueBaseType);

        if (options.containsKey("showchecks")) {
            long valuePos = positions.getStartPosition(root, valueTree);
            System.out.printf(
                    " %s (line %3d): %s %s\n     actual: %s %s\n   expected: %s %s\n",
                    (success ? "success" : "FAILURE"),
                    root.getLineMap().getLineNumber(valuePos),
                    valueTree.getKind(), valueTree,
                    valueType.getKind(), valueType,
                    varType.getKind(), varType);
        }

        // Use an error key only if it's overriden by a checker.
        String useKey;
        if (checker.getMessages().getProperty(errorKey) != null)
            useKey = errorKey;
        else useKey = "type.incompatible";
        
        if (!success) {
            checker.report(Result.failure(useKey,
                    valueType.toString(), varType.toString()), valueTree);
        }
    }

    /**
     * Tests whether the method can be invoked using the receiver of the 'node'
     * method invocation.
     * 
     * @param method    the type of the invoked method
     * @param node      the method invocation node
     * @return true iff the call of 'node' is a valid call
     */
    protected boolean checkMethodInvocability(AnnotatedExecutableType method, MethodInvocationTree node) {
        AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
        AnnotatedTypeMirror treeReceiver = factory.getReceiver(node).getErased();
        
        if (!checker.isSubtype(methodReceiver, treeReceiver)) {
            checker.report(Result.failure("method.invocation.invalid",
                factory.elementFromUse(node),
                treeReceiver.toString(), methodReceiver.toString()), node);
            return false;
        }
        return true;
    }
    
    /**
     * Tests whether the tree expressed by the passed type tree is a valid type,
     * and emits an error if that is not the case (e.g. '@Mutable String').
     * 
     * @param tree  the AST type supplied by the user
     */
    public void validateTypeOf(Tree tree) {
        AnnotatedTypeMirror type;
        // It's quite annoying that there is no TypeTree
        switch (tree.getKind()) {
        case PRIMITIVE_TYPE:
        case PARAMETERIZED_TYPE:
        case TYPE_PARAMETER:
        case ARRAY_TYPE:
            type = factory.getAnnotatedTypeFromTypeTree(tree);
            break;
        default:
            type = factory.getAnnotatedType(tree);
        }
        typeValidator.visit(type, tree);
    }

    // This is a test to ensure that all types are valid
    private AnnotatedTypeScanner<Void, Tree> typeValidator =
        new AnnotatedTypeScanner<Void, Tree>() {

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {

            // Ensure that type use is a subtype of the element type
            AnnotatedDeclaredType useType = type.getErased();
            AnnotatedDeclaredType elemType = (AnnotatedDeclaredType)
                factory.getAnnotatedType(
                        useType.getUnderlyingType().asElement()).getErased();

            if (!checker.isValidUse(elemType, useType)) {
                checker.report(Result.failure("type.invalid",
                        useType.getAnnotations(), elemType.toString()), p);
            }

            return super.visitDeclared(type, p);
        }
    };

    /**
     * Override Compilation Unit so we won't visit package names or imports
     */
    @Override
    public R visitCompilationUnit(CompilationUnitTree node, P p) {
        R r = scan(node.getPackageAnnotations(), p);
        // r = reduce(scan(node.getPackageName(), p), r);
        // r = reduce(scan(node.getImports(), p), r);
        r = reduce(scan(node.getTypeDecls(), p), r);
        return r;
    }
}
