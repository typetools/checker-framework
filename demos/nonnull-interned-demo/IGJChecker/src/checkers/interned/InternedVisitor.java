package checkers.interned;

import checkers.source.*;
import checkers.basetype.*;
import checkers.types.*;
import checkers.util.*;
import com.sun.source.tree.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

/**
 * A type-checking visitor for the {@link checkers.quals.Interned} type
 * qualifier that uses the {@link BaseTypeVisitor} implementation. This visitor
 * reports errors or warnings for violations for the following cases:
 *
 * <ul>
 *  <li>if both sides of a "==" or "!=" comparison are not Interned (error
 *  "not.interned")</li>
 *  <li>if the receiver and argument for a call to an equals method are both
 *  Interned (optional warning "unnecessary.equals")</li>
 * </ul>
 *
 * 
 * @see BaseTypeVisitor
 */
public class InternedVisitor extends BaseTypeVisitor<Void, Void> {

    /** The interned annotation. */
    private final AnnotationMirror INTERNED;
    
    /**
     * Creates a new visitor for type-checking {@link checkers.quals.Interned}.
     *
     * @param checker the {@link BaseTypeChecker} to use
     * @param root the root of the input program's AST to check
     */
    public InternedVisitor(BaseTypeChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.INTERNED = this.annoFactory.fromName("checkers.quals.Interned");
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {

        if (!(node.getKind() == Tree.Kind.EQUAL_TO || 
                node.getKind() == Tree.Kind.NOT_EQUAL_TO))
            return super.visitBinary(node, p);

        Tree leftOp = node.getLeftOperand(), rightOp = node.getRightOperand();

        if (leftOp.getKind() == Tree.Kind.NULL_LITERAL ||
                rightOp.getKind() == Tree.Kind.NULL_LITERAL)
            return super.visitBinary(node, p);

        AnnotatedTypeMirror left = factory.getAnnotatedType(leftOp);
        AnnotatedTypeMirror right = factory.getAnnotatedType(rightOp);
        
        // Check passes due to auto-unboxing.
        if (left.getKind().isPrimitive() || right.getKind().isPrimitive())
            return super.visitBinary(node, p);

        if (!left.hasAnnotation(INTERNED))
            checker.report(Result.failure("not.interned", left), leftOp);
        if (!right.hasAnnotation(INTERNED))
            checker.report(Result.failure("not.interned", right), rightOp);

        return super.visitBinary(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        Element elt = InternalUtils.symbol(node);
        assert elt != null && elt instanceof ExecutableElement;
        ExecutableElement method = (ExecutableElement) elt;
        
        if (method.getSimpleName().contentEquals("equals") &&
                method.getReturnType().getKind() == TypeKind.BOOLEAN &&
                method.getParameters().size() == 1) {

            AnnotatedTypeMirror recv = factory.getReceiver(node);
            AnnotatedTypeMirror comp = factory.getAnnotatedType(node.getArguments().get(0));

            if (this.checker.getLintOption("dotequals", true)
                    && recv.hasAnnotation(INTERNED)
                    && comp.hasAnnotation(INTERNED))
                checker.report(Result.warning("unnecessary.equals"), node);
        }

        return super.visitMethodInvocation(node, p);
    }
}
