package checkers.javari;


import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import checkers.basetype.BaseTypeVisitor;
import checkers.javari.quals.Assignable;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypes;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;


/**
 * A type-checking visitor for the Javari mutability annotations
 * ({@code @ReadOnly}, {@code @Mutable} and {@code @Assignable}) that
 * extends BaseTypeVisitor.
 *
 * @see BaseTypeVisitor
 */
public class JavariVisitor extends BaseTypeVisitor<JavariChecker> {

    final private AnnotationMirror READONLY, MUTABLE, POLYREAD, QREADONLY;

    /**
     * Creates a new visitor for type-checking the Javari mutability
     * annotations.
     *
     * @param checker the {@link JavariChecker} to use
     * @param root the root of the input program's AST to check
     */
    public JavariVisitor(JavariChecker checker, CompilationUnitTree root) {
        super(checker, root);
        READONLY = checker.READONLY;
        MUTABLE = checker.MUTABLE;
        POLYREAD = checker.POLYREAD;
        QREADONLY = checker.QREADONLY;
        checkForAnnotatedJdk();
    }

    /**
     * Ensures the class type is not {@code @PolyRead} outside a
     * {@code @PolyRead} context.
     */
    @Override
    public Void visitClass(ClassTree node, Void p) {
        if (atypeFactory.fromClass(node).hasEffectiveAnnotation(POLYREAD)
            && !atypeFactory.getSelfType(node).hasEffectiveAnnotation(POLYREAD))
            checker.report(Result.failure("polyread.type"), node);

        return super.visitClass(node, p);
    }

   /**
    * Checks whether the variable represented by the given type and
    * tree can be assigned, causing a checker error otherwise.
    */
    @Override
    protected void checkAssignability(AnnotatedTypeMirror varType, Tree varTree) {
        if (!(varTree instanceof ExpressionTree)) return;
        Element varElt = varType.getElement();
        if (varElt != null && atypeFactory.getDeclAnnotation(varElt, Assignable.class) != null)
            return;

        boolean variableLocalField =
            TreeUtils.isSelfAccess((ExpressionTree) varTree)
            && varElt != null
            && varElt.getKind().isField();
        // visitState.getMethodTree() is null when in static initializer block
        boolean inConstructor =  visitorState.getMethodTree() == null
            || TreeUtils.isConstructor(visitorState.getMethodTree());
        AnnotatedDeclaredType mReceiver = atypeFactory.getSelfType(varTree);

        if (variableLocalField && !inConstructor && !mReceiver.hasEffectiveAnnotation(MUTABLE))
            checker.report(Result.failure("ro.field"), varTree);

        if (varTree.getKind() == Tree.Kind.MEMBER_SELECT
            && !TreeUtils.isSelfAccess((ExpressionTree)varTree)) {
            AnnotatedTypeMirror receiver = atypeFactory.getReceiver((ExpressionTree)varTree);
            if (receiver != null && !receiver.hasEffectiveAnnotation(MUTABLE))
                checker.report(Result.failure("ro.field"), varTree);
        }

        if (varTree.getKind() == Tree.Kind.ARRAY_ACCESS) {
            AnnotatedTypeMirror receiver = atypeFactory.getReceiver((ExpressionTree)varTree);
            if (receiver != null && !receiver.hasEffectiveAnnotation(MUTABLE))
                checker.report(Result.failure("ro.element"), varTree);
        }

    }


    /**
     * Tests whether the tree expressed by the passed type tree
     * contains a qualified primitive type on its qualified type, and
     * if so emits an error.
     *
     * @param tree  the AST type supplied by the user
     */
    @Override
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
            type = atypeFactory.getAnnotatedTypeFromTypeTree(tree);
            break;
        default:
            type = atypeFactory.getAnnotatedType(tree);
        }

        // Here we simply test for primitive types
        // they can only occur at raw or array most inner component type
        type = AnnotatedTypes.innerMostType(type);
        if (type.getKind().isPrimitive()) {
            if (type.hasEffectiveAnnotation(QREADONLY)
                    || type.hasEffectiveAnnotation(READONLY)
                    || type.hasEffectiveAnnotation(POLYREAD)) {
                    checker.report(Result.failure("primitive.ro"), tree);
            }
        }
        super.validateTypeOf(tree);
    }
}
