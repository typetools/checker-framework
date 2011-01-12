package checkers.i18n;

import checkers.i18n.quals.LocalizableKey;
import checkers.propkey.PropertyKeyAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A PropertyKeyATF that uses LocalizableKey to annotate the keys.
 *
 * @author wmdietl
 */
public class LocalizableKeyAnnotatedTypeFactory extends
        PropertyKeyAnnotatedTypeFactory<LocalizableKeyChecker> {

    public LocalizableKeyAnnotatedTypeFactory(LocalizableKeyChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public TreeAnnotator createTreeAnnotator(LocalizableKeyChecker checker) {
        return new KeyLookupTreeAnnotator(checker, this, LocalizableKey.class);
    }
}
