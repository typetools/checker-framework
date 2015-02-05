package org.checkerframework.checker.experimental.tainting_qual_poly;

import org.checkerframework.checker.experimental.tainting_qual_poly.qual.ClassTaintingParam;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.MethodTaintingParam;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.MultiTainted;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.PolyTainting;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.Tainted;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.Untainted;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.Var;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.Wild;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

import javax.lang.model.element.AnnotationMirror;
import java.util.Arrays;
import java.util.HashSet;

public class TaintingAnnotationConverter extends SimpleQualifierParameterAnnotationConverter<Tainting> {

    public TaintingAnnotationConverter() {
        super(new CombiningOperation.Lub<>(new TaintingQualifierHierarchy()),
                new CombiningOperation.Glb<>(new TaintingQualifierHierarchy()),
                MultiTainted.class.getPackage().getName() + ".Multi",
                new HashSet<>(Arrays.asList(Tainted.class.getName(), Untainted.class.getName())),
                null,
                ClassTaintingParam.class,
                MethodTaintingParam.class,
                PolyTainting.class,
                Var.class,
                Wild.class,
                Tainting.TAINTED,
                Tainting.UNTAINTED,
                Tainting.TAINTED
        );
    }

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
