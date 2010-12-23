package checkers.fenum;

import com.sun.source.tree.CompilationUnitTree;

import checkers.types.BasicAnnotatedTypeFactory;


public class FenumAnnotatedTypeFactory extends
        BasicAnnotatedTypeFactory<FenumChecker> {

    public FenumAnnotatedTypeFactory(FenumChecker checker,
            CompilationUnitTree root) {
        // Disable flow checker, as it changes a field access that has the type
        // @Fenum("A") into an access @Fenum, ignoring the arguments!
        // This happens in BasicAnnotatedTypeFactory.annotateImplicit, where
        // all annotations are removed and the inferred annotation is added.
        // Inference apparently does not handle arguments yet.
        super(checker, root, false);
    }
}
