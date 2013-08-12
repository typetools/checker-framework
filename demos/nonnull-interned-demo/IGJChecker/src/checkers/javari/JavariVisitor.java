package checkers.javari;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;

import checkers.source.*;
import checkers.basetype.*;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;

import com.sun.source.tree.*;

/**
 * A type-checking visitor for the Javari mutability annotations
 * ({@code @ReadOnly}, {@code @Mutable} and {@code @Assignable}) that
 * extends BaseTypeVisitor.
 *
 * @see BaseTypeVisitor
 */
public class JavariVisitor extends BaseTypeVisitor<Void, Void> {

    final private AnnotationMirror MUTABLE, THISMUTABLE, READONLY,
        ROMAYBE, QREADONLY, ASSIGNABLE;

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
        READONLY = checker.READONLY;
        THISMUTABLE = checker.THISMUTABLE;
        MUTABLE = checker.MUTABLE;
        ROMAYBE = checker.ROMAYBE;
        QREADONLY = checker.QREADONLY;
        ASSIGNABLE = checker.ASSIGNABLE;
        assert factory instanceof JavariAnnotatedTypeFactory : factory;
    }

    /**
     * Allows a variable marked as ReadOnly and RoMaybe to be returned on a RoMaybe method.
     *
     * @see com.sun.source.util.TreeScanner#visitReturn(com.sun.source.tree.ReturnTree,java.lang.Object)
     */
    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        // Don't try to check return expressions for void methods.
        if (node.getExpression() == null)
            return super.visitReturn(node, p);

        AnnotatedTypeMirror returnValue = factory.getAnnotatedType(node.getExpression());

        if (returnValue.hasAnnotation(ROMAYBE))
            return null;

        return super.visitReturn(node, p);
    }

    /**
     * Overrides its super method, calling the assignment check unless the error key is
     * generic.argument.invalid and the upper bound is java.lang.Object .
     */
    @Override
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, String errorKey, Void p) {
    	if (errorKey.equals("generic.argument.invalid")
    		&& varType.getUnderlyingType().toString().equals("java.lang.Object"))
    		return;

    	super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, p);
    }

    /**
     * Overrides its super method, and does not invoke it, since currently BaseTypeVisitor does not
     * have a signature for this method that accepts AnnotatedTypeMirror parameters for both variable
     * and parent.
     *
     * On its current implementation, this method is responsible for assigning implicit annotations
     * to this-mutable variables, depending on receiver type, and to check that fields of ReadOnly
     * objects are not reassigned.
     */
    @Override
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueTree, String errorKey, Void p) {

        AnnotatedTypeMirror variable = factory.getAnnotatedType(varTree),
            value = factory.getAnnotatedType(valueTree);

        boolean variableLocalField, valueLocalField;

        // we want to determine if the variable is a field of the local class,
        // so if the current class has a field f,
        // a.f is not a local field, while this.f and f are local fields.
        if (varTree.getKind() == Tree.Kind.MEMBER_SELECT) {
            IdentifierTree varExprTree = (IdentifierTree) ((MemberSelectTree)varTree).getExpression();
            variableLocalField = factory.elementFromUse(varExprTree).getKind().isField();
        } else {
        	if (variable == null || variable.getElement() == null) variableLocalField = false;
        	else variableLocalField = variable.getElement().getKind().isField();
        }

        AnnotatedTypeMirror valueType = factory.getAnnotatedType(valueTree);
        if (valueTree.getKind() == Tree.Kind.MEMBER_SELECT) {
            IdentifierTree valueExprTree = (IdentifierTree) ((MemberSelectTree)valueTree).getExpression();
            valueLocalField = factory.elementFromUse(valueExprTree).getKind().isField();
        } else {
            if (valueType.getElement() == null) valueLocalField = false;
            else valueLocalField = valueType.getElement().getKind().isField();
        }

        // variable must be marked same as state
        AnnotatedDeclaredType mReceiver = factory.getSelfType(varTree);

        if (mReceiver != null) {
            if (variableLocalField) {
                if (!(variable.hasAnnotation(MUTABLE) || variable.hasAnnotation(ROMAYBE) || variable.hasAnnotation(READONLY))) {
                    if (mReceiver.hasAnnotation(READONLY))
                        variable.addAnnotation(READONLY);
                    else if (mReceiver.hasAnnotation(ROMAYBE))
                        variable.addAnnotation(ROMAYBE);
                    else
                        variable.addAnnotation(MUTABLE);
                }
            }
            if (valueLocalField) {
                if (!(value.hasAnnotation(MUTABLE) || value.hasAnnotation(ROMAYBE) || value.hasAnnotation(READONLY))) {
                    if (mReceiver.hasAnnotation(READONLY))
                        value.addAnnotation(READONLY);
                    else if (mReceiver.hasAnnotation(ROMAYBE))
                        value.addAnnotation(ROMAYBE);
                    else
                        value.addAnnotation(MUTABLE);
                }
            }
        }

    	boolean inConstructor = inConstructor();

    	if (mReceiver != null)
            if ((mReceiver.hasAnnotation(READONLY) || mReceiver.hasAnnotation(ROMAYBE)) && !inConstructor)
                if (!variable.hasAnnotation(ASSIGNABLE) && variableLocalField)
                    checker.report(Result.failure("ro.field"), varTree);

        if (varTree.getKind() == Tree.Kind.MEMBER_SELECT) {
            ExpressionTree et = ((MemberSelectTree)varTree).getExpression();
            AnnotatedTypeMirror outerType = factory.getAnnotatedType(et);
            Element exprElt = outerType.getElement();

            if (!(variableLocalField && inConstructor)
                && exprElt.getEnclosingElement().getKind()
                != ElementKind.METHOD) {
                if (!variable.hasAnnotation(ASSIGNABLE)
                    && (outerType.hasAnnotation(READONLY)
                        || outerType.hasAnnotation(QREADONLY)
                        || outerType.hasAnnotation(ROMAYBE)))
                    checker.report(Result.failure("ro.field"),varTree);

            }
        }

        // do not check primitives
        if (variable.getKind().isPrimitive())
            return;

        boolean success = checker.isSubtype(variable, value);

        //String useKey = (errorKey == null ? "type.incompatible" : errorKey);
        String useKey = "type.incompatible";
        if (!success) {
            checker.report(Result.failure(useKey,
                value.toString(), variable.toString()), valueTree);
        }

    }

    /**
     * Checks for prefix and postfix increments and decrements on final fields.
     *
     * @see com.sun.source.util.TreeScanner#visitUnary(com.sun.source.tree.UnaryTree,java.lang.Object)
     */
    @Override
    public Void visitUnary(UnaryTree node, Void p){
        Tree.Kind kind = node.getKind();
        if (factory.getSelfType(node).hasAnnotation(MUTABLE) && !inConstructor()) {
            if (kind == Tree.Kind.POSTFIX_DECREMENT
                || kind == Tree.Kind.POSTFIX_INCREMENT
                || kind == Tree.Kind.PREFIX_DECREMENT
                || kind == Tree.Kind.PREFIX_INCREMENT) {

                AnnotatedTypeMirror variable
                    = factory.getAnnotatedType(node.getExpression());
                if (!variable.hasAnnotation(ASSIGNABLE)
                    && ElementFilter
                    .fieldsIn(factory.getSelfType(node).getElement().getEnclosedElements())
                    .contains(variable.getElement())) { // if local field
                    checker.report(Result.failure("ro.field"),
                                   node.getExpression());
                }
            }
        }
        return super.visitUnary(node, p);
    }


    /**
     * Tests whether the tree expressed by the passed type tree is a valid type,
     * and emits an error if that is not the case (e.g. '@Mutable String').
     *
     * @param tree  the AST type supplied by the user
     */
    @Override
    public void validateTypeOf(Tree tree) {
        Tree tTree;
        if (tree.getKind() == Tree.Kind.VARIABLE) {
            tTree = ((VariableTree)tree).getType();
            AnnotatedTypeMirror vType = factory.getAnnotatedType(tree);
            if ((vType.hasAnnotation(QREADONLY) || vType.hasAnnotation(READONLY) || vType.hasAnnotation(ROMAYBE))
                && tTree.getKind() == Tree.Kind.PRIMITIVE_TYPE){
                checker.report(Result.failure("primitive.ro"), tree);
            }
        }
        else
            tTree = tree;

        switch (tTree.getKind()) {
        case ANNOTATED_TYPE: {
            AnnotatedTypeMirror vType = factory.getAnnotatedType(tree);
            Tree nextTree = ((AnnotatedTypeTree)tTree).getUnderlyingType();
            if (nextTree.getKind() == Tree.Kind.PRIMITIVE_TYPE) {
                if (vType.hasAnnotation(QREADONLY) || vType.hasAnnotation(READONLY) || vType.hasAnnotation(ROMAYBE)) {
                    checker.report(Result.failure("primitive.ro"), tTree);
                }
            } else {
                validateTypeOf(nextTree);
            }
            break;
        }
        case ARRAY_TYPE: {
            validateTypeOf(((ArrayTypeTree)tTree).getType());
            break;
        }
        }
        super.validateTypeOf(tree);
    }


    /**
     * Emits a warning if a cast is made increasing mutability.
     *
     * @see com.sun.source.util.TreeScanner#visitTypeCast(com.sun.source.tree.TypeCastTree,java.lang.Object)
     */
    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {

        AnnotatedTypeMirror exprType = factory.getAnnotatedType(node.getExpression()),
            castType = factory.getAnnotatedType(node);

        if (!checker.isSubtype(castType, exprType)) {
            checker.report(Result.warning("mutable.cast"), node.getType());
        }

        return super.visitTypeCast(node, p);
    }

    /**
     * Overriding invocability check to let {code @ReadOnly} references invoke {code @RoMaybe} methods.
     */
    @Override
    protected boolean checkMethodInvocability(AnnotatedExecutableType method, MethodInvocationTree node) {
        AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
        AnnotatedTypeMirror treeReceiver;

        if (methodReceiver.hasAnnotation(ROMAYBE)) methodReceiver.addAnnotation(READONLY);

        if (node.getMethodSelect().getKind() == Tree.Kind.MEMBER_SELECT) {
            MemberSelectTree mst = (MemberSelectTree) (node.getMethodSelect());
            treeReceiver = factory.getAnnotatedType(mst.getExpression());
        } else
            treeReceiver = factory.getSelfType(node);


        if (!checker.isSubtype(methodReceiver, treeReceiver)) {
            checker.report(Result.failure("ro.reference",
                factory.elementFromUse(node),
                treeReceiver.toString(), methodReceiver.toString()), node);
            return false;
        }
        return true;
    }


    /**
     * Overriding assignment check to annotate every {code @RoMaybe} parameter with {code @ReadOnly}.
     */
    @Override
    protected void checkArguments(List<? extends AnnotatedTypeMirror> requiredArgs,
            List<? extends ExpressionTree> passedArgs, Void p) {

        for (int i = 0; i < requiredArgs.size(); ++i) {
        	AnnotatedTypeMirror pType = requiredArgs.get(i);
        	if (pType.hasAnnotation(ROMAYBE) && !pType.hasAnnotation(MUTABLE))
        		pType.addAnnotation(READONLY);
        	else if (!pType.hasAnnotation(READONLY))
        		pType.addAnnotation(MUTABLE);

        	commonAssignmentCheck(pType,
                    passedArgs.get(i),
                    "argument.invalid", p);
        }
    }


    private boolean inConstructor() {
    	MethodTree mt = visitorState.getMethodTree();
    	if (mt == null) return true;
    	ExecutableElement mElt = factory.elementFromDeclaration(mt);
    	return mElt.getKind() == ElementKind.CONSTRUCTOR;
    }

}
