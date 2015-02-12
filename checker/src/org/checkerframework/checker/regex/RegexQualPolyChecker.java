package org.checkerframework.checker.regex;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.Regex.PartialRegex;
import org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.poly.QualifierParameterChecker;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxFormatterConfiguration;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter.AnnotationParts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link Checker} for the Regex-Qual-Param type system.
 */
public class RegexQualPolyChecker extends QualifierParameterChecker<Regex> {

    @Override
    protected RegexQualifiedTypeFactory createTypeFactory() {
        return new RegexQualifiedTypeFactory(this);
    }

    protected Set<?> getInvisibleQualifiers() {
        return new HashSet<>(
                Arrays.asList(
                        this.getTypeFactory().getQualifierHierarchy().getBottom(),
                        this.getTypeFactory().getQualifierHierarchy().getTop(),
                        Regex.BOTTOM, Regex.TOP));
    }

    @Override
    protected SurfaceSyntaxFormatterConfiguration<Regex> createSurfaceSyntaxFormatterConfiguration() {
        return new RegexSurfaceSyntaxConfiguration();
    }

    private class RegexSurfaceSyntaxConfiguration extends SurfaceSyntaxFormatterConfiguration<Regex> {

        private final Set<String> SUPPRESS_NAMES = new HashSet<>(
                Arrays.asList("RegexTop", "RegexBot", "PartialRegex"));

        public RegexSurfaceSyntaxConfiguration() {
            super(Regex.TOP, Regex.BOTTOM,
                    RegexQualPolyChecker.this.getContext().getTypeFactory().getQualifierHierarchy().getTop(),
                    RegexQualPolyChecker.this.getContext().getTypeFactory().getQualifierHierarchy().getBottom());
        }

        @Override
        protected boolean shouldPrintAnnotation(AnnotationParts anno, boolean printInvisibleQualifiers) {
            return printInvisibleQualifiers || !(SUPPRESS_NAMES.contains(anno.getName()));
        }

        @Override
        protected AnnotationParts getTargetTypeSystemAnnotation(Regex qual) {

            if (qual instanceof RegexVal) {
                AnnotationParts anno = new AnnotationParts("Regex");
                anno.put("value", String.valueOf(((RegexVal) qual).getCount()));
                return anno;

            } else if (qual instanceof PartialRegex) {
                AnnotationParts anno = new AnnotationParts("PartialRegex");
                anno.putQuoted("value", ((PartialRegex) qual).getPartialValue());
                return anno;

            } else {
                return new AnnotationParts(qual.toString());
            }
        }
    }

}
