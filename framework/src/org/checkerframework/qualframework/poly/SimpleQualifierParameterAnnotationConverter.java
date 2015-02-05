package org.checkerframework.qualframework.poly;


import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.util.ExtendedExecutableType;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SimpleQualifierParameterAnnotationConverter abstracts the logic to convert annotations to qualifiers
 * for typical qual-poly types systems that that support @Wild, @Var, and qualifier parameters.
 *
 * {@link SimpleQualifierParameterAnnotationConverter#getQualifier} should be implemented to convert
 * an annotation to a type system specific qualifier (e.g. @Regex or @Tainted).
 *
 */
public abstract class SimpleQualifierParameterAnnotationConverter<Q> implements QualifierParameterAnnotationConverter<Q> {

    // The default "Target" in an annotation is the primary qualifier
    // We can't use null in the annotation, so we use this special value
    public static final String PRIMARY_TARGET="_primary";
    public static final String TARGET_PARAM_NAME = "param";
    // The name of the qualifier parameter to use for polymorphic qualifiers.
    public static final String POLY_NAME = "_poly";

    // Annotation field names
    protected static final String SOURCE_VALUE_NAME = "arg";
    protected static final String WILDCARD_NAME = "wildcard";

    protected final String MULTI_ANNO_NAME_PREFIX;
    protected final CombiningOperation<Q> lowerOp;
    protected final CombiningOperation<Q> upperOp;
    protected final Q BOTTOM;
    protected final Q TOP;
    protected final Q DEFAULT_QUAL;

    private final Class<? extends Annotation> classAnno;
    private final Class<? extends Annotation> methodAnno;
    private final Class<? extends Annotation> polyAnno;
    private final Class<? extends Annotation> varAnno;
    private final Class<? extends Annotation> wildAnno;

    private final Set<String> supportedAnnotationNames;
    private final Set<String> specialCaseAnnotations;

    /**
     * Construct a SimpleQualifierParameterAnnotationConverter.
     */
    public SimpleQualifierParameterAnnotationConverter(AnnotationConverterConfiguration<Q> config) {

        this.MULTI_ANNO_NAME_PREFIX = config.getMultiAnnoNamePrefix();
        if (config.getSupportedAnnotationNames() == null ||
                config.getSupportedAnnotationNames().isEmpty()) {
            ErrorReporter.errorAbort("supportedAnnotationNames must be a list of type system qualifiers.");
        }
        this.supportedAnnotationNames = config.getSupportedAnnotationNames();

        if (config.getSpecialCaseAnnotations() == null) {
            this.specialCaseAnnotations = new HashSet<>();
        } else {
            this.specialCaseAnnotations = config.getSpecialCaseAnnotations();
        }
        this.lowerOp = config.getLowerOp();
        this.upperOp = config.getUpperOp();
        this.classAnno = config.getClassAnno();
        this.methodAnno = config.getMethodAnno();
        this.polyAnno = config.getPolyAnno();
        this.varAnno = config.getVarAnno();
        this.wildAnno = config.getWildAnno();
        this.TOP = config.getTop();
        this.BOTTOM = config.getBottom();
        this.DEFAULT_QUAL = config.getDefaultQual();
    }

    /**
     * Create a type system qualifier based on an annotation.
     *
     * @param anno the annotation
     * @return the resulting qualifier
     */
    public abstract Q getQualifier(AnnotationMirror anno);

    /**
     * Special case handle the AnnotationMirror. Useful for when more control
     * is need when processing an annotation.
     */
    protected QualParams<Q> specialCaseHandle(AnnotationMirror anno) {
        return null;
    }

    /**
     * Convert a list of AnnotationMirrors to a QualParams. Each AnnotationMirror is converted into a QualParams.
     * The resulting QualParams are merged together to create the result.
     *
     * If no primary qualifier is found, DEFAULT_QUAL will be used.
     *
     * @param annos the collection of type annotations to parse
     * @return the QualParams
     */
    @Override
    public QualParams<Q> fromAnnotations(Collection<? extends AnnotationMirror> annos) {
        Map<String, Wildcard<Q>> params = new HashMap<>();
        PolyQual<Q> primary = null;
        for (AnnotationMirror anno : annos) {
            Map<String, Wildcard<Q>> qualMap;
            PolyQual<Q> newPrimary;
            if (specialCaseAnnotations.contains(AnnotationUtils.annotationName(anno))) {
                QualParams<Q> result = specialCaseHandle(anno);
                qualMap = result;
                newPrimary = result.getPrimary();
            } else {
                qualMap = getQualifierMap(anno);
                newPrimary = getPrimaryAnnotation(anno);
            }

            mergeParams(params, qualMap);

            if (primary != null && newPrimary != null) {
                primary = primary.combineWith(newPrimary, lowerOp);
            } else {
                primary = newPrimary;
            }
        }

        if (primary == null) {
            primary = new GroundQual<>(DEFAULT_QUAL);
        }

        return new QualParams<>(params, primary);
    }

    /**
     * Merge two QualParam maps. Each annotation will be converted into a Map, so the map from
     * multiple annotations will need to be merged together.
     */
    private void mergeParams(
            Map<String, Wildcard<Q>> params,
            Map<String, Wildcard<Q>> newParams) {
        if (newParams == null) {
            return;
        }

        for (String name : newParams.keySet()) {
            if (!params.containsKey(name)) {
                params.put(name, newParams.get(name));
                continue;
            }

            Wildcard<Q> oldWild = params.get(name);
            Wildcard<Q> newWild = newParams.get(name);
            Wildcard<Q> combinedWild = oldWild.combineWith(newWild, lowerOp, upperOp);

            //System.err.printf("COMBINE[%s]: %s + %s = %s\n", name, oldWild, newWild, combinedWild);
            params.put(name, combinedWild);
        }
    }

    private Map<String, Wildcard<Q>> getQualifierMap(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);

        Map<String, Wildcard<Q>> result = null;
        if (name.startsWith(MULTI_ANNO_NAME_PREFIX)) {
            result = new HashMap<>();
            List<AnnotationMirror> subAnnos = AnnotationUtils.getElementValueArray(
                    anno, "value", AnnotationMirror.class, true);
            for (AnnotationMirror subAnno : subAnnos) {
                mergeParams(result, getQualifierMap(subAnno));
            }

        } else if (supportedAnnotationNames.contains(name)) {
            Q qual = getQualifier(anno);
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                result = Collections.singletonMap(target, handleWildcard(anno, new Wildcard<>(qual)));
            }

        } else if (name.equals(varAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            String value = AnnotationUtils.getElementValue(anno, SOURCE_VALUE_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                Wildcard<Q> valueWild = handleWildcard(anno, new Wildcard<>(
                        new QualVar<>(value, BOTTOM, TOP)));
                result = Collections.singletonMap(target, valueWild);
            }

        } else if (name.equals(polyAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                Wildcard<Q> polyWild = new Wildcard<>(
                        new QualVar<>(POLY_NAME, BOTTOM, TOP));
                result = Collections.singletonMap(target, polyWild);
            }

        } else if (name.equals(wildAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            result = Collections.singletonMap(target, new Wildcard<>(BOTTOM, TOP));
        }

        return result;
    }

    private Wildcard<Q> handleWildcard(AnnotationMirror anno, Wildcard<Q> current) {
        org.checkerframework.qualframework.poly.qual.Wildcard wildcard =
                AnnotationUtils.getElementValueEnum(anno, WILDCARD_NAME,
                        org.checkerframework.qualframework.poly.qual.Wildcard.class, true);

        switch (wildcard) {
            case SUPER:
                return new Wildcard<>(current.getLowerBound(), new GroundQual<>(TOP));
            case EXTENDS:
                return new Wildcard<>(new GroundQual<>(BOTTOM), current.getUpperBound());
            default:
                return current;
        }
    }

    private PolyQual<Q> getPrimaryAnnotation(AnnotationMirror anno) {

        String name = AnnotationUtils.annotationName(anno);
        PolyQual<Q> newQual = null;

        if (supportedAnnotationNames.contains(name)) {
            Q qual = getQualifier(anno);
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (PRIMARY_TARGET.equals(target)) {
                newQual = new GroundQual<>(qual);
            }

        } else if (name.equals(varAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            String value = AnnotationUtils.getElementValue(anno, SOURCE_VALUE_NAME, String.class, true);
            if (PRIMARY_TARGET.equals(target)) {
                newQual = new QualVar<>(value, BOTTOM, TOP);
            }

        } else if (name.equals(polyAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (PRIMARY_TARGET.equals(target)) {
                newQual = new QualVar<>(POLY_NAME, BOTTOM, TOP);
            }
        }
        return newQual;
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        // Avoid running getQualifierMap on Multi* annotations, since that could
        // involve a nontrivial amount of work.
        return name.startsWith(MULTI_ANNO_NAME_PREFIX)
            || specialCaseAnnotations.contains(name)
            || getQualifierMap(anno) != null
            || getPrimaryAnnotation(anno) != null;
    }

    @Override
    public Set<String> getDeclaredParameters(Element elt, Set<AnnotationMirror> declAnnotations, ExtendedTypeMirror type) {
        Set<String> result = new HashSet<>();
        try {
            for (AnnotationMirror anno: declAnnotations) {
                if (AnnotationUtils.areSameByClass(anno, methodAnno)
                    || AnnotationUtils.areSameByClass(anno, classAnno)) {

                    result.add(AnnotationUtils.getElementValue(anno, "value",
                            String.class, true));
                }
            }
        } catch (Exception e) {
            ErrorReporter.errorAbort("AnnotationConverter not configured correctly. Error looking up 'value' field.");
        }

        switch (elt.getKind()) {
            case CONSTRUCTOR:
            case METHOD:
                if (hasPolyAnnotation((ExtendedExecutableType)type)) {
                    result.add(POLY_NAME);
                }
                break;

            default:
                break;
        }

        return result;
    }

    /**
     * @return true if type has a polymorphic qualifier.
     */
    private boolean hasPolyAnnotation(ExtendedExecutableType type) {
        if (hasPolyAnnotationCheck(type.getReturnType())) {
            return true;
        }
        if (hasPolyAnnotationCheck(type.getReceiverType())) {
            return true;
        }
        for (ExtendedTypeMirror param : type.getParameterTypes()) {
            if (hasPolyAnnotationCheck(param)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if type has a polymorphic qualifier.
     */
    protected boolean hasPolyAnnotationCheck(ExtendedTypeMirror type) {
        if (type == null) {
            return false;
        }
        for (AnnotationMirror anno : type.getAnnotationMirrors()) {
            if (AnnotationUtils.annotationName(anno).equals(polyAnno.getName())) {
                return true;
            }
        }
        return false;
    }
}
