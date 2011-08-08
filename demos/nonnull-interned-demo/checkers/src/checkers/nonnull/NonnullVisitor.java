package checkers.nonnull;

import checkers.source.*;
import checkers.subtype.*;
import checkers.quals.*;
import checkers.types.*;

import com.sun.source.tree.*;

import javax.lang.model.element.*;

/**
 * A type-checking visitor for the {@code @NonNull} type qualifier
 * that uses the {@link SubtypeVisitor} implementation.
 *
 * @see SubtypeVisitor
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class NonnullVisitor extends SubtypeVisitor {

    /**
     * Creates a new visitor for type-checking {@code @NonNull}.
     *
     * @param checker the {@link SubtypeChecker} to use
     * @param root the root of the input program's AST to check
     */
    public NonnullVisitor(SubtypeChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public @Nullable Void visitUnary(UnaryTree node, Void p) {

        if (!(node.getKind() == Tree.Kind.POSTFIX_DECREMENT ||
                node.getKind() == Tree.Kind.POSTFIX_INCREMENT ||
                node.getKind() == Tree.Kind.PREFIX_DECREMENT ||
                node.getKind() == Tree.Kind.PREFIX_INCREMENT))
            return super.visitUnary(node, p);

        AnnotatedClassType expression = factory.getClass(node.getExpression());

        if (!expression.hasAnnotationAt(NonNull.class, AnnotationLocation.RAW))
            checker.report(Result.failure("unary.invalid"), node.getExpression());

        return super.visitUnary(node, p);
    }

    @Override
    public @Nullable Void visitMemberSelect(MemberSelectTree node, Void p) {

        AnnotatedClassType receiver = factory.getClass(node.getExpression());
        Element elt = receiver.getElement();
        @Nullable Element field = InternalUtils.symbol(node);

        if (elt instanceof PackageElement)
            return super.visitMemberSelect(node, p);

        if (field instanceof ExecutableElement) 
            return super.visitMemberSelect(node, p);

        if (field != null && field.getModifiers().contains(Modifier.STATIC))
            return super.visitMemberSelect(node, p);

        String className = InternalUtils.getQualifiedName(elt);
        if (!checker.shouldSkip(className)&&
                !receiver.hasAnnotationAt(NonNull.class, AnnotationLocation.RAW))
            checker.report(Result.failure("deref.invalid", receiver), node);

        return super.visitMemberSelect(node, p);
    }
}
