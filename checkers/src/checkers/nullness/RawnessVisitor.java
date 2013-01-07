package checkers.nullness;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

public class RawnessVisitor  extends BaseTypeVisitor<RawnessSubchecker> {
    public RawnessVisitor(RawnessSubchecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // Instead of this implementation, I would like to add
        // @ImplicitFor(trees={Tree.Kind.CLASS})
        // to the Raw annotation. But that creates some failures.
        return true;
    }
}