package org.checkerframework.qualframework.poly;


import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.util.ExtendedExecutableType;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SimpleQualifierParameterAnnotationConverter<Q> implements QualifierParameterAnnotationConverter<Q> {

    // The default "Target" in an annotation is the primary qualifier
    // We can't use null in the annotation, so we use this special value
    public static final String PRIMARY_TARGET="_primary";
    public static final String TARGET_PARAM_NAME = "param";

    // The name of the qualifier parameter to use for polymorphic qualifiers.
    private static final String POLY_NAME = "_poly";

    private static final String SOURCE_VALUE_NAME = "arg";
    private static final String WILDCARD_NAME = "wildcard";

    private final String MULTI_ANNO_NAME_PREFIX;
    private final CombiningOperation<Q> lubOp;
    private final Q BOTTOM;
    private final Q TOP;
    private final Q DEFAULT_QUAL;

    private final Class<? extends Annotation> classAnno;
    private final Class<? extends Annotation> methodAnno;
    private final Class<? extends Annotation> polyAnno;
    private final Class<? extends Annotation> varAnno;
    private final Class<? extends Annotation> wildAnno;
    private final List<String> supportedAnnotationNames;

    public SimpleQualifierParameterAnnotationConverter(CombiningOperation<Q> lubOp,
            String multiAnnoNamePrefix,
            List<String> supportedAnnotationNames,
            Class<? extends Annotation> classAnno,
            Class<? extends Annotation> methodAnno,
            Class<? extends Annotation> polyAnno,
            Class<? extends Annotation> varAnno,
            Class<? extends Annotation> wildAnno,
            Q top,
            Q bottom,
            Q defaultQual) {

        this.MULTI_ANNO_NAME_PREFIX = multiAnnoNamePrefix;
        this.supportedAnnotationNames = supportedAnnotationNames;
        this.lubOp = lubOp;
        this.classAnno = classAnno;
        this.methodAnno = methodAnno;
        this.polyAnno = polyAnno;
        this.varAnno = varAnno;
        this.wildAnno = wildAnno;
        this.TOP = top;
        this.BOTTOM = bottom;
        this.DEFAULT_QUAL = defaultQual;
    }

    public abstract Q getQualifier(AnnotationMirror anno);

    @Override
    public QualParams<Q> fromAnnotations(Collection<? extends AnnotationMirror> annos) {
        Map<String, Wildcard<Q>> params = new HashMap<>();
        PolyQual<Q> primary = null;
        for (AnnotationMirror anno : annos) {
            mergeParams(params, getQualifierMap(anno));

            PolyQual<Q> newPrimary = getPrimaryAnnotation(anno);
            if (primary != null && newPrimary != null) {
                primary = primary.combineWith(newPrimary, lubOp);
            } else {
                primary = newPrimary;
            }
        }

        if (primary == null) {
            // TODO: This is pretty coarse defaulting
            primary = new GroundQual<>(DEFAULT_QUAL);
        }

        return (params.isEmpty() && primary == null ? null : new QualParams<>(params, primary));
    }

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
            Wildcard<Q> combinedWild = oldWild.combineWith(newWild, lubOp, lubOp);

            //System.err.printf("COMBINE[%s]: %s + %s = %s\n", name, oldWild, newWild, combinedWild);
            params.put(name, combinedWild);
        }
    }

    private Map<String, Wildcard<Q>> getQualifierMap(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);

        if (name.startsWith(MULTI_ANNO_NAME_PREFIX)) {
            Map<String, Wildcard<Q>> result = new HashMap<>();
            AnnotationMirror[] subAnnos = AnnotationUtils.getElementValue(
                    anno, "value", AnnotationMirror[].class, true);
            for (AnnotationMirror subAnno : subAnnos) {
                mergeParams(result, getQualifierMap(subAnno));
            }
            return result;
        } else if (supportedAnnotationNames.contains(name)) {
            Q qual = getQualifier(anno);
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                return Collections.singletonMap(target, handleWildcard(anno, new Wildcard<>(qual)));
            }
        } else if (name.equals(varAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            String value = AnnotationUtils.getElementValue(anno, SOURCE_VALUE_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                Wildcard<Q> valueWild = handleWildcard(anno, new Wildcard<>(
                        new QualVar<>(value, BOTTOM, TOP)));
                return Collections.singletonMap(target, valueWild);
            }

        } else if (name.equals(polyAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                Wildcard<Q> polyWild = new Wildcard<>(
                        new QualVar<>(POLY_NAME, BOTTOM, TOP));
                return Collections.singletonMap(target, polyWild);
            }
        } else if (name.equals(wildAnno.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            return Collections.singletonMap(target, new Wildcard<>(BOTTOM, TOP));
        }

        return null;
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
            || getQualifierMap(anno) != null
            || getPrimaryAnnotation(anno) != null;
    }

    @Override
    public Set<String> getDeclaredParameters(Element elt, ExtendedTypeMirror type) {
        Set<String> result = new HashSet<>();
        try {
            for (Annotation a : elt.getAnnotationsByType(methodAnno)) {
                result.add((String) methodAnno.cast(a).getClass().getMethod("value").invoke(a));
            }
            for (Annotation a : elt.getAnnotationsByType(classAnno)) {
                result.add((String) classAnno.cast(a).getClass().getMethod("value").invoke(a));
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

    private boolean hasPolyAnnotationCheck(ExtendedTypeMirror type) {
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
