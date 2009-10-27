package checkers.localizing;

import com.sun.source.tree.CompilationUnitTree;

import checkers.types.BasicAnnotatedTypeFactory;

/**
 * Template for type-introduction rules for the Localizing Checker
 *
 * @see LocalizingChecker
 */
public class LocalizingAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<LocalizingChecker> {

    public LocalizingAnnotatedTypeFactory(LocalizingChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }
}

