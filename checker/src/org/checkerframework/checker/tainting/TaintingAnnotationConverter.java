package org.checkerframework.checker.tainting;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.javacutil.AnnotationUtils;

import org.checkerframework.qualframework.base.AnnotationConverter;

import org.checkerframework.qualframework.poly.QualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.Wildcard;

import org.checkerframework.checker.tainting.qual.*;

public class TaintingAnnotationConverter implements QualifierParameterAnnotationConverter<Tainting> {
    private Map<String, Wildcard<Tainting>> lookup;
    private CombiningOperation<Tainting> lubOp;

    public TaintingAnnotationConverter() {
        QualVar<Tainting> mainVar = new QualVar<>("Main", Tainting.UNTAINTED, Tainting.TAINTED);
        QualVar<Tainting> polyVar = new QualVar<>("_poly", Tainting.UNTAINTED, Tainting.TAINTED);

        lookup = new HashMap<>();
        lookup.put(Untainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED));
        lookup.put(Tainted.class.getName(), new Wildcard<>(Tainting.TAINTED));
        lookup.put(UseMain.class.getName(), new Wildcard<>(mainVar));
        lookup.put(ExtendsUntainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED, Tainting.UNTAINTED));
        lookup.put(ExtendsTainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED, Tainting.TAINTED));
        lookup.put(ExtendsMain.class.getName(), new Wildcard<>(
                    new GroundQual<>(Tainting.UNTAINTED), mainVar));
        lookup.put(PolyTainting.class.getName(), new Wildcard<>(polyVar));

        this.lubOp = new CombiningOperation.Lub<>(new TaintingQualifierHierarchy());
    }

    private Wildcard<Tainting> fromAnnotation(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        return lookup.get(name);
    }

    @Override
    public QualParams<Tainting> fromAnnotations(Collection<? extends AnnotationMirror> annos) {
        Map<String, Wildcard<Tainting>> params = new HashMap<>();
        for (AnnotationMirror anno : annos) {
            String name = "Main";

            Wildcard<Tainting> value = fromAnnotation(anno);
            if (value == null) {
                continue;
            }

            Wildcard<Tainting> oldValue = params.get(name);
            if (oldValue != null) {
                value = value.combineWith(oldValue, lubOp, lubOp);
            }

            params.put(name, value);
        }
        return (params.isEmpty() ? null : new QualParams<>(params));
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        return lookup.containsKey(name);
    }

    @Override
    public Set<String> getDeclaredParameters(Element elt) {
        Set<String> result = new HashSet<>();
        for (Annotation a : elt.getAnnotationsByType(TaintingParam.class)) {
            result.add(((TaintingParam)a).value());
        }

        switch (elt.getKind()) {
            case CLASS:
            case INTERFACE:
            case ENUM:
                result.add("Main");
                break;
            case CONSTRUCTOR:
            case METHOD:
                if (hasPolyAnnotation((ExecutableElement)elt)) {
                    result.add("_poly");
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
