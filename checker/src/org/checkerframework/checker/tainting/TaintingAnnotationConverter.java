package org.checkerframework.checker.tainting;

import org.checkerframework.checker.tainting.qual.ClassTaintingParam;
import org.checkerframework.checker.tainting.qual.MethodTaintingParam;
import org.checkerframework.checker.tainting.qual.MultiTainted;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.checker.tainting.qual.Var;
import org.checkerframework.checker.tainting.qual.Wild;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.AnnotationConverterConfiguration;
import org.checkerframework.qualframework.poly.CombiningOperation.Lub;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

import javax.lang.model.element.AnnotationMirror;
import java.util.Arrays;
import java.util.HashSet;

public class TaintingAnnotationConverter extends SimpleQualifierParameterAnnotationConverter<Tainting> {

    public TaintingAnnotationConverter() {
        super(new AnnotationConverterConfiguration<>(
                new Lub<>(new TaintingQualifierHierarchy()),
                new Lub<>(new TaintingQualifierHierarchy()),
                MultiTainted.class.getPackage().getName() + ".Multi",
                new HashSet<>(Arrays.asList(Tainted.class.getName(), Untainted.class.getName())),
                null,
                ClassTaintingParam.class,
                MethodTaintingParam.class,
                PolyTainted.class,
                Var.class,
                Wild.class,
                Tainting.TAINTED,
                Tainting.UNTAINTED,
                Tainting.TAINTED));
    }

    /**
     * Convert @Tainted and @Untainted annotations into a Tainting enum
     */
    @Override
    public Tainting getQualifier(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        if (name.equals(Tainted.class.getName())) {
            return Tainting.TAINTED;
        } else if (name.equals(Untainted.class.getName())) {
            return Tainting.UNTAINTED;
        } else {
            ErrorReporter.errorAbort("Unexpected AnnotationMirror encountered when processing supported qualifiers:" + name);
            return null; // Dead
        }
    }
}
