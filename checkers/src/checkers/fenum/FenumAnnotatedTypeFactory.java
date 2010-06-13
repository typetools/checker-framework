package checkers.fenum;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.CompilationUnitTree;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;


public class FenumAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<FenumChecker> {
	
    public FenumAnnotatedTypeFactory(FenumChecker checker,
            CompilationUnitTree root) {
    	// disable flow checker, as it seems to ignore the results of postAsMemberOf
        super(checker, root, false);
    }
    
    /**
     * Adapt the type of field accesses.
     */
    @Override
    protected void postAsMemberOf(AnnotatedTypeMirror type,
            AnnotatedTypeMirror owner, Element element) {
		assert type != null;
		assert owner != null;
		assert element != null;
		
		if (type.getKind() == TypeKind.EXECUTABLE
				|| type.getKind() == TypeKind.ARRAY) {
			// nothing to do
			return;
		}
		if (element.getKind() == ElementKind.LOCAL_VARIABLE) {
			// the type of local variables also does not need to change
			return;
		}

		if (type.hasAnnotation(FenumChecker.FENUM_DECL)) {
			Set<AnnotationMirror> all = type.getAnnotations();
			
			Map<? extends ExecutableElement, ? extends AnnotationValue> map = null;
			for( AnnotationMirror one : all ) {
				map = one.getElementValues();
				// TODO: there should be only one, maybe check
			}
			if (map==null) {
				map = Collections.emptyMap();
			}
			final Map<? extends ExecutableElement, ? extends AnnotationValue> oldmap = map;
			
			type.removeAnnotation(FenumChecker.FENUM_DECL);
			
			// can we reuse class com.sun.tools.javac.code.Attribute$Compound ?
			
			AnnotationMirror newannot = new AnnotationMirror() {
				@Override
				public DeclaredType getAnnotationType() {
					return FenumChecker.FENUM.getAnnotationType();
				}

				@Override
				public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
					return oldmap;
				}

				@Override
				public String toString() {
					StringBuilder res = new StringBuilder();
					res.append(FenumChecker.FENUM.toString());
					res.append("(");
					// this works for the single value that we have currently
					res.append(oldmap.values().toArray()[0]);
					res.append(")");
					
					/*
					 * The more general solution, in case we need to add other attributes.
					int size = oldmap.size();
					if (size > 0) {
						res.append("(");
						boolean nonfirst = false;
						for( Entry<? extends ExecutableElement, ? extends AnnotationValue> e : oldmap.entrySet() ) {
							
							if(nonfirst) {
								res.append(", ");
							} else {
								nonfirst = true;
							}
							if (size > 1 ||
								!e.getKey().toString().equals("value()") ) {
								res.append(e.getKey());
								res.append("=");
							}
							res.append(e.getValue());
						}
						res.append(")");
					}
					*/
					
					// System.out.println("result: " + res);
					return res.toString();
				}
			};

			type.addAnnotation(newannot);
		}
		
    	// System.out.println("out type: " + type);
    }
}