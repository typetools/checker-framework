package javacutils;

import java.lang.annotation.Annotation;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

public class BasicAnnotationProvider implements AnnotationProvider {

    @Override
    public AnnotationMirror getDeclAnnotation(Element elt,
            Class<? extends Annotation> anno) {
        String annoName = anno.getCanonicalName();
        List<? extends AnnotationMirror> annotationMirrors = elt
                .getAnnotationMirrors();

        // Then look at the real annotations.
        for (AnnotationMirror am : annotationMirrors) {
            if (AnnotationUtils.areSameByName(am, annoName)) {
                return am;
            }
        }

        return null;
    }
}
