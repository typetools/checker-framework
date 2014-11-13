package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.checker.experimental.regex_qual_poly.qual.ClassRegexParam;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.MethodRegexParam;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.MultiRegex;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.PolyRegex;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.Var;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.Wild;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.Arrays;
import java.util.Map;

/**
 * Convert @Regex annotations into qualifiers.
 *
 */
public class RegexAnnotationConverter extends SimpleQualifierParameterAnnotationConverter<Regex> {

    public RegexAnnotationConverter() {
        super(new CombiningOperation.Lub<>(new RegexQualifierHierarchy()),
                MultiRegex.class.getPackage().getName() + ".Multi",
                Arrays.asList(org.checkerframework.checker.experimental.regex_qual_poly.qual.Regex.class.getName()),
                ClassRegexParam.class,
                MethodRegexParam.class,
                PolyRegex.class,
                Var.class,
                Wild.class,
                Regex.TOP,
                Regex.BOTTOM, Regex.TOP);
    }

    @Override
    public Regex getQualifier(AnnotationMirror anno) {
        if (AnnotationUtils.annotationName(anno).equals(
                org.checkerframework.checker.experimental.regex_qual_poly.qual.Regex.class.getName())) {

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                    anno.getElementValues().entrySet()) {

                if (entry.getKey().getSimpleName().toString().equals("value")) {
                    return new Regex.RegexVal((Integer)entry.getValue().getValue());
                }
            }
            return new Regex.RegexVal(0);
        }
        return null;
    }
}
