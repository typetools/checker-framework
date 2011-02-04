package checkers.interned;

import checkers.quals.*;
import checkers.source.*;
import checkers.subtype.*;
import checkers.types.*;

import com.sun.source.tree.*;

import javax.lang.model.element.*;

/**
 * A type-checking visitor for the {@code @Interned} type qualifier that uses
 * the {@link SubtypeVisitor} implementation.
 *
 * @see SubtypeVisitor
 */
public class InternedVisitor extends SubtypeVisitor {

    protected final InternedAnnotatedTypeFactory iaf;
    
    /**
     * Creates a new visitor for type-checking {@code @Interned}.
     *
     * @param checker the {@link SubtypeChecker} to use
     * @param root the root of the input program's AST to check
     */
    public InternedVisitor(SubtypeChecker checker, CompilationUnitTree root) {
        super(checker, root);

        // TODO: improve this eventually -- for now it works but it's ugly
        this.iaf = (InternedAnnotatedTypeFactory)factory;
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {

        if (node.getKind() == Tree.Kind.EQUAL_TO || 
                node.getKind() == Tree.Kind.NOT_EQUAL_TO) {

            if (node.getLeftOperand().getKind() == Tree.Kind.NULL_LITERAL ||
                    node.getRightOperand().getKind() == Tree.Kind.NULL_LITERAL)
                return super.visitBinary(node, p);

            AnnotatedClassType left = factory.getClass(node.getLeftOperand());
            AnnotatedClassType right = factory.getClass(node.getRightOperand());

            if (!left.hasAnnotationAt(Interned.class, AnnotationLocation.RAW)
                    || !right.hasAnnotationAt(Interned.class, AnnotationLocation.RAW)) {
                checker.report(Result.failure("not.interned"), node);
            }
        }

        return super.visitBinary(node, p);
    }

    private void annotateIfInternedFinal(Element elt, AnnotatedClassType expr) {
        if (expr.hasAnnotationAt(Interned.class, AnnotationLocation.RAW)
                && elt != null 
                && elt.getModifiers().contains(Modifier.FINAL))
            iaf.annotateElement(elt, Interned.class, AnnotationLocation.RAW);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {

        AnnotatedClassType expression = factory.getClass(node.getExpression());
        AnnotatedClassType variable = factory.getClass(node.getVariable());

        annotateIfInternedFinal(variable.getElement(), expression);
        
        return super.visitAssignment(node, p);
    }
    
    @Override
    public Void visitVariable(VariableTree node, Void p) {

        if (node.getInitializer() == null)
            return super.visitVariable(node, p);

        AnnotatedClassType init = factory.getClass(node.getInitializer());
        AnnotatedClassType variable = factory.getClass(node);

        annotateIfInternedFinal(variable.getElement(), init);

        return super.visitVariable(node, p);
    }
}
