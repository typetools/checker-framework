package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.qualframework.base.AnnotationConverter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.Collection;
import java.util.Map;

/**
 * Convert {@link org.checkerframework.checker.regex.qual.Regex}
 * annotations into {@link Regex} qualifiers.
 */
public class RegexAnnotationConverter implements AnnotationConverter<Regex> {

    private String regexName;
    private static final Regex DEFAULT = Regex.TOP;

    public RegexAnnotationConverter() {
        regexName = org.checkerframework.checker.regex.qual.Regex.class.getName();
    }

    @Override
    public Regex fromAnnotations(Collection<? extends AnnotationMirror> annos) {

        for (AnnotationMirror anno: annos) {
            if (AnnotationUtils.annotationName(anno).equals(regexName)) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                        anno.getElementValues().entrySet()) {

                    if (entry.getKey().getSimpleName().toString().equals("value")) {
                        return new Regex.RegexVal((Integer)entry.getValue().getValue());
                    }
                }
                return new Regex.RegexVal(0);
            }
        }
        return DEFAULT;
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        return AnnotationUtils.annotationName(anno).equals(regexName);
    }
}
