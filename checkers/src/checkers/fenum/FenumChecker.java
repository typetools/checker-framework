package checkers.fenum;


import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.fenum.quals.Fenum;
import checkers.fenum.quals.FenumBottom;
import checkers.fenum.quals.FenumDecl;
import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.FenumUnqualified;
import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
// import checkers.source.SupportedLintOptions;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;


/**
 * The main checker class for the fake enum checker.
 * 
 * @author wmdietl
 */
@TypeQualifiers( { FenumDecl.class, Fenum.class, FenumUnqualified.class,
		FenumTop.class, FenumBottom.class } )
// @SupportedLintOptions({"allowLost", "checkOaM", "checkStrictPurity"})
public class FenumChecker extends BaseTypeChecker {
	
	protected static AnnotationMirror FENUM_DECL, FENUM_UNQUAL, FENUM;
	
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        FENUM_DECL = annoFactory.fromClass(FenumDecl.class);
        FENUM_UNQUAL = annoFactory.fromClass(FenumUnqualified.class);
        FENUM = annoFactory.fromClass(Fenum.class);
        
        super.init(env);
    }
    
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
		// The checker calls this method to compare the annotation used in a
		// type to the modifier it adds to the class declaration. As our default
		// modifier is Unqualified, this results in an error when a non-subtype
		// is used. Just ignore this check here and do them manually in the
		// visitor.
    	return true;
    }
}
