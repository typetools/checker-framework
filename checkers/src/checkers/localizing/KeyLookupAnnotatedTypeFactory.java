package checkers.localizing;

import java.util.Set;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.localizing.quals.LocalizableKey;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;

public class KeyLookupAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<KeyLookupChecker> {

    private final Set<String> localizableKeys;

    public KeyLookupAnnotatedTypeFactory(KeyLookupChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
        this.localizableKeys = checker.getLocalizableKeys();
    }

    @Override
    public TreeAnnotator createTreeAnnotator(KeyLookupChecker checker) {
        return new KeyLookupTreeAnnotator(checker);
    }

    private class KeyLookupTreeAnnotator extends TreeAnnotator {

        public KeyLookupTreeAnnotator(BaseTypeChecker checker) {
            super(checker);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && tree.getKind() == Tree.Kind.STRING_LITERAL
                && localizableKeys.contains((String)tree.getValue()))
                type.addAnnotation(LocalizableKey.class);
            return super.visitLiteral(tree, type);
        }
    }
}
