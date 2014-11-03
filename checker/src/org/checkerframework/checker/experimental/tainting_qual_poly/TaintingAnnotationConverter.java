package org.checkerframework.checker.experimental.tainting_qual_poly;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.javacutil.AnnotationUtils;

import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.QualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.Wildcard;

import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

public class TaintingAnnotationConverter implements QualifierParameterAnnotationConverter<Tainting> {

    private static final String TARGET_PARAM_NAME = "param";
    private static final String SOURCE_VALUE_NAME = "arg";
    private static final String WILDCARD_NAME = "wildcard";

    private CombiningOperation<Tainting> lubOp;

    // The default "Target" in an annotation is the primary qualifier
    // We can't use null in the annotation, so we must use a special value
    public static final String PRIMARY_TARGET="_primary";
    private static final String POLY_NAME = "_poly";
    private static final String MULTI_ANNO_NAME_PREFIX = MultiTainted.class.getPackage().getName() + ".Multi";

    private static final Tainting BOTTOM = Tainting.UNTAINTED;
    private static final Tainting TOP = Tainting.TAINTED;

    public TaintingAnnotationConverter() {
        this.lubOp = new CombiningOperation.Lub<>(new TaintingQualifierHierarchy());
    }

    @Override
    public QualParams<Tainting> fromAnnotations(Collection<? extends AnnotationMirror> annos) {
        Map<String, Wildcard<Tainting>> params = new HashMap<>();
        for (AnnotationMirror anno : annos) {
            mergeParams(params, getQualifierMap(anno));
        }

        PolyQual<Tainting> primary = getPrimaryAnnotation(annos);
        if (primary == null) {
            // TODO: Add a way to change this defaulting
            primary = new PolyQual.GroundQual<>(TOP);
        }

        return (params.isEmpty() && primary == null ? null : new QualParams<>(params, primary));
    }

    private void mergeParams(
            Map<String, Wildcard<Tainting>> params,
            Map<String, Wildcard<Tainting>> newParams) {
        if (newParams == null) {
            return;
        }

        for (String name : newParams.keySet()) {
            if (!params.containsKey(name)) {
                params.put(name, newParams.get(name));
                continue;
            }

            Wildcard<Tainting> oldWild = params.get(name);
            Wildcard<Tainting> newWild = newParams.get(name);
            Wildcard<Tainting> combinedWild = oldWild.combineWith(newWild, lubOp, lubOp);

            //System.err.printf("COMBINE[%s]: %s + %s = %s\n", name, oldWild, newWild, combinedWild);
            params.put(name, combinedWild);
        }
    }

    private Map<String, Wildcard<Tainting>> getQualifierMap(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);

        if (name.startsWith(MULTI_ANNO_NAME_PREFIX)) {
            Map<String, Wildcard<Tainting>> result = new HashMap<>();
            AnnotationMirror[] subAnnos = AnnotationUtils.getElementValue(
                    anno, "value", AnnotationMirror[].class, true);
            for (AnnotationMirror subAnno : subAnnos) {
                mergeParams(result, getQualifierMap(subAnno));
            }
            return result;

        } else if (name.equals(Tainted.class.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                return Collections.singletonMap(target, handleWildcard(anno, new Wildcard<>(Tainting.TAINTED)));
            }

        } else if (name.equals(Untainted.class.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                return Collections.singletonMap(target, handleWildcard(anno, new Wildcard<>(Tainting.UNTAINTED)));
            }

        } else if (name.equals(Var.class.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            String value = AnnotationUtils.getElementValue(anno, SOURCE_VALUE_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                Wildcard<Tainting> valueWild = handleWildcard(anno, new Wildcard<>(
                        new QualVar<>(value, BOTTOM, TOP)));
                return Collections.singletonMap(target, valueWild);
            }

        } else if (name.equals(PolyTainting.class.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            if (!PRIMARY_TARGET.equals(target)) {
                Wildcard<Tainting> polyWild = new Wildcard<>(
                        new QualVar<>(POLY_NAME, BOTTOM, TOP));
                return Collections.singletonMap(target, polyWild);
            }
        } else if (name.equals(Wild.class.getName())) {
            String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
            return Collections.singletonMap(target, new Wildcard<>(BOTTOM, TOP));
        }

        return null;
    }


    private Wildcard<Tainting> handleWildcard(AnnotationMirror anno, Wildcard<Tainting> current) {
        org.checkerframework.checker.experimental.tainting_qual_poly.qual.Wildcard wildcard =
                AnnotationUtils.getElementValueEnum(anno, WILDCARD_NAME,
                org.checkerframework.checker.experimental.tainting_qual_poly.qual.Wildcard.class, true);

        switch (wildcard) {
            case SUPER:
                return new Wildcard<>(current.getLowerBound(), new GroundQual<>(TOP));
            case EXTENDS:
                return new Wildcard<>(new GroundQual<>(BOTTOM), current.getUpperBound());
            default:
                return current;
        }
    }

    private PolyQual<Tainting> getPrimaryAnnotation(Collection<? extends AnnotationMirror> annos) {

        PolyQual<Tainting> result = null;
        for (AnnotationMirror anno : annos) {
            String name = AnnotationUtils.annotationName(anno);
            PolyQual<Tainting> newQual = null;
            if (name.equals(Tainted.class.getName())) {
                String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
                if (PRIMARY_TARGET.equals(target)) {
                    newQual = new PolyQual.GroundQual<>(Tainting.TAINTED);
                }
            } else if (name.equals(Untainted.class.getName())) {
                String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
                if (PRIMARY_TARGET.equals(target)) {
                    newQual =  new PolyQual.GroundQual<>(Tainting.UNTAINTED);
                }
            } else if (name.equals(Var.class.getName())) {
                String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
                String value = AnnotationUtils.getElementValue(anno, SOURCE_VALUE_NAME, String.class, true);
                if (PRIMARY_TARGET.equals(target)) {
                    newQual =  new QualVar<>(value, BOTTOM, TOP);
                }
            } else if (name.equals(PolyTainting.class.getName())) {
                String target = AnnotationUtils.getElementValue(anno, TARGET_PARAM_NAME, String.class, true);
                if (PRIMARY_TARGET.equals(target)) {
                    newQual = new QualVar<>(POLY_NAME, BOTTOM, TOP);
                }
            }

            if (result != null && newQual != null) {
                result = result.combineWith(newQual, lubOp);
            } else {
                result = newQual;
            }
        }
        return result;
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        // Avoid running getQualifierMap on Multi* annotations, since that could
        // involve a nontrivial amount of work.
        return name.startsWith(MULTI_ANNO_NAME_PREFIX)
            || getQualifierMap(anno) != null
            || getPrimaryAnnotation(Arrays.asList(anno)) != null;
    }

    @Override
    public Set<String> getDeclaredParameters(Element elt) {
        Set<String> result = new HashSet<>();
        for (Annotation a : elt.getAnnotationsByType(MethodTaintingParam.class)) {
            result.add(((MethodTaintingParam)a).value());
        }
        for (Annotation a : elt.getAnnotationsByType(ClassTaintingParam.class)) {
            result.add(((ClassTaintingParam)a).value());
        }

        switch (elt.getKind()) {
            case CONSTRUCTOR:
            case METHOD:
                if (hasPolyAnnotation((ExecutableElement)elt)) {
                    result.add(POLY_NAME);
                }
                break;

            default:
                break;
        }

        return result;
    }

    private boolean hasPolyAnnotation(ExecutableElement elt) {
        if (hasPolyAnnotation(elt.getReturnType())) {
            return true;
        }
        if (hasPolyAnnotation(elt.getReceiverType())) {
            return true;
        }
        for (VariableElement paramElt : elt.getParameters()) {
            if (hasPolyAnnotation(paramElt.asType())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPolyAnnotation(TypeMirror type) {
        if (type == null) {
            return false;
        }
        for (AnnotationMirror anno : type.getAnnotationMirrors()) {
            if (AnnotationUtils.annotationName(anno).equals(PolyTainting.class.getName())) {
                return true;
            }
        }
        return false;
    }
}
