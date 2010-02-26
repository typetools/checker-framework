package checkers.subtype;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;

import checkers.quals.*;
import checkers.source.*;
import checkers.types.*;
import checkers.util.*;

import com.sun.tools.javac.code.*;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * A type-checking visitor for type qualifiers for which the qualified type
 * is the subtype of the unqualified type.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public abstract class SubtypeVisitor extends SourceVisitor<Void, Void> {

    /** The checker associated with this visitor. */
    protected final SubtypeChecker checker;

    /**
     * Creates a visitor for checking subtype qualifiers, using the given
     * checker, which will operate on the given syntax tree root.
     *
     * @param checker the checker associated with this visitor
     * @param root the root of the syntax tree to check
     */
    public SubtypeVisitor(SubtypeChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.checker = checker;
    }

    /**
     * @see com.sun.source.util.TreeScanner#visitAssignment(com.sun.source.tree.AssignmentTree,
     *      java.lang.Object)
     */
    @Override
    public @Nullable Void visitAssignment(AssignmentTree node, Void p) {

        for (Tree t : this.getCurrentPath())
            if (t.getKind() == Tree.Kind.ANNOTATION)
                return super.visitAssignment(node, p);

        AnnotatedClassType expression = factory.getClass(node.getExpression());
        AnnotatedClassType variable = factory.getClass(node.getVariable());

        if (!checker.isSubtype(expression, variable)) {
            checker.report(Result.failure("assignment.invalid", 
                        variable.toCondensedString(), 
                        expression.toCondensedString()), node
                    .getExpression());
        }

        return super.visitAssignment(node, p);
    }

    /**
     * @see com.sun.source.util.TreeScanner#visitVariable(com.sun.source.tree.VariableTree,
     *      java.lang.Object)
     */
    @Override
    public @Nullable Void visitVariable(VariableTree node, Void p) {

        if (node.getInitializer() == null)
            return super.visitVariable(node, p);

        AnnotatedClassType init = factory.getClass(node.getInitializer());
        AnnotatedClassType variable = factory.getClass(node);

        if (!checker.isSubtype(init, variable))
            checker.report(Result.failure("assignment.invalid", 
                        variable.toCondensedString(),
                        init.toCondensedString()), variable
                    .getElement());

        return super.visitVariable(node, p);
    }

    @Override
    public @Nullable Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {

        AnnotatedClassType expression = factory.getClass(node.getExpression());
        AnnotatedClassType variable = factory.getClass(node.getVariable());

        if (!checker.isSubtype(expression, variable)) {
            checker.report(Result.failure("assignment.compound.invalid"), node
                    .getExpression());
        }

        return super.visitCompoundAssignment(node, p);
    }

    /**
     * @see com.sun.source.util.TreeScanner#visitMethodInvocation(com.sun.source.tree.MethodInvocationTree,
     *      java.lang.Object)
     */
    @Override
    public @Nullable Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        AnnotatedMethodType method = factory.getMethod(node);
        List<AnnotatedClassType> parameters = method
                .getAnnotatedParameterTypes();

        ExecutableElement m = method.getElement();
        
        // Skip checking method invocations (i.e., super) inside the
        // (synthetic) constructors of anonymous inner classes, since they're
        // synthesized without any annotations on their arguments.
        @Nullable TreePath path = TreePath.getPath(root, node);
        assert path != null; /*nninvariant*/
        if (InternalUtils.isAnonymousConstructor(path))
            return super.visitMethodInvocation(node, p);

        List<AnnotatedClassType> arguments = new LinkedList<AnnotatedClassType>();
        for (ExpressionTree arg : node.getArguments())
            arguments.add(factory.getClass(arg));

        int maxParam = (m.isVarArgs() ? parameters.size() - 1 : parameters.size());
        
        for (int i = 0; i < maxParam && i < arguments.size(); i++) {
            if (!checker.isSubtype(arguments.get(i), parameters.get(i))) {
                @Nullable Tree argTree = node.getArguments().get(i);
                assert argTree != null; /*nninvariant*/
                checker.report(Result.failure("argument.invalid", 
                            parameters.get(i).toCondensedString(),
                            arguments.get(i).toCondensedString()), argTree);
            }
        }

        // MemberSelectTree has an identifier and expression ("a" and "b"
        // respectively for "a.b").
        if (node.getMethodSelect() instanceof MemberSelectTree) {
            MemberSelectTree ms = (MemberSelectTree) node.getMethodSelect();
            AnnotatedClassType receiver = factory.getClass(ms.getExpression());

            Element methodElt = method.getAnnotatedReceiverType().getElement();

            String className = InternalUtils.getQualifiedName(methodElt);
            if (checker.shouldSkip(className))
                return super.visitMethodInvocation(node, p);

            // TODO *** probably shouldn't be here?
            ExpressionTree expr = ms.getExpression();
            if (expr != null) {
                @Nullable Element exprElt = InternalUtils.symbol(expr);
                if (exprElt != null && exprElt.getSimpleName().contentEquals("this"))
                    return super.visitMethodInvocation(node, p);

            }

            if (!checker.isSubtypeIgnoringTypeParameters(receiver, method.getAnnotatedReceiverType()))
                checker.report(Result.failure("receiver.invalid", 
                            method.getAnnotatedReturnType().toCondensedString(),
                            receiver.toCondensedString()), ms);
        }

        return super.visitMethodInvocation(node, p);

    }

    /**
     * @see com.sun.source.util.TreeScanner#visitReturn(com.sun.source.tree.ReturnTree,java.lang.Object)
     */
    // (The second parameter in the above @see expression is Void, not Object,
    // but the javadoc tool fails to recognize it with the correct type.)
    @Override
    public @Nullable Void visitReturn(ReturnTree node, Void p) {

        if (node.getExpression() == null)
            return super.visitReturn(node, p);

        AnnotatedClassType ret = factory.getClass(node.getExpression());
        @Nullable TreePath path = TreePath.getPath(root, node);
        assert path != null; /*nninvariant*/
        @Nullable Tree enclosingMethod = InternalUtils.enclosingMethod(path);

        AnnotatedMethodType meth = factory.getMethod(enclosingMethod);

        if (!checker.isSubtype(ret, meth.getAnnotatedReturnType()))
            checker.report(Result.failure("return.invalid", 
                        meth.getAnnotatedReturnType().toCondensedString(),
                        ret.toCondensedString()), node);

        return super.visitReturn(node, p);
    }

    private void closureMethods(TypeMirror type,
            Map<Element, @NonNull List<? extends Element>> methods) {
        for (TypeMirror t : types.directSupertypes(type)) {
            @Nullable Element te = types.asElement(t);
            if (te != null) /*nnbug*/
                methods.put(te, te.getEnclosedElements());
            closureMethods(t, methods);
        }
    }

    @Override
    public @Nullable Void visitTypeCast(TypeCastTree node, Void p) {

        @Nullable Element ex = InternalUtils.symbol(node.getExpression());
        @Nullable Element type = InternalUtils.symbol(node.getType());

        if (ex != null)
            if (type != null && !ex.asType().equals(type.asType())) // FIXME: flow workaround
                return super.visitTypeCast(node, p);

        AnnotatedClassType cast = factory.getClass(node);

        if (checker.getLintOption("cast:redundant", false)) {
            AnnotatedClassType expr = factory.getClass(node.getExpression());
            if (expr.hasAnnotationAt(checker.annotation, AnnotationLocation.RAW, false) &&
                    cast.hasAnnotationAt(checker.annotation, AnnotationLocation.RAW, false)) {
                checker.report(Result.warning("cast.redundant"), node);
                return super.visitTypeCast(node, p);
            }
        }

        if (checker.getLintOption("cast", true)) {
            if (cast.hasAnnotationAt(checker.annotation, AnnotationLocation.RAW, false))
                checker.report(Result.warning("cast.annotated"), node);
        }

        return super.visitTypeCast(node, p);
    }

    /**
     * A utility method that takes the element for a class/interface
     * type and returns a set of {@link TypeMirror}s representing all
     * of the supertypes of that type.
     *
     * @param subtype the element of the type for which all supertypes
     *                will be obtained
     * @return an unmodifiable set of supertypes for {@code subtype},
     *         as {@link TypeMirror}s
     */
    @Deprecated
    private Set<TypeMirror> superTypes(TypeElement subtype) {

        Set<TypeMirror> supertypes = new HashSet<TypeMirror>();

        // Set up a stack containing the type mirror of subtype, which
        // is our starting point.
        Deque<TypeMirror> stack = new ArrayDeque<TypeMirror>();
        stack.push(subtype.asType());

        while (!stack.isEmpty()) {
            TypeMirror current = stack.pop();

            // For each direct supertype of the current type, if it
            // hasn't already been visited, push it onto the stack and
            // add it to our supertypes set.
            for (TypeMirror supertype : types.directSupertypes(current)) {
                if (!supertypes.contains(supertype)) {
                    stack.push(supertype);
                    supertypes.add(supertype);
                }
            }
        }

        return Collections.<@NonNull TypeMirror>unmodifiableSet(supertypes);
    }

    /**
     * A utility method that takes the element for a method and the set
     * of all supertypes of the method's containing class and returns
     * the set of all elements that method overrides (as {@link
     * ExecutableElement}s).
     *
     * @param method the overriding method
     * @param supertypes the set of supertypes to check for methods
     *        that are overriden by {@code method}
     * @return an unmodified set of {@link ExecutableElements}
     *         representing the elements that {@code} method overrides
     *         among {@code supertypes}
     */
    private Set<ExecutableElement> overriddenMethods(ExecutableElement method,
            Set<TypeMirror> supertypes) {

        Set<ExecutableElement> overrides = new HashSet<ExecutableElement>();

        for (TypeMirror supertype : supertypes) {

            // Get the element for the supertype so we can see what
            // methods it contains.
            @Nullable TypeElement supertypeElt = (TypeElement)this.types.asElement(supertype);
            if (supertypeElt == null) break; /*nnbug*/

            // For all method in the supertype, add it to the set if
            // it overrides the given method.
            for (ExecutableElement supermethod :
                    methodsIn(supertypeElt.getEnclosedElements())) {
                if (this.elements.overrides(method, supermethod, supertypeElt)) {
                    overrides.add(supermethod);
                    break;
                }
            }
        }

        return Collections.<@NonNull ExecutableElement>unmodifiableSet(overrides);
    }

    @Override
    public @Nullable Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {

        GenericsUtils g = 
            new GenericsUtils(checker.getProcessingEnvironment(), factory);
        @Nullable Element elt = InternalUtils.symbol(tree.getExpression());
        @Nullable TypeMirror iterableType = g.iteratedType(tree);

        // TODO: finish me

        return super.visitEnhancedForLoop(tree, p);
    }

    /**
     * A utility method that takes the tree node for a method and the
     * element of a method that it overrides and produces an error if
     * the overriding is not valid with respect to the annotations on
     * each.
     *
     * @param overrider the tree node for an overriding method
     * @param overridden the element of that {@code overrider}
     *        overrides
     */
    private void checkOverride(MethodTree overrider,
                               ExecutableElement overridden) {

        String className = InternalUtils.getQualifiedName(overridden);
        if (checker.shouldSkip(className))
            return;

        // Get the AnnotatedMethodTypes for each.
        AnnotatedMethodType method = factory.getMethod(overrider);
        AnnotatedMethodType superMethod = factory.getMethod(overridden);

        @Nullable Element classElt = method.getElement().getEnclosingElement();
        assert classElt != null; /*nninvariant*/
            
        GenericsUtils g = 
            new GenericsUtils(checker.getProcessingEnvironment(), factory);

        Set<AnnotationData> methodReturn =
            method.getAnnotatedReturnType().getAnnotationData(true);
        Set<AnnotationData> superReturn =
            g.annotationsFor(overridden.getReturnType(), classElt);

        for (AnnotationData a : superReturn)
            if (!methodReturn.contains(a)) {
                checker.report(Result.failure("override.return.invalid"),
                        overrider);
                break;
            }
        
        // Get the parameters for both methods.
        List<AnnotatedClassType> methodParams =
            method.getAnnotatedParameterTypes();
        List<AnnotatedClassType> superMethodParams =
            superMethod.getAnnotatedParameterTypes();

        assert methodParams.size() == superMethodParams.size();

        // The overrider's parameter types must be identical to those
        // of the overriden.
        for (int i = 0; i < superMethodParams.size(); i++) {

            AnnotatedClassType methodParam = methodParams.get(i);

            @Nullable VariableElement v = overridden.getParameters().get(i);
            if (v == null || (v != null && v.asType() == null)) /*nnbug*/ // FIXME: flow workaround
                continue;

            assert v != null; // FIXME: flow workaround
            @Nullable TypeMirror vType = v.asType();
            assert vType != null; // FIXME: flow workaround
            Set<AnnotationData> superAnnos = g.annotationsFor(vType, classElt); // FIXME: checker bug (flow)
            Set<AnnotationData> methodAnnos = methodParam.getAnnotationData(true);
  
            Set<AnnotationData> intersection = new HashSet<AnnotationData>();
            intersection.addAll(superAnnos);
            if (intersection.retainAll(methodAnnos))
                checker.report(Result.failure("override.param.invalid"),
                        methodParam.getElement());
        }
    }

    @Override
    public @Nullable Void visitMethod(MethodTree node, Void p) {

        // Get the current path, and use it to obtain the element for the
        // enclosing class (via the scope) and the element for the current
        // method.
        @Nullable TreePath path = this.getCurrentPath();

        ExecutableElement method;
        {
            @Nullable Element e = this.trees.getElement(path);
            assert e instanceof ExecutableElement; /*nninvariant*/
            method = (ExecutableElement)e;
        }

        AnnotatedMethodType amt = factory.getMethod(node);
        if (method.getModifiers().contains(Modifier.STATIC) &&
                amt.getAnnotatedReceiverType().hasAnnotationAt(checker.getAnnotation(),
                    AnnotationLocation.RAW))
            checker.report(Result.failure("missing.this"), node);
        
        // FIXME: Ideally, the commented code below would be used, but it
        // produces "cannot find symbol: java" errors.
//        Scope scope = this.trees.getScope(path);
//        TypeElement enclosingClass = scope.getEnclosingClass();

        @Nullable TypeElement enclosingClass = (TypeElement)method.getEnclosingElement();
        assert enclosingClass != null; /*nninvariant*/

        // Use utility methods to obtain the set of methods that this one
        // overrides.
        Set<TypeMirror> supertypes = superTypes(enclosingClass);
        Set<ExecutableElement> overridden = overriddenMethods(method, supertypes);


        // Check each that the overriding is valid for each.
        for (ExecutableElement ex : overridden)
            checkOverride(node, ex);

        return super.visitMethod(node, p);
    }
}
