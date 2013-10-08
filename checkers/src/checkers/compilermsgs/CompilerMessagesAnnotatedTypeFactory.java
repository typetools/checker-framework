package checkers.compilermsgs;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.propkey.PropertyKeyAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;

/**
 * A PropertyKeyATF that uses CompilerMessageKey to annotate the keys.
 *
 * @author wmdietl
 */
public class CompilerMessagesAnnotatedTypeFactory extends PropertyKeyAnnotatedTypeFactory {

    public CompilerMessagesAnnotatedTypeFactory(CompilerMessagesChecker checker) {
        super(checker);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new KeyLookupTreeAnnotator(this, CompilerMessageKey.class);
    }
}
