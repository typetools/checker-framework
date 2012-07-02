package checkers.oigj;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

public class ImmutabilityVisitor extends BaseTypeVisitor<ImmutabilitySubchecker> {

    public ImmutabilityVisitor(ImmutabilitySubchecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        return true;
    }

    @Override
    public void validateTypeOf(Tree tree) {}
}
