package checkers.javari;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;

import java.lang.annotation.Annotation;

import checkers.source.*;
import checkers.subtype.*;
import checkers.types.*;
import checkers.util.GenericsUtils;

import checkers.javari.VisitorState;
import checkers.javari.VisitorState.State;

import checkers.quals.Mutable;
import checkers.quals.ReadOnly;
import checkers.quals.RoMaybe;
import checkers.quals.Assignable;
import checkers.quals.QReadOnly;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * A type-checking visitor for the Javari mutability annotations
 * ({@code @ReadOnly}, {@code @Mutable} and {@code @Assignable}) that
 * extends SourceVisitor.
 *
 * @see SourceVisitor
 */
public class JavariVisitor extends SourceVisitor<Void, VisitorState> {

    /** Whether or not system classes should not be checked against. */
    protected final boolean SKIP_SYSTEM_CLASSES =
            Boolean.valueOf(System.getProperty("checkers.skipSystemClasses", "true"));

    protected JavariChecker checker;

    protected TreePath rootPath;

    protected final Map<Element, Tree> treeMap = new HashMap<Element, Tree>();
    protected final Map<Tree, ClassTree> classMap =
        new HashMap<Tree, ClassTree>();
    protected final Map<Tree, MethodTree> methodMap =
        new HashMap<Tree, MethodTree>();


    /**
     * Creates a new visitor for type-checking the Javari mutability
     * annotations.
     *
     * @param checker the {@link JavariChecker} to use
     * @param root the root of the input program's AST to check
     */
    public JavariVisitor(JavariChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.checker = checker;
        rootPath = new TreePath(root);
        root.accept(new TreeMapperVisitor(treeMap, classMap, methodMap), null);
    }


    /**
     * Modifies p to the approriate values, according to super
     * classes, interfaces, and outer classes.
     *
     * @see com.sun.source.util.TreeScanner#visitClass(com.sun.source.tree.ClassTree,java.lang.Object)
     */
    @Override
    public Void visitClass(ClassTree node, VisitorState p) {
        boolean readonly = false, mutable = false, romaybe = false;

        if (p == null){
            p = new VisitorState(null);
        } else {
            VisitorState pNew = new VisitorState(null);
            pNew.constructor = p.constructor;
            pNew.state = p.state;
            pNew.classState = p.classState;
            p = pNew;
        }

        AnnotatedClassType type = factory.getClass(node);

        Tree ext = node.getExtendsClause();
        if (ext != null){
            AnnotatedClassType extendsType = factory.getClass(ext);

            if (!checker.isSubtype(extendsType, type))
                checker.report(Result.failure("extends.invalid"),
                               node.getExtendsClause());

            if (extendsType.hasAnnotationAt(ReadOnly.class,
                                            AnnotationLocation.RAW))
                readonly = true;
            if (extendsType.hasAnnotationAt(Mutable.class,
                                            AnnotationLocation.RAW))
                mutable = true;
            if (extendsType.hasAnnotationAt(RoMaybe.class,
                                            AnnotationLocation.RAW))
                romaybe = true;
        }

        List<? extends Tree> imps = node.getImplementsClause();
        for(Tree it : imps) {
            AnnotatedClassType iType = factory.getClass(it);
            if (!checker.isSubtype(iType, type))
                checker.report(Result.failure("implements.invalid"), it);

            if (iType.hasAnnotationAt(ReadOnly.class,
                                      AnnotationLocation.RAW))
                readonly = true;
            if (iType.hasAnnotationAt(Mutable.class,
                                      AnnotationLocation.RAW))
                mutable = true;
            if (iType.hasAnnotationAt(RoMaybe.class,
                                      AnnotationLocation.RAW))
                romaybe = true;
        }

        if (type.hasAnnotationAt(ReadOnly.class, AnnotationLocation.RAW))
            readonly = true;
        else if (type.hasAnnotationAt(Mutable.class, AnnotationLocation.RAW))
            mutable = true;
        else if (type.hasAnnotationAt(RoMaybe.class, AnnotationLocation.RAW))
            romaybe = true;

        if (!(readonly || mutable || romaybe))
            mutable = true;

        if (romaybe) {
            if (readonly || mutable)
                checker.report(Result.failure("romaybe.only"), node);
            MethodTree mt = methodMap.get(node);
            if (mt == null || p.state != State.RO_MAYBE)
                checker.report(Result.failure("romaybe.type"), node);
            p.setClassState(State.RO_MAYBE);

        } else if (readonly && mutable) {
            checker.report(Result.failure("ro.and.mutable"), node);
            p.setClassState(State.MUTABLE);
        } else if (readonly)
            p.setClassState(State.READONLY);
        else
            p.setClassState(State.MUTABLE);


        p.annotatedClassType = type;

        return super.visitClass(node, p);
    }

    /**
     * Modifies the parameter p to its appropriate value. Checks:
     *
     * <ul>
     *  <li> the return type cannot be primitive and marked with ReadOnly
     *  <li> whether the method overrides a method illegally
     *  <li> whether the receiver type is invalid (Mutable inside ReadOnly)
     * </ul>
     *
     * @see com.sun.source.util.TreeScanner#visitMethod(com.sun.source.tree.MethodTree,java.lang.Object)
     */
    @Override
    public Void visitMethod(MethodTree node, VisitorState p) {

        AnnotatedMethodType method = factory.getMethod(node);

        AnnotatedClassType receiverType = method.getAnnotatedReceiverType();

        if (receiverType.hasAnnotationAt(RoMaybe.class,
                                         AnnotationLocation.RAW))
            p.state = State.RO_MAYBE;
        else if (receiverType.hasAnnotationAt(ReadOnly.class,
                                              AnnotationLocation.RAW))
            p.state = State.READONLY;
        else if (receiverType.hasAnnotationAt(Mutable.class,
                                              AnnotationLocation.RAW))
            p.state = State.MUTABLE;
        else if (p.classState == State.MUTABLE)
            p.state = State.MUTABLE;
        else if (p.classState == State.READONLY)
            p.state = State.READONLY;
        else if (p.classState == State.RO_MAYBE)
            p.state = State.RO_MAYBE;

        p.annotatedClassType = receiverType;

        // check if receiver is invalid
        if ((receiverType.hasAnnotationAt(Mutable.class,
                                          AnnotationLocation.RAW)
             || receiverType.hasAnnotationAt(RoMaybe.class,
                                             AnnotationLocation.RAW))
            && p.classState == State.READONLY)
            checker.report(Result.failure("receiver.invalid"), node);

        // check if constructor
        {
        String methodName = node.getName().toString();
        p.constructor = methodName.equals("<init>")
            || methodName.equals("<cinit>");
        }

        // check if overriding is legal
        JavariAnnotatedTypeFactory jfactory
            = (JavariAnnotatedTypeFactory) factory;
        ExecutableElement oMethodElt
            = jfactory.getOverridenMethod(method.getElement());

        if (oMethodElt != null) {

            AnnotatedMethodType oMethod = factory.getMethod(oMethodElt);

            // check parameters
            {
                List<AnnotatedClassType> mParamTypes
                    = method.getAnnotatedParameterTypes(),
                    omParamTypes = oMethod.getAnnotatedParameterTypes();
                for (int i = 0; i < mParamTypes.size(); i++) {
                    if (!checker.isSubtype(omParamTypes.get(i),
                                           mParamTypes.get(i)))
                        checker.report(Result
                                       .failure("override.param.invalid"),
                                       mParamTypes.get(i).getElement());
                }
            }
            // check throw types
            {
                List<AnnotatedClassType> mThrowsTypes
                    = method.getAnnotatedThrowsTypes(),
                    omThrowsTypes = oMethod.getAnnotatedThrowsTypes();
                for (int i = 0; i < mThrowsTypes.size(); i++) {
                    if (!checker.isSubtype(mThrowsTypes.get(i),
                                           omThrowsTypes.get(i)))
                        checker.report(Result
                                       .failure("override.throws.invalid"),
                                       mThrowsTypes.get(i).getElement());
                }
            }
            // check receiver
            {
                AnnotatedClassType mReceiverType
                    = method.getAnnotatedReceiverType(),
                    omReceiverType = oMethod.getAnnotatedReceiverType();
                if (!checker.isSubtype(omReceiverType, mReceiverType))
                    checker.report(Result
                                   .failure("override.receiver.invalid"),
                                   mReceiverType.getElement());
            }
            // check return
            {
                AnnotatedClassType mReturnType
                    = method.getAnnotatedReturnType(),
                    omReturnType = oMethod.getAnnotatedReturnType();
                if (!checker.isSubtype(mReturnType, omReturnType))
                    checker.report(Result
                                   .failure("override.return.invalid"),
                                   mReturnType.getElement());

            }

        }

        AnnotatedClassType returnType = method.getAnnotatedReturnType();
        if (isPrimitiveReadOnly(returnType))
            checker.report(Result.failure("primitive.ro"),
                           node.getReturnType());

        return super.visitMethod(node, p);

    }

    /**
     * Checks whether the assignment is possible:
     *
     * <ul>
     *  <li> ReadOnly references may not be assigned to Mutable variables
     *  <li> ReadOnly fields are not assignable, unless marked with
     *  {@code @Assignable}
     *  <li> local fields are final in a READONLY context
     * </ul>
     *
     * @see com.sun.source.util.TreeScanner#visitAssignment(com.sun.source.tree.AssignmentTree,java.lang.Object)
     */
    @Override
    public Void visitAssignment(AssignmentTree node, VisitorState p) {

        commonAssignment(node.getExpression(), node.getVariable(), p, true);
        return super.visitAssignment(node, p);
    }

    /**
     * Same implementation of  visitAssignment, but for {@link CompoundAssignmentTree}.
     *
     * @see com.sun.source.util.TreeScanner#visitCompoundAssignment(com.sun.source.tree.CompoundAssignmentTree,java.lang.Object)
     */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node,
                                        VisitorState p) {
        commonAssignment(node.getExpression(), node.getVariable(), p, true);
        return super.visitCompoundAssignment(node, p);
    }


    private boolean localField(AnnotatedClassType t) {
        if (t.getElement() == null) return false;
        if (t.getElement().getKind() != ElementKind.FIELD) return false;
        if (t.getTree().getKind() == Tree.Kind.MEMBER_SELECT) {
            ExpressionTree et = ((MemberSelectTree)t.getTree()).getExpression();
            if (et.getKind() == Tree.Kind.IDENTIFIER) {
                Name name = ((IdentifierTree)et).getName();
                return name.toString().equals("this") || name.toString().equals("super")
                    || localField(factory.getClass(et));
            }
            return false;
        }
        return true;
    }

    // code for assignments
    private void commonAssignment(ExpressionTree expressionTree,
                                  Tree variableTree,
                                  VisitorState p, boolean checkRoField){

        AnnotatedClassType expression = factory.getClass(expressionTree);
        AnnotatedClassType variable = factory.getClass(variableTree);

        boolean variableLocalField = localField(variable),
            expressionLocalField = localField(expression);

        // must check if can be used as readonly
        if (checkRoField
            && (p.state == State.READONLY || p.state == State.RO_MAYBE)
            && !p.constructor) {

            if (!variable.hasAnnotationAt(Assignable.class,
                                          AnnotationLocation.RAW)
                && variableLocalField) {
                checker.report(Result.failure("ro.field"),
                               variableTree);
            }
        }

        if (variable.getElement() != null
            && !variable.getElement().asType().getKind().isPrimitive()) {
            if (variableLocalField && noAnnotations(variable)){
                if (p.state == State.MUTABLE)
                    variable.include(Mutable.class);
                if (p.state == State.RO_MAYBE)
                    variable.include(RoMaybe.class);
                if (p.state == State.READONLY)
                    variable.include(ReadOnly.class);
            }

            if (expressionLocalField && noAnnotations(expression)) {
                if (p.state == State.READONLY)
                    expression.include(ReadOnly.class);
                if (p.state == State.RO_MAYBE)
                    expression.include(RoMaybe.class);
                if (p.state == State.MUTABLE)
                    expression.include(Mutable.class);
            }

        }

        if (variableTree instanceof MemberSelectTree) {
            ExpressionTree et = ((MemberSelectTree)variableTree).getExpression();
            Element exprElt;
            if (et.getKind() == Tree.Kind.ARRAY_ACCESS)
                exprElt = InternalUtils.symbol(((ArrayAccessTree)et).getExpression());
            else
                exprElt = InternalUtils.symbol(et);

            if (!(variableLocalField && p.constructor)
                && exprElt.getEnclosingElement().getKind()
                != ElementKind.METHOD) {
                if (!variable.hasAnnotationAt(Assignable.class,
                                              AnnotationLocation.RAW)
                    && (variable.hasAnnotationAt(ReadOnly.class,
                                                 AnnotationLocation.RAW)
                        || variable.hasAnnotationAt(QReadOnly.class,
                                                    AnnotationLocation.RAW)
                        || variable.hasAnnotationAt(RoMaybe.class,
                                                    AnnotationLocation.RAW))) {

                    checker.report(Result.failure("ro.field"),
                               variableTree);
                }
            }
        }

        // do not check primitives
        if (variable.getElement() != null
            && variable.getElement().asType().getKind().isPrimitive())
            return;

        if (!checker.isSubtype(expression, variable)) {
            checker.report(Result.failure("assignment.invalid"),
                           expressionTree);
        }
    }

    // convenience method for checking for lack of annotations
    private boolean noAnnotations(AnnotatedClassType type) {
        return type.getAnnotationData(ReadOnly.class, true).isEmpty()
            && type.getAnnotationData(RoMaybe.class, true).isEmpty()
            && type.getAnnotationData(Mutable.class, true).isEmpty();
    }

    /**
     * Checks for prefix and posfix increments and decrements on final fields.
     *
     * @see com.sun.source.util.TreeScanner#visitUnary(com.sun.source.tree.UnaryTree,java.lang.Object)
     */
    public Void visitUnary(UnaryTree node, VisitorState p){
        Tree.Kind kind = node.getKind();
        if (p.state != State.MUTABLE && !p.constructor) {
            if (kind == Tree.Kind.POSTFIX_DECREMENT
                || kind == Tree.Kind.POSTFIX_INCREMENT
                || kind == Tree.Kind.PREFIX_DECREMENT
                || kind == Tree.Kind.PREFIX_INCREMENT) {

                AnnotatedClassType variable
                    = factory.getClass(node.getExpression());
                if (!variable.hasAnnotationAt(Assignable.class,
                                              AnnotationLocation.RAW)
                    && ElementFilter
                    .fieldsIn(InternalUtils.symbol(classMap.get(node))
                              .getEnclosedElements())
                    .contains(variable.getElement())) { // if local field
                    checker.report(Result.failure("ro.field"),
                                   node.getExpression());
                }
            }
        }
        return super.visitUnary(node, p);
    }

    /**
     * Verifies that the iterator element type can be assigned to the element.
     *
     * @see com.sun.source.util.TreeScanner#visitEnhancedForLoop(com.sun.source.Tree.EnhancedForLoopTree,java.lang.Object)
     */
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, VisitorState p) {

        AnnotatedClassType eltType = factory.getClass(node.getVariable());
        if (!eltType.hasAnnotationAt(ReadOnly.class, AnnotationLocation.RAW)
            && !eltType.hasAnnotationAt(RoMaybe.class, AnnotationLocation.RAW)
            && eltType.getElement() != null
            && !eltType.getElement().asType().getKind().isPrimitive())
            eltType.includeAt(Mutable.class, AnnotationLocation.RAW);

        AnnotatedClassType actualType = new AnnotatedClassType(checker.getProcessingEnvironment());
        Element exprElt = factory.getClass(node.getExpression()).getElement();

        TypeKind tk = null;
        if (exprElt != null) {
            if (exprElt.getKind() == ElementKind.METHOD) {
                ExecutableElement eElt = (ExecutableElement)exprElt;
                tk = eElt.getReturnType().getKind();
            } else {
                tk = exprElt.asType().getKind();
            }
        }

        if (tk == TypeKind.ARRAY) {
            AnnotatedClassType arrayType = factory.getClass(node.getExpression());
            AnnotationLocation newRoot = AnnotationLocation.fromArray(new int[] { 0 });
            for (AnnotationData ad : arrayType.getAnnotationData(true)) {
                AnnotationLocation loc = ad.getLocation();
                if (loc.isSubtreeOf(newRoot)) {
                    actualType.include(factory.asSubOf(ad, newRoot));
                }
            }

        } else {
            GenericsUtils g =
                new GenericsUtils(checker.getProcessingEnvironment(), factory);
            // must use factory to solve method invocations
            Element elt;
            if (node.getExpression().getKind() == Tree.Kind.NEW_CLASS)
                elt = InternalUtils.symbol(((NewClassTree)(node.getExpression())).getIdentifier());
            else
                elt = InternalUtils.symbol(node.getExpression());

            TypeMirror iterableType =  g.iteratedType(node);
            if (iterableType != null)
                for (AnnotationData ad : g.annotationsFor(iterableType, elt))
                    actualType.annotate(ad);
        }

        if (!checker.isSubtype(actualType, eltType))
            checker.report(Result.failure("assignment.invalid"), node.getVariable());

        return super.visitEnhancedForLoop(node, p);
    }

    /**
     * Emits a warning if a cast is made from readonly to mutable.
     *
     * @see com.sun.source.util.TreeScanner#visitTypeCast(com.sun.source.Tree.TypeCastTree,java.lang.Object)
     */
    public Void visitTypeCast(TypeCastTree node, VisitorState p) {

        AnnotatedClassType exprType = factory.getClass(node.getExpression());
        AnnotatedClassType castType = factory.getClass(node);

        if (!checker.isSubtype(exprType, castType)) {
            checker.report(Result.warning("mutable.cast"), node.getType());
        }

        return super.visitTypeCast(node, p);
    }


    /**
     * Checks if the variable is not a ReadOnly primitive, and if it
     * is initialized, whether the initial value is assignable.
     *
     * @see com.sun.source.util.TreeScanner#visitVariable(com.sun.source.tree.VariableTree,java.lang.Object)
     */
    @Override
    public Void visitVariable(VariableTree node, VisitorState p){
        AnnotatedClassType variable = factory.getClass(node);

        if (isPrimitiveReadOnly(variable))
            checker.report(Result.failure("primitive.ro"), node);

        ExpressionTree initializer = node.getInitializer();

        if (initializer == null)
            return super.visitVariable(node, p);

        commonAssignment(initializer, node, p, false);

        return super.visitVariable(node, p);
    }


    /**
     * Checks if a method invocation satisfies the following conditions:
     *
     *<ul>
     * <li> All arguments are assignable to its parameter types.
     * <li> A mutable method cannot be invoked from inside a method
     * that is not mutable.
     * <li> A mutable method cannot be invoked from a readonly reference.
     *</ul>
     *
     * @see com.sun.source.util.TreeScanner#visitMethodInvocation(com.sun.source.tree.MethodInvocationTree,java.lang.Object)
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node,
                                      VisitorState p) {

        AnnotatedMethodType method = factory.getMethod(node);

        // need to ask factory to resolve RoMaybes
        AnnotatedClassType returnType = factory.getClass(node);

        ExecutableElement methodElt = method.getElement();
        AnnotatedClassType receiver = method.getAnnotatedReceiverType();

        AnnotatedClassType instanceType;
        if (node.getMethodSelect() instanceof MemberSelectTree) {
            MemberSelectTree mst = (MemberSelectTree) (node.getMethodSelect());

            instanceType = factory.getClass(mst.getExpression());
            AnnotatedClassType receiverType
                = method.getAnnotatedReceiverType();

            for (AnnotationLocation a : receiverType.getAnnotatedLocations()) {
                if (receiverType.hasAnnotationAt(RoMaybe.class, a)) {
                    receiverType.excludeAt(RoMaybe.class, a);
                    if (a.equals(AnnotationLocation.RAW))
                        receiverType.includeAt(ReadOnly.class, a);
                    else receiverType.includeAt(QReadOnly.class, a);
                }
            }

            if (!checker.isSubtype(instanceType, receiverType)){
                checker.report(Result.failure("ro.reference"), node);
            }
        } else {
            instanceType = p.annotatedClassType;

            if (!receiver.hasAnnotationAt(RoMaybe.class,
                                          AnnotationLocation.RAW)
                && !checker.isSubtype(instanceType, receiver)
                && !(node.getMethodSelect().toString().equals("super")
                && node.getArguments().isEmpty())) {
                checker.report(Result.failure("ro.reference"), node);
            }
        }

        List<AnnotatedClassType> parameters = method
                .getAnnotatedParameterTypes();

        // check whether RoMaybe is limiting parameters
        if (receiver.hasAnnotationAt(RoMaybe.class, AnnotationLocation.RAW)) {
            boolean roMaybeReturn = false;

            AnnotatedClassType annotatedReturn = method.getAnnotatedReturnType();
            for (AnnotationLocation a : annotatedReturn.getAnnotatedLocations()) {
                if (annotatedReturn.hasAnnotationAt(RoMaybe.class, a)) {
                    roMaybeReturn = true;
                    break;
                }
            }

            if (method.getElement().getKind() == ElementKind.CONSTRUCTOR)
                roMaybeReturn = true;

            if (!roMaybeReturn) {
                boolean isRAWMutable
                    = returnType.hasAnnotationAt(Mutable.class, AnnotationLocation.RAW);

                for (AnnotatedClassType pType : parameters) {
                    for (AnnotationLocation a : pType.getAnnotatedLocations()) {
                        if (pType.hasAnnotationAt(RoMaybe.class, a)) {
                            pType.excludeAt(RoMaybe.class, a);
                            if (isRAWMutable) {
                                pType.includeAt(Mutable.class, a);
                            } else {
                                if (a.equals(AnnotationLocation.RAW))
                                    pType.includeAt(ReadOnly.class, a);
                                else
                                    pType.includeAt(QReadOnly.class, a);
                            }
                        }
                    }
                }
            }
        }


        for (int i = 0; i < java.lang.Math.min(parameters.size(), node.getArguments().size()); i++) {
            AnnotatedClassType pType = factory.getClass(node.getArguments()
                                                        .get(i));
            if (pType.getAnnotatedLocations().isEmpty())
                pType.includeAt(Mutable.class, AnnotationLocation.RAW);

            if (pType.getElement() != null
                && !pType.getElement().asType().getKind().isPrimitive()) {

                if (!checker.isSubtype(pType, parameters.get(i)))
                    checker.report(Result.failure("argument.invalid"),
                                   node.getArguments().get(i));

                if (parameters.get(i).getAnnotatedLocations().size() >
                    pType.getAnnotatedLocations().size())
                    checker.report(Result.warning("supertype.loss"),
                                   node.getArguments().get(i));

            }
        }

        return super.visitMethodInvocation(node, p);
    }


    /**
     * Checks if a constructor invocation satisfies the following conditions:
     *
     *<ul>
     * <li> All arguments are assignable to its parameter types.
     *</ul>
     *
     * @see com.sun.source.util.TreeScanner#visitNewClass(com.sun.source.tree.NewClassTree,java.lang.Object)
     */
    @Override
    public Void visitNewClass(NewClassTree node, VisitorState p) {
        ExecutableElement constructorElt = InternalUtils.constructor(node);
        AnnotatedMethodType constructor = factory.getMethod(constructorElt);

        List<AnnotatedClassType> parameters = constructor
            .getAnnotatedParameterTypes();

        for (int i = 0; i < parameters.size(); i++) {
            if (!checker.isSubtypeIgnoringTypeParameters(factory.getClass(node.getArguments().get(i)),
                                                         parameters.get(i))) {
                checker.report(Result.failure("argument.invalid"),
                               node.getArguments().get(i));
            }
        }

        return super.visitNewClass(node, p);
    }


    /**
     * Checks whether the return type of a returned variable is
     * assignable to its declared return type.
     *
     * @see com.sun.source.util.TreeScanner#visitReturn(com.sun.source.tree.ReturnTree,java.lang.Object)
     */
    @Override
    public Void visitReturn(ReturnTree node, VisitorState p) {

        if (node.getExpression() == null)
            return super.visitReturn(node, p);

        AnnotatedClassType ret = factory.getClass(node.getExpression());
        MethodTree enclosingMethod
            = (MethodTree) InternalUtils.enclosingMethod(TreePath.getPath(root, node));

        AnnotatedMethodType meth = factory.getMethod(enclosingMethod);
        AnnotatedClassType retType = meth.getAnnotatedReturnType();
        if (retType.getAnnotatedLocations().isEmpty())
            retType.includeAt(Mutable.class, AnnotationLocation.RAW);

        if (enclosingMethod.getReturnType().getKind() != Tree.Kind.PRIMITIVE_TYPE) {
            if (!checker.isSubtype(ret, retType))
                checker.report(Result.failure("return.invalid"), node);

            if (ret.getAnnotatedLocations().size() >
                retType.getAnnotatedLocations().size())
                checker.report(Result.warning("supertype.loss"), node);
        }

        return super.visitReturn(node, p);
    }

    /**
     * Checks whether an AnnotatedClassType refers to a primitive type
     * marked as {@code @ReadOnly} (which is illegal).
     *
     * @param type the (@link AnnotatedClassType} to check
     * @return true if the type refers to a primitive and marked as
     * {@code @ReadOnly}.
     */
    protected boolean isPrimitiveReadOnly(AnnotatedClassType type) {
        TypeMirror tm = type.getElement().asType();
        AnnotationLocation a = AnnotationLocation.RAW;
        List<Integer> list = new ArrayList<Integer>();
        list.add(0);
        int i = 0;

        while (tm.getKind() == TypeKind.ARRAY) {
            tm = ((ArrayType)tm).getComponentType();
            List<Integer> l = a.asList();
            if (!l.isEmpty()) {
                list.remove(i);
                list.add(++i);
            }
            a = AnnotationLocation.fromList(list);
        }

        if (tm.getKind().isPrimitive())
            return type.hasAnnotationAt(ReadOnly.class, a);

        return false;
    }

}
