package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.checker.regex.qual.UnknownRegex;
import org.checkerframework.qualframework.base.AnnotationConverter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import java.util.Collection;
import java.util.Map;

/**
 * Convert @Regex annotations into qualifiers.
 *
 */
public class RegexAnnotationConverter implements AnnotationConverter<Regex> {
    private String regexName;
//    private String unknown;

    public RegexAnnotationConverter() {
        regexName = org.checkerframework.checker.experimental.regex_qual.qual.Regex.class.getName();
//        unknown = UnknownRegex.class.getName();
    }

    @Override
    public Regex fromAnnotations(Collection<? extends AnnotationMirror> annos) {

        for (AnnotationMirror anno: annos) {
            if (getAnnotationTypeName(anno).equals(regexName)) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                        anno.getElementValues().entrySet()) {

                    if (entry.getKey().getSimpleName().toString().equals("value")) {
                        return new Regex.RegexVal((Integer)entry.getValue().getValue());
                    }
                }
                return new Regex.RegexVal(0);
            }
        }
        return Regex.TOP;
    }

    private String getAnnotationTypeName(AnnotationMirror anno) {
        Element elt = anno.getAnnotationType().asElement();
        if (elt instanceof QualifiedNameable) {
            @SuppressWarnings("unchecked")
            QualifiedNameable nameable = (QualifiedNameable)elt;
            return nameable.getQualifiedName().toString();
        } else {
            return null;
        }
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        String name = getAnnotationTypeName(anno);
        return name.equals(regexName);
    }
}
