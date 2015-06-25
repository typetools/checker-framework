package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.qualframework.base.AnnotationConverter;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;

/**
 * Convert {@link org.checkerframework.checker.regex.qual.Regex}
 * annotations into a {@link Regex} qualifier.
 */
public class RegexAnnotationConverter implements AnnotationConverter<Regex> {

    private static final String regexName = org.checkerframework.checker.regex.qual.Regex.class.getName();
    private static final Regex DEFAULT = Regex.TOP;

    /** If annotated with @Regex, create a RegexVal qualifier. **/
    @Override
    public Regex fromAnnotations(Collection<? extends AnnotationMirror> annos) {

        for (AnnotationMirror anno: annos) {
            if (AnnotationUtils.annotationName(anno).equals(regexName)) {
                Integer value = AnnotationUtils.getElementValue(anno, "value", Integer.class, true);
                return new Regex.RegexVal(value);
            }
        }
        return DEFAULT;
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        return AnnotationUtils.annotationName(anno).equals(regexName);
    }
}
