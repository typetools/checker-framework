package checkers.basetype;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.igj.quals.Immutable;
import checkers.igj.quals.ReadOnly;
import checkers.nullness.NullnessChecker;
import checkers.quals.DefaultQualifier;
import checkers.quals.Unused;
import checkers.source.Result;
import checkers.source.SourceVisitor;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

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
 * in subclasses (e.g., dereference checking in the {@link NullnessChecker} is
 * implemented in the {@link NullnessChecker}'s
 * {@link TreeScanner#visitMemberSelect} method).
 *
 * <p>
 *
 * This implementation does the following checks:
 * 1. <b>Assignment and Pseudo-Assignment Check</b>:
 *    It verifies that any assignment type check, using
 *    {@code Checker.isSubtype} method. This includes method invocation and
 *    method overriding checks.
 *
 * 2. <b>Type Validity Check</b>:
 *    It verifies that any user-supplied type is a valid type, using
 *    {@code isValidUse} method.
 *
 * 3. <b>(Re-)Assignability Check</b>:
 *    It verifies that any assignment is valid, using
 *    {@code Checker.isAssignable} method.
 *
 * @see "JLS $4"
 * @see BaseTypeChecker#isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)
 * @see AnnotatedTypeFactory
 */
public class BaseTypeVisitor<Checker extends BaseTypeChecker> extends SourceVisitor<Void, Void> {

    /** The checker corresponding to this visitor. */
    protected final Checker checker;

    /** The annotation factory to use for creating annotations. */
    protected final AnnotationUtils annoFactory;

    /** The options that were provided to the checker using this visitor. */
    protected final Map<String, String> options;

    /** For obtaining line numbers in -Ashowchecks debugging output. */
    private final SourcePositions positions;

    /** utilities class for annotated types **/
    protected final AnnotatedTypes annoTypes;

    /** For storing visitor state**/
    protected final VisitorState visitorState;

    /**
     * @param checker the typechecker associated with this visitor (for
     *        callbacks to {@link BaseTypeChecker#isSubtype})
     * @param root the root of the AST that this visitor operates on
     */
    public BaseTypeVisitor(Checker checker, CompilationUnitTree root) {
        super(checker, root);
        this.checker = checker;

        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.annoFactory = AnnotationUtils.getInstance(env);
        this.options = env.getOptions();
        this.positions = trees.getSourcePositions();
        this.annoTypes =
            new AnnotatedTypes(checker.getProcessingEnvironment(), atypeFactory);
        this.visitorState = atypeFactory.getVisitorState();
    }

    // **********************************************************************
    // Responsible for updating the factory for the location (for performance)
    // **********************************************************************

    @Override
    public Void scan(Tree tree, Void p) {
        if (tree != null && getCurrentPath() != null)
            this.visitorState.setPath(new TreePath(getCurrentPath(), tree));
        return super.scan(tree, p);
    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        if (checker.shouldSkipDefs(node)) {
            // Not "return super.visitClass(node, p);" because that would
            // recursively call visitors on subtrees; we want to skip the
            // class entirely.
            return null;
        }

        AnnotatedDeclaredType preACT = visitorState.getClassType();
        ClassTree preCT = visitorState.getClassTree();
        AnnotatedDeclaredType preAMT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();
        Tree preAssCtxt = visitorState.getAssignmentContextTree();

        visitorState.setClassType(atypeFactory.getAnnotatedType(node));
        visitorState.setClassTree(node);
        visitorState.setMethodReceiver(null);
        visitorState.setMethodTree(null);
        visitorState.setAssignmentContextTree(null);

        try {
            if (!TreeUtils.hasExplicitConstructor(node)) {
                checkDefaultConstructor(node);
            }

            /* Visit the extends and implements clauses.
             * The superclass also visits them, but only calls visitParameterizedType, which
             * looses a main modifier.
             */
            Tree ext = node.getExtendsClause();
            if (ext!=null) {
                validateTypeOf(ext);
            }

            List<? extends Tree> impls = node.getImplementsClause();
            if (impls!=null) {
                for (Tree im : impls) {
                    validateTypeOf(im);
                }
            }

            return super.visitClass(node, p);
        } finally {
            this.visitorState.setClassType(preACT);
            this.visitorState.setClassTree(preCT);
            this.visitorState.setMethodReceiver(preAMT);
            this.visitorState.setMethodTree(preMT);
            this.visitorState.setAssignmentContextTree(preAssCtxt);
        }
    }

    protected void checkDefaultConstructor(ClassTree node) { }

    /**
     * Performs pseudo-assignment check: checks that the method obeys override
     * and subtype rules to all overridden methods.
     *
     * The override rule specifies that a method, m1, may override a method
     * m2 only if:
     * <ul>
     *  <li> m1 return type is a subtype of m2 </li>
     *  <li> m1 receiver type is a supertype of m2 <li>
     *  <li> m1 parameters are supertypes of corresponding m2 parameters </li>
     * </ul>
     *
     * Also, it issues a "missing.this" error for static method annotated
     * receivers.
     */
    @Override
    public Void visitMethod(MethodTree node, Void p) {

        AnnotatedExecutableType methodType = atypeFactory.getAnnotatedType(node);
        AnnotatedDeclaredType preMRT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();
        visitorState.setMethodReceiver(methodType.getReceiverType());
        visitorState.setMethodTree(node);

        try {
        Element elt = InternalUtils.symbol(node);
        assert elt != null : "no symbol for method";
        if (InternalUtils.isAnonymousConstructor(node)) {
            // We shouldn't dig deeper
            return null;
        }

        // constructor return types are null
        if (node.getReturnType() != null) {
            typeValidator.visit(methodType.getReturnType(), node.getReturnType());
        }

        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);
        AnnotatedDeclaredType enclosingType =
            (AnnotatedDeclaredType)atypeFactory.getAnnotatedType(
                    methodElement.getEnclosingElement());

        // Find which method this overrides!
        Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
            annoTypes.overriddenMethods(methodElement);
        for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair: overriddenMethods.entrySet()) {
            AnnotatedDeclaredType overriddenType = pair.getKey();
            AnnotatedExecutableType overriddenMethod =
                annoTypes.asMemberOf(overriddenType, pair.getValue());
            checkOverride(node, enclosingType, overriddenMethod, overriddenType, p);
        }
        return super.visitMethod(node, p);
        } finally {
            visitorState.setMethodReceiver(preMRT);
            visitorState.setMethodTree(preMT);
        }
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree node, Void p) {
        validateTypeOf(node);
        return super.visitTypeParameter(node, p);
    }

    // **********************************************************************
    // Assignment checkers and pseudo-assignments
    // **********************************************************************


    @Override
    public Void visitVariable(VariableTree node, Void p) {
        Tree preAssCtxt = visitorState.getAssignmentContextTree();
        visitorState.setAssignmentContextTree(node);

        try {
            validateTypeOf(node);
            // If there's no assignment in this variable declaration, skip it.
            if (node.getInitializer() != null) {
                commonAssignmentCheck(node, node.getInitializer(), "assignment.type.incompatible");
            }
        } finally {
            visitorState.setAssignmentContextTree(preAssCtxt);
        }
        return super.visitVariable(node, p);
    }

    /**
     * Performs two checks: subtyping and assignability checks, using
     * {@link #commonAssignmentCheck(Tree, ExpressionTree, String)}.
     *
     * If the subtype check fails, it issues a "assignment.type.incompatible" error.
     */
    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        Tree preAssCtxt = visitorState.getAssignmentContextTree();
        visitorState.setAssignmentContextTree(node);
        try {
            commonAssignmentCheck(node.getVariable(), node.getExpression(),
                    "assignment.type.incompatible");
        } finally {
            visitorState.setAssignmentContextTree(preAssCtxt);
        }
        return super.visitAssignment(node, p);
    }

    /**
     * Performs a subtype check, to test whether the node expression
     * iterable type is a subtype of the variable type in the enhanced for
     * loop.
     *
     * If the subtype check fails, it issues a "type.incompatible" error.
     */
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        AnnotatedTypeMirror var = atypeFactory.getAnnotatedType(node.getVariable());
        AnnotatedTypeMirror iterableType =
            atypeFactory.getAnnotatedType(node.getExpression());
        AnnotatedTypeMirror iteratedType =
            annoTypes.getIteratedType(iterableType);
        validateTypeOf(node.getVariable());
        commonAssignmentCheck(var, iteratedType, node.getExpression(),
                "enhancedfor.type.incompatible");
        return super.visitEnhancedForLoop(node, p);
    }

    /**
     * Performs a method invocation check.
     *
     * An invocation of a method, m, on the receiver, r is valid only if:
     * <ul>
     *  <li> passed arguments are subtypes of corresponding m parameters </li>
     *  <li> r is a subtype of m receiver type </li>
     *  <li> if m is generic, passed type arguments are subtypes
     *      of m type variables </li>
     * </ul>
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        // Skip calls to the Enum constructor (they're generated by javac and
        // hard to check).
        if (isEnumSuper(node))
            return super.visitMethodInvocation(node, p);

        if (shouldSkipUses(node))
            return super.visitMethodInvocation(node, p);

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.first;
        List<AnnotatedTypeMirror> typeargs = mfuPair.second;

        checkTypeArguments(node, invokedMethod.getTypeVariables(),
                typeargs, node.getTypeArguments());

        List<AnnotatedTypeMirror> params =
            annoTypes.expandVarArgs(invokedMethod, node.getArguments());
        checkArguments(params, node.getArguments());

        if (isVectorCopyInto(invokedMethod)) {
            typeCheckVectorCopyIntoArgument(node, params);
        }

        if (!ElementUtils.isStatic(invokedMethod.getElement())
            && !TreeUtils.isSuperCall(node))
            checkMethodInvocability(invokedMethod, node);

        return super.visitMethodInvocation(node, p);
    }

    // Handle case Vector.copyInto()
    private final AnnotatedDeclaredType vectorType =
        atypeFactory.fromElement(elements.getTypeElement("java.util.Vector"));

    /**
     * Returns true if the method symbol represents {@code Vector.copyInto}
     */
    protected boolean isVectorCopyInto(AnnotatedExecutableType method) {
        ExecutableElement elt = method.getElement();
        if (elt.getSimpleName().contentEquals("copyInto")
            && elt.getParameters().size() == 1)
            return true;

        return false;
    }

    /**
     * Type checks the method arguments of {@code Vector.copyInto()}.
     *
     * The Checker Framework special-cases the method invocation, as it is
     * type safety cannot be expressed by Java's type system.
     *
     * For a Vector {@code v} of type {@code Vectory<E>}, the method
     * invocation {@code v.copyInto(arr)} is type-safe iff {@code arr}
     * is a array of type {@code T[]}, where {@code T} is a subtype of
     * {@code E}.
     *
     * In other words, this method checks that the type argument of the
     * receiver method is a subtype of the component type of the passed array
     * argument.
     *
     * @param node   a method invocation of {@code Vector.copyInto()}
     * @param params the types of the parameters of {@code Vectory.copyInto()}
     *
     */
    protected void typeCheckVectorCopyIntoArgument(MethodInvocationTree node,
            List<? extends AnnotatedTypeMirror> params) {
        assert params.size() == 1;
        assert node.getArguments().size() == 1;

        AnnotatedTypeMirror passed = atypeFactory.getAnnotatedType(node.getArguments().get(0));
        AnnotatedArrayType passedAsArray = (AnnotatedArrayType)passed;

        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(node);
        AnnotatedDeclaredType receiverAsVector =
            (AnnotatedDeclaredType)annoTypes.asSuper(receiver, vectorType);
        if (receiverAsVector == null || receiverAsVector.getTypeArguments().isEmpty())
            return;

        commonAssignmentCheck(
                passedAsArray.getComponentType(),
                receiverAsVector.getTypeArguments().get(0),
                node.getArguments().get(0),
                "vector.copyinto.type.incompatible");
    }

    /**
     * Performs a new class invocation check.
     *
     * An invocation of a constructor, c, is valid only if:
     * <ul>
     *  <li> passed arguments are subtypes of corresponding c parameters </li>
     *  <li> if c is generic, passed type arguments are subtypes
     *      of c type variables <li>
     * </ul>
     */
    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        if (checker.shouldSkipUses(InternalUtils.constructor(node)))
            return super.visitNewClass(node, p);

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> fromUse = atypeFactory.constructorFromUse(node);
        AnnotatedExecutableType constructor = fromUse.first;
        List<AnnotatedTypeMirror> typeargs = fromUse.second;

        List<? extends ExpressionTree> passedArguments = node.getArguments();
        List<AnnotatedTypeMirror> params =
            annoTypes.expandVarArgs(constructor, passedArguments);

        checkArguments(params, passedArguments);

        // Get the constructor type.
        // TODO: What is the difference between "type" and "constructor"?
        // Using "constructor" seems to work equally well...
        // AnnotatedExecutableType type =
        //   atypeFactory.getAnnotatedType(InternalUtils.constructor(node));

        checkTypeArguments(node, constructor.getTypeVariables(),
                typeargs, node.getTypeArguments());

        AnnotatedDeclaredType dt = atypeFactory.getAnnotatedType(node);
        checkConstructorInvocation(dt, constructor, node);
        validateTypeOf(node);

        return super.visitNewClass(node, p);
    }

    /**
     * Checks that the type of the return expression is a subtype of the
     * enclosing method required return type.  If not, it issues a
     * "return.type.incompatible" error.
     */
    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        // Don't try to check return expressions for void methods.
        if (node.getExpression() == null)
            return super.visitReturn(node, p);

        MethodTree enclosingMethod =
            TreeUtils.enclosingMethod(getCurrentPath());

        AnnotatedExecutableType methodType = atypeFactory.getAnnotatedType(enclosingMethod);
        commonAssignmentCheck(methodType.getReturnType(), node.getExpression(),
                "return.type.incompatible");

        return super.visitReturn(node, p);
    }

    /** Ensure that the annotation arguments comply to their declarations.
     * This needs some special casing, as annotation arguments form special trees.
     */
    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        List<? extends ExpressionTree> args = node.getArguments();
        if (args.isEmpty()) {
            // Nothing to do if there are no annotation arguments.
            return null;
        }

        Element anno = TreeInfo.symbol((JCTree) node.getAnnotationType());
        if (anno.toString().equals(DefaultQualifier.class.getName()) ||
                anno.toString().equals(SuppressWarnings.class.getName())) {
            // Skip these two annotations, as we don't care about the string
            // arguments to them.
            return null;
        }

        // Mapping from argument simple name to its annotated type.
        Map<String, AnnotatedTypeMirror> annoTypes = new HashMap<String, AnnotatedTypeMirror>();
        for (Element encl : ElementFilter.methodsIn(anno.getEnclosedElements())) {
            AnnotatedExecutableType exeatm = (AnnotatedExecutableType) atypeFactory.getAnnotatedType(encl);
            AnnotatedTypeMirror retty = exeatm.getReturnType();
            annoTypes.put(encl.getSimpleName().toString(), retty);
        }

        for (ExpressionTree arg : args) {
            if (!(arg instanceof AssignmentTree)) {
                // TODO: when can this happen?
                continue;
            }

            AssignmentTree at = (AssignmentTree) arg;
            // Ensure that we never ask for the annotated type of an annotation, because
            // we don't have a type for annotations.
            if (at.getExpression().getKind() == Tree.Kind.ANNOTATION) {
                visitAnnotation((AnnotationTree)at.getExpression(), p);
                continue;
            }
            if (at.getExpression().getKind() == Tree.Kind.NEW_ARRAY) {
                NewArrayTree nat = (NewArrayTree) at.getExpression();
                boolean isAnno = false;
                for (ExpressionTree init : nat.getInitializers()) {
                    if (init.getKind() == Tree.Kind.ANNOTATION) {
                        visitAnnotation((AnnotationTree)init, p);
                        isAnno = true;
                    }
                }
                if (isAnno) {
                    continue;
                }
            }

            AnnotatedTypeMirror expected = annoTypes.get(at.getVariable().toString());
            Tree preAssCtxt = visitorState.getAssignmentContextTree();
            visitorState.setAssignmentContextTree(at.getVariable());

            try {
            AnnotatedTypeMirror actual = atypeFactory.getAnnotatedType(at.getExpression());
            if (expected.getKind()!=TypeKind.ARRAY) {
                // Expected is not an array -> direct comparison.
                commonAssignmentCheck(expected, actual, at.getExpression(),
                        "annotation.type.incompatible");
            } else {
                if (actual.getKind()==TypeKind.ARRAY) {
                    // Both actual and expected are arrays.
                    commonAssignmentCheck(expected, actual, at.getExpression(),
                            "annotation.type.incompatible");
                } else {
                    // The declaration is an array type, but just a single element is given.
                    commonAssignmentCheck(((AnnotatedArrayType)expected).getComponentType(),
                            actual, at.getExpression(),
                            "annotation.type.incompatible");
                }
            }
            } finally {
                visitorState.setAssignmentContextTree(preAssCtxt);
            }
        }
        return null;
    }


    // **********************************************************************
    // Check for illegal re-assignment
    // **********************************************************************

    /**
     * Performs assignability check using
     * {@link #checkAssignability(AnnotatedTypeMirror, Tree)}.
     */
    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        if ((node.getKind() == Tree.Kind.PREFIX_DECREMENT) ||
                (node.getKind() == Tree.Kind.PREFIX_INCREMENT) ||
                (node.getKind() == Tree.Kind.POSTFIX_DECREMENT) ||
                (node.getKind() == Tree.Kind.POSTFIX_INCREMENT)) {
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node.getExpression());
            checkAssignability(type, node.getExpression());
        }
        return super.visitUnary(node, p);
    }

    /**
     * Performs assignability check using
     * {@link #checkAssignability(AnnotatedTypeMirror, Tree)}.
     */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node.getVariable());
        checkAssignability(type, node.getVariable());
        return super.visitCompoundAssignment(node, p);
    }

    // **********************************************************************
    // Check for invalid types inserted by the user
    // **********************************************************************

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        validateTypeOf(node);
        if (node.getType() != null && node.getInitializers() != null) {
            AnnotatedArrayType arrayType = atypeFactory.getAnnotatedType(node);
            checkArrayInitialization(arrayType.getComponentType(), node.getInitializers());
        }

        return super.visitNewArray(node, p);
    }

    /* TODO: add once lambda is fully integrated.
    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
        System.out.println("Params: " + node.getParameters());
        System.out.println("Body: " + node.getBody());
        return super.visitLambdaExpression(node, p);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree node, Void p) {
        // node.getQualifierExpression()
        // node.getTypeArguments()
        return super.visitMemberReference(node, p);
    }*/

    /**
     * Do not override this method!
     * Previously, this method contained some logic, but the main modifier of types was missing.
     * It has been merged with the TypeValidator below.
     * This method doesn't need to do anything, as the type is already validated.
     */
    @Override
    public final Void visitParameterizedType(ParameterizedTypeTree node, Void p) {
        return null; // super.visitParameterizedType(node, p);
    }

    protected void checkTypecastRedundancy(TypeCastTree node, Void p) {
        if (!checker.getLintOption("cast:redundant", false))
            return;

        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(node);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());

        if (annoTypes.areSame(castType, exprType)) {
            checker.report(Result.warning("cast.redundant", castType), node);
        }
    }

    protected void checkTypecastSafety(TypeCastTree node, Void p) {
        if (!checker.getLintOption("cast:unsafe", true))
            return;

        boolean isSubtype = false;

        // We cannot do a simple test of casting, as isSubtypeOf requires
        // the input types to be subtypes according to Java
        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(node);
        if (castType.getKind() == TypeKind.DECLARED) {
            // eliminate false positives, where the annotations are
            // implicitly added by the declared type declaration
            AnnotatedDeclaredType castDeclared = (AnnotatedDeclaredType)castType;
            AnnotatedDeclaredType elementType =
                atypeFactory.fromElement((TypeElement)castDeclared.getUnderlyingType().asElement());
            if (AnnotationUtils.areSame(castDeclared.getAnnotations(), elementType.getAnnotations())) {
                isSubtype = true;
            }
        }
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());

        if (!isSubtype) {
            // TODO: Test type arguments and array components types
            // The following
            // isSubtype = checker.isSubtype(exprType, castType);
            // would be too restrictive, as we only want to ensure the relation
            // between annotations, not the whole type.
            isSubtype = checker.getQualifierHierarchy().isSubtype(exprType.getEffectiveAnnotations(), castType.getEffectiveAnnotations());
        }

        if (!isSubtype) {
            checker.report(Result.warning("cast.unsafe", exprType, castType), node);
        }
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        validateTypeOf(node.getType());
        checkTypecastSafety(node, p);
        checkTypecastRedundancy(node, p);
        return super.visitTypeCast(node, p);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {
        validateTypeOf(node.getType());
        return super.visitInstanceOf(node, p);
    }

    // **********************************************************************
    // Helper methods to provide a single overriding point
    // **********************************************************************

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value
     * to a variable and emits an error message (through the compiler's
     * messaging interface) if it is not valid.
     *
     * @param varTree the AST node for the variable
     * @param valueExp the AST node for the value
     * @param errorKey the error message to use if the check fails
     */
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp, /*@CompilerMessageKey*/ String errorKey) {
        AnnotatedTypeMirror var = atypeFactory.getAnnotatedType(varTree);
        assert var != null;
        checkAssignability(var, varTree);
        commonAssignmentCheck(var, valueExp, errorKey);
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value
     * to a variable and emits an error message (through the compiler's
     * messaging interface) if it is not valid.
     *
     * @param varType the annotated type of the variable
     * @param valueExp the AST node for the value
     * @param errorKey the error message to use if the check fails
     */
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            ExpressionTree valueExp, /*@CompilerMessageKey*/ String errorKey) {
        if (shouldSkipUses(valueExp))
            return;
        if (varType.getKind() == TypeKind.ARRAY
                && valueExp instanceof NewArrayTree
                && ((NewArrayTree)valueExp).getType() == null) {
            AnnotatedTypeMirror compType = ((AnnotatedArrayType)varType).getComponentType();
            NewArrayTree arrayTree = (NewArrayTree)valueExp;
            assert arrayTree.getInitializers() != null;
            checkArrayInitialization(compType, arrayTree.getInitializers());
        }
        AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedType(valueExp);
        assert valueType != null;
        commonAssignmentCheck(varType, valueType, valueExp, errorKey);
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value
     * to a variable and emits an error message (through the compiler's
     * messaging interface) if it is not valid.
     *
     * @param varType the annotated type of the variable
     * @param valueType the annotated type of the value
     * @param valueTree the location to use when reporting the error message
     * @param errorKey the error message to use if the check fails
     */
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey) {

        if (options.containsKey("showchecks")) {
            long valuePos = positions.getStartPosition(root, valueTree);
            System.out.printf(
                    " %s (line %3d): %s %s%n     actual: %s %s%n   expected: %s %s%n",
                    "About to test whether actual is a subtype of expected",
                    root.getLineMap().getLineNumber(valuePos),
                    valueTree.getKind(), valueTree,
                    valueType.getKind(), valueType,
                    varType.getKind(), varType);
        }

        boolean success = checker.isSubtype(valueType, varType);

        String valueTypeString = valueType.toString();
        String varTypeString = varType.toString();

        if (options.containsKey("showchecks")) {
            // In case of failure, if both types as strings are the same, try outputting
            // the type including also invisible qualifiers.
            // This usually means there is a mistake in type defaulting.
            // This code is therefore not covered by a test.
            if (!success && valueTypeString.equals(varTypeString)) {
                valueTypeString = valueType.toString(true);
                varTypeString = varType.toString(true);
            }

            long valuePos = positions.getStartPosition(root, valueTree);
            System.out.printf(
                    " %s (line %3d): %s %s%n     actual: %s %s%n   expected: %s %s%n",
                    (success ? "success: actual is subtype of expected" : "FAILURE: actual is not subtype of expected"),
                    root.getLineMap().getLineNumber(valuePos),
                    valueTree.getKind(), valueTree,
                    valueType.getKind(), valueTypeString,
                    varType.getKind(), varTypeString);
        }

        // Use an error key only if it's overridden by a checker.
        if (!success) {
            checker.report(Result.failure(errorKey,
                    valueTypeString, varTypeString), valueTree);
        }
    }

    protected void checkArrayInitialization(AnnotatedTypeMirror type,
            List<? extends ExpressionTree> initializers) {
        for (ExpressionTree init : initializers)
            commonAssignmentCheck(type, init, "type.incompatible");
    }

    /**
     * Checks that the annotations on the type arguments supplied to a type or a
     * method invocation are within the bounds of the type variables as
     * declared, and issues the "generic.argument.invalid" error if they are
     * not.
     *
     * @param toptree the tree for error reporting, only used for inferred type arguments
     * @param typevars the type variables from a class or method declaration
     * @param typeargs the type arguments from the type or method invocation
     * @param typeargTrees the type arguments as trees, used for error reporting
     */
    protected void checkTypeArguments(Tree toptree,
            List<? extends AnnotatedTypeVariable> typevars,
            List<? extends AnnotatedTypeMirror> typeargs,
            List<? extends Tree> typeargTrees) {

        // System.out.printf("BaseTypeVisitor.checkTypeArguments: %s, TVs: %s, TAs: %s, TATs: %s\n",
        //         toptree, typevars, typeargs, typeargTrees);

        // If there are no type variables, do nothing.
        if (typevars.isEmpty()) return;

        assert typevars.size() == typeargs.size() :
            "BaseTypeVisitor.checkTypeArguments: mismatch between type arguments: " +
            typeargs + " and type variables" + typevars;

        Iterator<? extends AnnotatedTypeVariable> varIter = typevars.iterator();
        Iterator<? extends AnnotatedTypeMirror> argIter = typeargs.iterator();

        while (varIter.hasNext()) {

            AnnotatedTypeVariable typeVar = varIter.next();
            AnnotatedTypeMirror typearg = argIter.next();

            // TODO skip wildcards for now to prevent a crash
            if (typearg.getKind() == TypeKind.WILDCARD) continue;

            if (typeVar.getEffectiveUpperBound() != null)  {
                if (typeargTrees == null || typeargTrees.isEmpty()) {
                    // The type arguments were inferred and we mark the whole method.
                    // The inference fails if we provide invalid arguments,
                    // therefore issue an error for the arguments.
                    // I hope this is less confusing for users.
                    commonAssignmentCheck(typeVar.getEffectiveUpperBound(), typearg,
                            toptree,
                            "type.argument.type.incompatible");
                } else {
                    commonAssignmentCheck(typeVar.getEffectiveUpperBound(), typearg,
                            typeargTrees.get(typeargs.indexOf(typearg)),
                            "type.argument.type.incompatible");
                }
            }

            // Should we compare lower bounds instead of the annotations on the type variables?
            if (!typeVar.getAnnotations().isEmpty()) {
                if (!typearg.getAnnotations().equals(typeVar.getAnnotations())) {
                    if (typeargTrees == null || typeargTrees.isEmpty()) {
                        // The type arguments were inferred and we mark the whole method.
                        checker.report(Result.failure("type.argument.type.incompatible",
                                typearg, typeVar),
                                toptree);
                    } else {
                        checker.report(Result.failure("type.argument.type.incompatible",
                                typearg, typeVar),
                                typeargTrees.get(typeargs.indexOf(typearg)));
                    }
                }
            }

        }
    }

    /**
     * Tests whether the method can be invoked using the receiver of the 'node'
     * method invocation, and issues a "method.invocation.invalid" if the
     * invocation is invalid.
     *
     * This implementation tests whether the receiver in the method invocation
     * is a subtype of the method receiver type.
     *
     * @param method    the type of the invoked method
     * @param node      the method invocation node
     * @return true iff the call of 'node' is a valid call
     */
    protected boolean checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
        AnnotatedTypeMirror treeReceiver = methodReceiver.getCopy(false);
        AnnotatedTypeMirror rcv = atypeFactory.getReceiverType(node);
        treeReceiver.addAnnotations(rcv.getEffectiveAnnotations());

        if (!checker.isSubtype(treeReceiver, methodReceiver)) {
            checker.report(Result.failure("method.invocation.invalid",
                TreeUtils.elementFromUse(node),
                treeReceiver.toString(), methodReceiver.toString()), node);
            return false;
        }
        return true;
    }

    protected boolean checkConstructorInvocation(AnnotatedDeclaredType dt,
            AnnotatedExecutableType constructor, Tree src) {
        AnnotatedDeclaredType receiver = constructor.getReceiverType();
        boolean b = checker.isSubtype(dt, receiver) || checker.isSubtype(receiver, dt);

        if (!b) {
            checker.report(Result.failure("constructor.invocation.invalid",
                    constructor.toString(), dt, receiver), src);
        }
        return b;
    }

    /**
     * A helper method to check that each passed argument is a subtype of the
     * corresponding required argument, and issues "argument.invalid" error
     * for each passed argument that not a subtype of the required one.
     *
     * Note this method requires the lists to have the same length, as it
     * does not handle cases like var args.
     *
     * @param requiredArgs  the required types
     * @param passedArgs    the expressions passed to the corresponding types
     */
    // This really should have a private final method
    // Unfortunately Javari override it!
    protected void checkArguments(List<? extends AnnotatedTypeMirror> requiredArgs,
            List<? extends ExpressionTree> passedArgs) {
        assert requiredArgs.size() == passedArgs.size();
        for (int i = 0; i < requiredArgs.size(); ++i) {
            commonAssignmentCheck(requiredArgs.get(i),
                    passedArgs.get(i),
                    "argument.type.incompatible");
        }
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
            Void p) {

        if (checker.shouldSkipUses(overriddenType.getElement())) {
            return true;
        }

        // Get the type of the overriding method.
        AnnotatedExecutableType overrider =
            atypeFactory.getAnnotatedType(overriderTree);

        boolean result = true;

        if (overrider.getTypeVariables().isEmpty() && !overridden.getTypeVariables().isEmpty()) {
            overridden = overridden.getErased();
        }
        String overriderMeth = overrider.getElement().toString();
        String overriderTyp = enclosingType.getUnderlyingType().asElement().toString();
        String overriddenMeth = overridden.getElement().toString();
        String overriddenTyp = overriddenType.getUnderlyingType().asElement().toString();

        // Check the return value.
        if ((overrider.getReturnType().getKind() != TypeKind.VOID)) {
            boolean success = checker.isSubtype(overrider.getReturnType(),
                overridden.getReturnType());
            if (options.containsKey("showchecks")) {
                long valuePos = positions.getStartPosition(root, overriderTree.getReturnType());
                System.out.printf(
                        " %s (line %3d):%n     overrider: %s %s (return type %s)%n   overridden: %s %s (return type %s)%n",
                        (success ? "success: overriding return type is subtype of overridden" : "FAILURE: overriding return type is not subtype of overridden"),
                        root.getLineMap().getLineNumber(valuePos),
                        overriderMeth, overriderTyp, overrider.getReturnType().toString(),
                        overriddenMeth, overriddenTyp, overridden.getReturnType().toString());
            }
            if (!success) {
                checker.report(Result.failure("override.return.invalid",
                        overriderMeth, overriderTyp,
                        overriddenMeth, overriddenTyp,
                        overrider.getReturnType().toString(),
                        overridden.getReturnType().toString()),
                        overriderTree.getReturnType());
                // emit error message
                result = false;
            }
        }

        // Check parameter values. (FIXME varargs)
        List<AnnotatedTypeMirror> overriderParams =
            overrider.getParameterTypes();
        List<AnnotatedTypeMirror> overriddenParams =
            overridden.getParameterTypes();
        for (int i = 0; i < overriderParams.size(); ++i) {
            boolean success = checker.isSubtype(overriddenParams.get(i), overriderParams.get(i));
            if (options.containsKey("showchecks")) {
                long valuePos = positions.getStartPosition(root, overriderTree.getParameters().get(i));
                System.out.printf(
                        " %s (line %3d):%n     overrider: %s %s (parameter %d type %s)%n   overridden: %s %s (parameter %d type %s)%n",
                        (success ? "success: overridden parameter type is subtype of overriding" : "FAILURE: overridden parameter type is not subtype of overriding"),
                        root.getLineMap().getLineNumber(valuePos),
                        overriderMeth, overriderTyp, i, overriderParams.get(i).toString(),
                        overriddenMeth, overriddenTyp, i, overriddenParams.get(i).toString());
            }
            if (!success) {
                checker.report(Result.failure("override.param.invalid",
                        overriderMeth, overriderTyp,
                        overriddenMeth, overriddenTyp,
                        overriderParams.get(i).toString(),
                        overriddenParams.get(i).toString()),
                               overriderTree.getParameters().get(i));
                // emit error message
                result = false;
            }
        }

        // Check the receiver type.
        // isSubtype() requires its arguments to be actual subtypes with
        // respect to JLS, but overrider receiver is not a subtype of the
        // overridden receiver.  Hence copying the annotations.
        // TODO: this will need to be improved for generic receivers.
        AnnotatedTypeMirror overriddenReceiver =
            overrider.getReceiverType().getErased().getCopy(false);
        overriddenReceiver.addAnnotations(overridden.getReceiverType().getAnnotations());
        if (!checker.isSubtype(overriddenReceiver,
                overrider.getReceiverType().getErased())) {
            checker.report(Result.failure("override.receiver.invalid",
                    overriderMeth, overriderTyp, overriddenMeth, overriddenTyp,
                    overrider.getReceiverType(),
                    overridden.getReceiverType()),
                    overriderTree);
            result = false;
        }
        return result;
    }

    /**
     * Tests, for a re-assignment, whether the variable is assignable or not.
     * If not, it emits an assignability.invalid error.
     *
     * @param varType   the type of the variable being re-assigned
     * @param varTree   the tree used to access the variable in the assignment
     */
    protected void checkAssignability(AnnotatedTypeMirror varType, Tree varTree) {
        if (varTree instanceof ExpressionTree &&
                !checker.isAssignable(varType,
                        atypeFactory.getReceiverType((ExpressionTree)varTree),
                        varTree,
                        atypeFactory)) {
            checker.report(
                    Result.failure("assignability.invalid",
                            InternalUtils.symbol(varTree),
                            atypeFactory.getReceiverType((ExpressionTree)varTree)),
                    varTree);
        }
    }

    protected MemberSelectTree enclosingMemberSelect() {
        TreePath path = this.getCurrentPath();
        assert path.getLeaf().getKind() == Tree.Kind.IDENTIFIER;
        if (path.getParentPath().getLeaf().getKind() == Tree.Kind.MEMBER_SELECT)
            return (MemberSelectTree)path.getParentPath().getLeaf();
        else
            return null;
    }

    protected Tree enclosingStatement(Tree tree) {
        TreePath path = this.getCurrentPath();
        while (path != null && path.getLeaf() != tree)
            path = path.getParentPath();

        if (path != null)
            return path.getParentPath().getLeaf();
        else
            return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        checkAccess(node, p);
        return super.visitIdentifier(node, p);
    }

    protected void checkAccess(IdentifierTree node, Void p) {
        MemberSelectTree memberSel = enclosingMemberSelect();
        ExpressionTree tree;
        Element elem;

        if (memberSel == null) {
            tree = node;
            elem = TreeUtils.elementFromUse(node);
        } else {
            tree = memberSel;
            elem = TreeUtils.elementFromUse(memberSel);
        }

        if (elem == null || !elem.getKind().isField())
            return;

        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(tree);

        if (!isAccessAllowed(elem, receiver, tree)) {
            checker.report(Result.failure("unallowed.access", elem, receiver), node);
        }
    }

    protected boolean isAccessAllowed(Element field, AnnotatedTypeMirror receiver, ExpressionTree accessTree) {
        AnnotationMirror unused = atypeFactory.getDeclAnnotation(field, Unused.class);
        if (unused == null)
            return true;

        String when = AnnotationUtils.elementValueClassName(unused, "when");
        if (receiver.getAnnotation(when) == null)
            return true;

        Tree tree = this.enclosingStatement(accessTree);

        // assigning unused to null is OK
        return (tree != null
                && tree.getKind() == Tree.Kind.ASSIGNMENT
                && ((AssignmentTree)tree).getVariable() == accessTree
                && ((AssignmentTree)tree).getExpression().getKind() == Tree.Kind.NULL_LITERAL);
    }

    /**
     * Tests that the qualifiers present on the useType are valid qualifiers,
     * given the qualifiers on the declaration of the type, declarationType.
     *
     * <p>
     *
     * The check is shallow, as it does not descend into generic or array
     * types (i.e. only performing the validity check on the raw type or
     * outermost array dimension).  {@link BaseTypeVisitor#validateTypeOf(Tree)}
     * would call this for each type argument or array dimension separately.
     *
     * <p>
     *
     * For instance, in the IGJ type system, a {@code @Mutable} is an invalid
     * qualifier for {@link String}, as {@link String} is declared as
     * {@code @Immutable String}.
     *
     * <p>
     *
     * In most cases, {@code useType} simply needs to be a subtype of
     * {@code declarationType}, but there are exceptions.  In IGJ, a variable may be
     * declared {@code @ReadOnly String}, even though {@link String} is
     * {@code @Immutable String};  {@link ReadOnly} is not a subtype of
     * {@link Immutable}.
     *
     * @param declarationType  the type of the class (TypeElement)
     * @param useType   the use of the class (instance type)
     * @return  if the useType is a valid use of elemType
     */
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        return checker.isSubtype(useType.getErased(), declarationType.getErased());
    }

    /**
     * Tests that the qualifiers present on the primitive type are valid.
     *
     * The default implementation always returns true.
     * Subclasses should override this method to limit what annotations are
     * allowed on primitive types.
     */
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        return true;
    }

    /**
     * Tests that the qualifiers present on the array type are valid.
     * This method will be invoked for each array level independently, i.e. this
     * method only needs to check the top-level qualifiers of an array.
     *
     * The default implementation always returns true.
     * Subclasses should override this method to limit what annotations are
     * allowed on array types.
     */
    public boolean isValidUse(AnnotatedArrayType type) {
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
        case UNBOUNDED_WILDCARD:
        case EXTENDS_WILDCARD:
        case SUPER_WILDCARD:
        case ANNOTATED_TYPE:
            type = atypeFactory.getAnnotatedTypeFromTypeTree(tree);
            break;
        default:
            type = atypeFactory.getAnnotatedType(tree);
        }
        typeValidator.visit(type, tree);
    }

    // This is a test to ensure that all types are valid
    protected final TypeValidator typeValidator = createTypeValidator();

    protected TypeValidator createTypeValidator() {
        return new TypeValidator();
    }

    protected class TypeValidator extends AnnotatedTypeScanner<Void, Tree> {
        protected void reportError(AnnotatedTypeMirror type, Tree p) {
            checker.report(Result.failure("type.invalid",
                        type.getAnnotations(), type.toString()), p);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
            if (checker.shouldSkipUses(type.getElement()))
                return super.visitDeclared(type, tree);

            {
                // Ensure that type use is a subtype of the element type
                // isValidUse determines the erasure of the types.
                AnnotatedDeclaredType elemType = (AnnotatedDeclaredType)
                        atypeFactory.getAnnotatedType(
                                type.getUnderlyingType().asElement());

                if (!isValidUse(elemType, type)) {
                    reportError(type, tree);
                }
            }

            // System.out.println("Type: " + type);
            // System.out.println("Tree: " + tree);
            // System.out.println("Tree kind: " + tree.getKind());

            /* Try to reconstruct the ParameterizedTypeTree from the given tree.
             * TODO: there has to be a nicer way to do this...
             */
            Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p = extractParameterizedTypeTree(tree, type);
            ParameterizedTypeTree typeargtree = p.first;
            type = p.second;

            if (typeargtree!=null) {
                // We have a ParameterizedTypeTree -> visit it.

                visitParameterizedType(type, typeargtree);

                /* Instead of calling super with the unchanged "tree", adapt the second
                 * argument to be the corresponding type argument tree.
                 * This ensures that the first and second parameter to this method always correspond.
                 * visitDeclared is the only method that had this problem.
                 */
                List<? extends AnnotatedTypeMirror> tatypes = type.getTypeArguments();

                if (tatypes == null)
                    return null;

                // May be zero for a "diamond" (inferred type args in constructor invocation).
                int numTypeArgs = typeargtree.getTypeArguments().size();
                if (numTypeArgs != 0) {
                    assert tatypes.size() == numTypeArgs;

                    for (int i=0; i < tatypes.size(); ++i) {
                        scan(tatypes.get(i), typeargtree.getTypeArguments().get(i));
                    }
                }

                return null;

                // Don't call the super version, because it creates a mismatch between
                // the first and second parameters.
                // return super.visitDeclared(type, tree);
            }

            return super.visitDeclared(type, tree);
        }

        private Pair<ParameterizedTypeTree, AnnotatedDeclaredType>
        extractParameterizedTypeTree(Tree tree, AnnotatedDeclaredType type) {
            ParameterizedTypeTree typeargtree = null;

            switch (tree.getKind()) {
            case VARIABLE:
                Tree lt = ((VariableTree)tree).getType();
                if (lt instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) lt;
                } else {
                  //   System.out.println("Found a: " + lt);
                }
                break;
            case PARAMETERIZED_TYPE:
                typeargtree = (ParameterizedTypeTree) tree;
                break;
            case NEW_CLASS:
                NewClassTree nct = (NewClassTree) tree;
                ExpressionTree nctid = nct.getIdentifier();
                if (nctid.getKind()==Tree.Kind.PARAMETERIZED_TYPE) {
                    typeargtree = (ParameterizedTypeTree) nctid;
                    /*
                     * This is quite tricky... for anonymous class instantiations,
                     * the type at this point has no type arguments.
                     * By doing the following, we get the type arguments again.
                     */
                    type = (AnnotatedDeclaredType) atypeFactory.getAnnotatedType(typeargtree);
                }
                break;
            case ANNOTATED_TYPE:
                AnnotatedTypeTree tr = (AnnotatedTypeTree) tree;
                ExpressionTree undtr = tr.getUnderlyingType();
                if (undtr instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) undtr;
                } else if (undtr instanceof IdentifierTree) {
                    // @Something D -> Nothing to do
                } else {
                    // TODO: add more test cases to ensure that nested types are handled correctly,
                    // e.g. @Nullable() List<@Nullable Object>[][]
                    Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p = extractParameterizedTypeTree(undtr, type);
                    typeargtree = p.first;
                    type = p.second;
                }
                break;
            case IDENTIFIER:
            case ARRAY_TYPE:
            case NEW_ARRAY:
            case MEMBER_SELECT:
            case UNBOUNDED_WILDCARD:
            case EXTENDS_WILDCARD:
            case SUPER_WILDCARD:
            case TYPE_PARAMETER:
                // Nothing to do.
                // System.out.println("Found a: " + (tree instanceof ParameterizedTypeTree));
                break;
            default:
                System.err.printf("TypeValidator.visitDeclared unhandled tree: %s of kind %s\n", tree, tree.getKind());
            }

            return Pair.of(typeargtree, type);
        }


        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
            if (checker.shouldSkipUses(type.getElement()))
                return super.visitPrimitive(type, tree);

            if (!isValidUse(type)) {
                reportError(type, tree);
            }

            return super.visitPrimitive(type, tree);
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Tree tree) {
            if (checker.shouldSkipUses(type.getElement()))
                return super.visitArray(type, tree);

            if (!isValidUse(type)) {
                reportError(type, tree);
            }

            return super.visitArray(type, tree);
        }

        /**
         * Checks that the annotations on the type arguments supplied to a
         * type or a method invocation are within the bounds of the type
         * variables as declared, and issues the "generic.argument.invalid"
         * error if they are not.
         *
         * This method used to be visitParameterizedType, which incorrectly
         * handles the main annotation on generic types.
         */
        protected Void visitParameterizedType(AnnotatedDeclaredType type, ParameterizedTypeTree tree) {
            // System.out.printf("TypeValidator.visitParameterizedType: type: %s, tree: %s\n", type, tree);

            if (TreeUtils.isDiamondTree(tree))
                return null;

            final TypeElement element = (TypeElement) type.getUnderlyingType().asElement();
            if (checker.shouldSkipUses(element))
                return null;

            List<AnnotatedTypeVariable> typevars = atypeFactory.typeVariablesFromUse(type, element);

            checkTypeArguments(tree, typevars, type.getTypeArguments(), tree.getTypeArguments());

            return null;
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, Tree tree) {
            // Keep in sync with visitWildcard
            Set<AnnotationMirror> onVar = type.getAnnotations();
            if (!onVar.isEmpty()) {
                // System.out.printf("BaseTypeVisitor.TypeValidator.visitTypeVariable(type: %s, tree: %s)",
                //         type, tree);

                if (type.getUpperBoundField()!=null) {
                    Set<AnnotationMirror> onUpper = type.getUpperBound().getAnnotations();
                    if (!checker.getQualifierHierarchy().isSubtype(onVar, onUpper)) {
                        this.reportError(type, tree);
                    }
                }

                if (type.getLowerBoundField()!=null) {
                    Set<AnnotationMirror> onLower = type.getLowerBound().getAnnotations();
                    if (!onLower.isEmpty() &&
                        !checker.getQualifierHierarchy().isSubtype(onLower, onVar)) {
                        this.reportError(type, tree);
                    }
                }
            }

            return super.visitTypeVariable(type, tree);
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, Tree tree) {
            // Keep in sync with visitTypeVariable
            Set<AnnotationMirror> onVar = type.getAnnotations();
            if (!onVar.isEmpty()) {
                // System.out.printf("BaseTypeVisitor.TypeValidator.visitWildcard(type: %s, tree: %s)",
                //         type, tree);

                if (type.getExtendsBound()!=null) {
                    Set<AnnotationMirror> onUpper = type.getExtendsBound().getAnnotations();
                    if (!checker.getQualifierHierarchy().isSubtype(onVar, onUpper)) {
                        this.reportError(type, tree);
                    }
                }

                if (type.getSuperBound()!=null) {
                    Set<AnnotationMirror> onLower = type.getSuperBound().getAnnotations();
                    if (!onLower.isEmpty() &&
                        !checker.getQualifierHierarchy().isSubtype(onLower, onVar)) {
                        this.reportError(type, tree);
                    }
                }
            }
            return super.visitWildcard(type, tree);
        }
    }

    // **********************************************************************
    // Random helper methods
    // **********************************************************************

    /**
     * @param node the method invocation to check
     * @return true if this is a super call to the {@link Enum} constructor
     */
    private boolean isEnumSuper(MethodInvocationTree node) {
        ExecutableElement ex = TreeUtils.elementFromUse(node);
        Name name = ElementUtils.getQualifiedClassName(ex);
        return "java.lang.Enum".contentEquals(name);
    }

    /**
     * Tests whether the expression should not be checked because of the tree
     * referring to unannotated classes, as specified in
     * the {@code checker.skipUses} property.
     *
     * It returns true if exprTree is a method invocation or a field access
     * to a class whose qualified name matches @{link checker.skipUses}
     * expression.
     *
     * @param exprTree  any expression tree
     * @return true if checker should not test exprTree
     */
    protected final boolean shouldSkipUses(ExpressionTree exprTree) {
        // System.out.printf("shouldSkipUses: %s: %s%n", exprTree.getClass(), exprTree);

        // This special case for ConditionalExpressionTree seems wrong, so
        // I commented it out.  It will skip expressions that should be
        // checked, just because they are lexically near expressions that
        // should be skipped.  Presumably it's because conditionals do some
        // type inference, but if so, this is the wrong way to fix the
        // problem. -MDE
        // if (exprTree instanceof ConditionalExpressionTree) {
        //     ConditionalExpressionTree condTree =
        //         (ConditionalExpressionTree)exprTree;
        //     return (shouldSkipUses(condTree.getTrueExpression()) ||
        //             shouldSkipUses(condTree.getFalseExpression()));
        // }

        Element elm = InternalUtils.symbol(exprTree);
        return checker.shouldSkipUses(elm);
    }


    // **********************************************************************
    // Overriding to avoid visit part of the tree
    // **********************************************************************

    /**
     * Override Compilation Unit so we won't visit package names or imports
     */
    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
        Void r = scan(node.getPackageAnnotations(), p);
        // r = reduce(scan(node.getPackageName(), p), r);
        // r = reduce(scan(node.getImports(), p), r);
        r = reduce(scan(node.getTypeDecls(), p), r);
        return r;
    }


    // **********************************************************************
    // Check that the annotated JDK is being used.
    // **********************************************************************


    private static boolean checkedJDK = false;

    // Not all subclasses call this -- only those that have an annotated JDK.
    /** Warn if the annotated JDK is not being used. */
    protected void checkForAnnotatedJdk() {
        if (checkedJDK) {
            return;
        }
        checkedJDK = true;
        if (options.containsKey("nocheckjdk")) {
            return;
        }
        TypeElement objectTE = elements.getTypeElement("java.lang.Object");
        List<? extends Element> members = elements.getAllMembers(objectTE);

        for (Element member : members) {
            if (member.toString().equals("equals(java.lang.Object)")) {
                ExecutableElement m = (ExecutableElement) member;
                // The Nullness JDK serves as a proxy for all annotated
                // JDKs.  (In part because of problems with
                // IGJAnnotatedTypeFactory.postAsMemberOf that make it hard
                // to directly check for the IGJ annotated JDK.)

                // Note that we cannot use the AnnotatedTypeMirrors from the
                // Checker Framework, because those only return the annotations
                // that are used by the current checker.
                // That is, if this code is executed by something other than the
                // Nullness Checker, we would not find the annotations.
                // Therefore, we go to the Element and get all annotations on
                // the parameter.

                // TODO: doing types.typeAnnotationOf(m.getParameters().get(0).asType(), Nullable.class)
                // or types.typeAnnotationsOf(m.asType())
                // does not work any more. It should.

                boolean foundNN = false;
                for (com.sun.tools.javac.code.Attribute.TypeCompound tc :
                        ((com.sun.tools.javac.code.Symbol)m).typeAnnotations) {
                    if ( tc.position.type==com.sun.tools.javac.code.TargetType.METHOD_PARAMETER &&
                            tc.position.parameter_index==0 &&
                            tc.type.toString().equals(checkers.nullness.quals.Nullable.class.getName()) ) {
                        foundNN = true;
                    }
                }

                if (!foundNN) {
                    checker.getProcessingEnvironment().getMessager().printMessage(Kind.WARNING,
                        "You do not seem to be using the distributed annotated JDK.  To fix the" +
                        System.getProperty("line.separator") +
                        "problem, supply this argument (first, fill in the \"...\") when you run javac:" +
                        System.getProperty("line.separator") +
                        "  -Xbootclasspath/p:.../checkers/jdk/jdk.jar");
                }
            }
        }
    }
}
