package org.checkerframework.framework.util;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Base class for viewpoint adaptation in framework side. Subclass should extend this class to perform type
 * system specific viewpoint adaptation, because FrameworkVPUtil doesn't perform real viewpoint adaptation
 * - it simply returns declared type
 * @author tamier
 *
 */
public class FrameworkVPUtil extends GenericVPUtil<AnnotationMirror> {

    @Override
    public AnnotationMirror getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f) {
        return atm.getAnnotations().iterator().hasNext()
                ? atm.getAnnotations().iterator().next()
                : null;
    }

    @Override
    public AnnotationMirror getAnnotationFromModifier(AnnotationMirror t) {
        return t;
    }

    @Override
    public AnnotationMirror combineModifierWithModifier(
            AnnotationMirror recvModifier, AnnotationMirror declModifier, AnnotatedTypeFactory f) {
        return declModifier;
    }
}
