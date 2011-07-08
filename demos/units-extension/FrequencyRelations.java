import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotatedTypeMirror;
import checkers.util.AnnotationUtils;
import checkers.util.AnnotationUtils.AnnotationBuilder;

import checkers.units.*;

public class FrequencyRelations implements UnitsRelations {

    protected AnnotationMirror hz, s;
    
    public UnitsRelations init(AnnotationUtils annos, ProcessingEnvironment env) {
        AnnotationBuilder builder = new AnnotationBuilder(env, Hz.class);
        builder.setValue("value", checkers.units.quals.Prefix.one);
        hz = builder.build();

        builder = new AnnotationBuilder(env, checkers.units.quals.s.class);
        builder.setValue("value", checkers.units.quals.Prefix.one);
        s = builder.build();
        
        return this;
    }
            
    public AnnotationMirror multiplication(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
	return null;
    }

    public AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        if (p1.getAnnotations().isEmpty() && p2.getAnnotations().contains(s)) {
            return hz;
        }

        return null;
    }
    
}