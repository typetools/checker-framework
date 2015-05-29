package org.checkerframework.qualframework.poly;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Plain old java object to help configure SimpleQualifierParameterAnnotationConverter.
 */
public class AnnotationConverterConfiguration<Q> {

    private final CombiningOperation<Q> lowerOp;
    private final CombiningOperation<Q> upperOp;
    private final String multiAnnoNamePrefix;
    private final Set<String> supportedAnnotationNames;
    private final Set<String> specialCaseAnnotations;
    private final Class<? extends Annotation> classAnno;
    private final Class<? extends Annotation> methodAnno;
    private final Class<? extends Annotation> polyAnno;
    private final Class<? extends Annotation> varAnno;
    private final Class<? extends Annotation> wildAnno;
    private final Q top;
    private final Q bottom;
    private final Q defaultQual;

    /**
     * Construct a configuration object
     *
     * @param lowerOp The operation to perform on the lower bound when combining annotations
     * @param upperOp The operation to perform on the upper bound when combining annotations
     * @param multiAnnoNamePrefix The package and class name prefix for repeatable annotations
     * @param supportedAnnotationNames A list of supported annotations specific to the type system
     * @param specialCaseAnnotations A list of annotations to be processed solely by the specialCaseProcess method
     * @param classAnno The annotation for class parameter declaration
     * @param methodAnno The annotation for method parameter declaration
     * @param polyAnno The poly annotation for the type system
     * @param varAnno The polymorphic qualifier use variable
     * @param wildAnno The annotation for specifying a wildcard
     * @param top The top qualifier in the system
     * @param bottom The bottom qualifier in the system
     * @param defaultQual The qualifier to use if no annotations result in a qualifier.
     */
    public AnnotationConverterConfiguration(CombiningOperation<Q> lowerOp,
            CombiningOperation<Q> upperOp,
            String multiAnnoNamePrefix,
            Set<String> supportedAnnotationNames,
            Set<String> specialCaseAnnotations,
            Class<? extends Annotation> classAnno,
            Class<? extends Annotation> methodAnno,
            Class<? extends Annotation> polyAnno,
            Class<? extends Annotation> varAnno,
            Class<? extends Annotation> wildAnno,
            Q top,
            Q bottom,
            Q defaultQual) {

        this.lowerOp = lowerOp;
        this.upperOp = upperOp;
        this.multiAnnoNamePrefix = multiAnnoNamePrefix;
        this.supportedAnnotationNames = supportedAnnotationNames;
        this.specialCaseAnnotations = specialCaseAnnotations;
        this.classAnno = classAnno;
        this.methodAnno = methodAnno;
        this.polyAnno = polyAnno;
        this.varAnno = varAnno;
        this.wildAnno = wildAnno;
        this.top = top;
        this.bottom = bottom;
        this.defaultQual = defaultQual;
    }


    public CombiningOperation<Q> getLowerOp() {
        return lowerOp;
    }

    public CombiningOperation<Q> getUpperOp() {
        return upperOp;
    }

    public String getMultiAnnoNamePrefix() {
        return multiAnnoNamePrefix;
    }

    public Set<String> getSupportedAnnotationNames() {
        return supportedAnnotationNames;
    }

    public Set<String> getSpecialCaseAnnotations() {
        return specialCaseAnnotations;
    }

    public Class<? extends Annotation> getClassAnno() {
        return classAnno;
    }

    public Class<? extends Annotation> getMethodAnno() {
        return methodAnno;
    }

    public Class<? extends Annotation> getPolyAnno() {
        return polyAnno;
    }

    public Class<? extends Annotation> getVarAnno() {
        return varAnno;
    }

    public Class<? extends Annotation> getWildAnno() {
        return wildAnno;
    }

    public Q getTop() {
        return top;
    }

    public Q getBottom() {
        return bottom;
    }

    public Q getDefaultQual() {
        return defaultQual;
    }
}
