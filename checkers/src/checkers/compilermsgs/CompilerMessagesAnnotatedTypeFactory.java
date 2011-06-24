package checkers.compilermsgs;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.propkey.PropertyKeyAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A PropertyKeyATF that uses CompilerMessageKey to annotate the keys.
 *
 * @author wmdietl
 */
public class CompilerMessagesAnnotatedTypeFactory extends
        PropertyKeyAnnotatedTypeFactory<CompilerMessagesChecker> {

    public CompilerMessagesAnnotatedTypeFactory(CompilerMessagesChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public TreeAnnotator createTreeAnnotator(CompilerMessagesChecker checker) {
        return new KeyLookupTreeAnnotator(checker, this, CompilerMessageKey.class);
    }
}
