package checkers.nonnull;

import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;

import com.sun.source.tree.*;

/**
 * A type-checking visitor for the {@link checkers.nullness.quals.NonNull} type
 * qualifier that uses the {@link BaseTypeVisitor} implementation. This visitor
 * reports errors or warnings for violations for the following cases:
 *
 * <ul>
 * <li>if the receiver of a member dereference is not NonNull (error
 * "dereference.of.nullable")</li>
 * </ul>
 */
public class NonNullVisitor extends BaseTypeVisitor<Void, Void> {

    private final AnnotationMirror NONNULL;

    public NonNullVisitor(BaseTypeChecker checker, CompilationUnitTree root) {
        super(checker, root);
        NONNULL = this.annoFactory.fromName("checkers.nullness.quals.NonNull");
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {

        // Skip checking package declarations.
        if (node == root.getPackageName())
            return super.visitMemberSelect(node, p);
        
        AnnotatedTypeMirror type = factory.getAnnotatedType(node.getExpression());

        if (!type.hasAnnotation(NONNULL))
            checker.report(Result.failure("dereference.of.nullable",
                        node.getExpression().toString()), node);

        return super.visitMemberSelect(node, p);
    }

}
