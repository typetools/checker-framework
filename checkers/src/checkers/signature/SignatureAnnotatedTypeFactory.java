package checkers.signature;

import com.sun.source.tree.CompilationUnitTree;

import checkers.types.BasicAnnotatedTypeFactory;


// This code is copied from SignatureAnnotatedTypeFactory.
// The two could be generalized and combined, perhaps.
// TODO: the above comment is self-referential. What should it mean?

// TODO: Does not yet handle method signature annotations, such as
// @MethodDescriptor.


/**
 * This class is currently not needed, as recent refactorings made the String pattern matching
 * obsolete. I left it in to make future extension easier.
 */
public class SignatureAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<SignatureChecker> {

    public SignatureAnnotatedTypeFactory(SignatureChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    /*
    @Override
    public TreeAnnotator createTreeAnnotator(SignatureChecker checker) {
        return new SignatureTreeAnnotator(checker);
    }

    private class SignatureTreeAnnotator extends TreeAnnotator {

        public SignatureTreeAnnotator(BaseTypeChecker checker) {
            super(checker, SignatureAnnotatedTypeFactory.this);
        }

    }
    */
}
