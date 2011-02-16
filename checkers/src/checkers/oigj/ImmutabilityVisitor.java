package checkers.oigj;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;

public class ImmutabilityVisitor extends BaseTypeVisitor<Void, Void> {

    public ImmutabilityVisitor(ImmutabilitySubchecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public void validateTypeOf(Tree tree) {}
}
