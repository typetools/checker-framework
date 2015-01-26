package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.RegexQualifierHierarchy;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.ClassRegexParam;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.MethodRegexParam;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.MultiRegex;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.PolyRegex;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.Var;
import org.checkerframework.checker.experimental.regex_qual_poly.qual.Wild;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.Wildcard;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * Convert {@link org.checkerframework.checker.regex.qual.Regex}
 * annotations into a {@link Regex} qualifier with support for
 * PolyRegex, Var, Wild annotations.
 *
 */
public class RegexAnnotationConverter extends SimpleQualifierParameterAnnotationConverter<Regex> {

    public RegexAnnotationConverter() {
        super(new CombiningOperation.Lub<>(new RegexQualifierHierarchy()),
                new CombiningOperation.Glb<>(new RegexQualifierHierarchy()),
                MultiRegex.class.getPackage().getName() + ".Multi",
                new HashSet<>(Arrays.asList(org.checkerframework.checker.experimental.regex_qual_poly.qual.Regex.class.getName())),
                new HashSet<>(Arrays.asList(
                        org.checkerframework.checker.regex.qual.Regex.class.getName(),
                        org.checkerframework.checker.regex.qual.PolyRegex.class.getName())),
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

            Integer value = AnnotationUtils.getElementValue(anno, "value", Integer.class, true);
            return new Regex.RegexVal(value);
        }
        return null;
    }

    @Override
    protected QualParams<Regex> specialCaseHandle(AnnotationMirror anno) {

        if (AnnotationUtils.annotationName(anno).equals(
                org.checkerframework.checker.regex.qual.Regex.class.getName())) {

            Integer value = AnnotationUtils.getElementValue(anno, "value", Integer.class, true);
            return new QualParams<>(new GroundQual<Regex>(new Regex.RegexVal(value)));

        } else if (AnnotationUtils.annotationName(anno).equals(
                org.checkerframework.checker.regex.qual.PolyRegex.class.getName())) {

            return new QualParams<>(new QualVar<>(POLY_NAME, BOTTOM, TOP));
        }

        ErrorReporter.errorAbort("Unexpected AnnotationMirror found in special case handling: " + anno);
        return null;
    }

    /**
     * This override sets up a polymorphic qualifier when the old PolyRegex annotaiton is used.
     */
    @Override
    protected boolean hasPolyAnnotationCheck(ExtendedTypeMirror type) {
        if (type == null) {
            return false;
        }

        for (AnnotationMirror anno : type.getAnnotationMirrors()) {
            if (AnnotationUtils.annotationName(anno).equals(org.checkerframework.checker.regex.qual.PolyRegex.class.getName())) {
                return true;
            }
        }
        return super.hasPolyAnnotationCheck(type);
    }
}
