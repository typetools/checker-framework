package org.checkerframework.checker.experimental.tainting_qual_poly;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.Regex.PartialRegex;
import org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import org.checkerframework.qualframework.base.Checker;

import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.QualifierParameterChecker;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxFormatterConfiguration;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter.AnnotationParts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TaintingChecker extends QualifierParameterChecker<Tainting> {

    @Override
    protected TaintingQualifiedTypeFactory createTypeFactory() {
        return new TaintingQualifiedTypeFactory(this);
    }


    @Override
    protected SurfaceSyntaxFormatterConfiguration<Tainting> createSurfaceSyntaxFormatterConfiguration() {
        return new TaintingSurfaceSyntaxConfiguration();
    }

    private class TaintingSurfaceSyntaxConfiguration extends SurfaceSyntaxFormatterConfiguration<Tainting> {

        public TaintingSurfaceSyntaxConfiguration() {
            super(Tainting.TAINTED, Tainting.UNTAINTED,
                    TaintingChecker.this.getContext().getTypeFactory().getQualifierHierarchy().getTop(),
                    TaintingChecker.this.getContext().getTypeFactory().getQualifierHierarchy().getBottom());
        }

        @Override
        protected boolean shouldPrintAnnotation(boolean printInvisibleQualifiers, AnnotationParts anno) {
            return printInvisibleQualifiers;
        }

        @Override
        protected AnnotationParts getTargetTypeSystemAnnotation(Tainting qual) {

            switch(qual) {
                case TAINTED:
                    return new AnnotationParts("Tainted");
                case UNTAINTED:
                    return new AnnotationParts("Untainted");
                default:
                    return null; // Dead code
            }
        }
    }
}
