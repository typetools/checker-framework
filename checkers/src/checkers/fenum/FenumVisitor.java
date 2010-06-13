package checkers.fenum;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

public class FenumVisitor extends BaseTypeVisitor<Void, Void> {
	public FenumVisitor(FenumChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }
	
	/**
	 * The type validator to ensure correct usage of modifiers.
	 */
    @Override
    protected TypeValidator createTypeValidator() {
    	return new FenumTypeValidator();
    }
  
	private final class FenumTypeValidator extends TypeValidator {
		@Override
		public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
			checkOneAnnotation(type, p);
			return super.visitDeclared(type, p);
		}

		@Override
		public Void visitPrimitive(AnnotatedPrimitiveType type, Tree p) {
			checkOneAnnotation(type, p);
			return super.visitPrimitive(type, p);
		}

		private void checkOneAnnotation(AnnotatedTypeMirror type, Tree p) {
			int count = 0;
			if (type.hasAnnotation(FenumChecker.FENUM))
				++count;
			if (type.hasAnnotation(FenumChecker.FENUM_DECL))
				++count;
			if (type.hasAnnotation(FenumChecker.FENUM_UNQUAL))
				++count;

			if (count > 1) {
				reportError(type, p);
			}
		}
	}
}
