package org.checkerframework.checker.tainting;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.QualifiedNameable;

import org.checkerframework.javacutil.AnnotationUtils;

import org.checkerframework.qualframework.base.AnnotationConverter;

import org.checkerframework.checker.qualparam.QualifierParameterAnnotationConverter;
import org.checkerframework.checker.qualparam.CombiningOperation;
import org.checkerframework.checker.qualparam.PolyQual.GroundQual;
import org.checkerframework.checker.qualparam.PolyQual.QualVar;
import org.checkerframework.checker.qualparam.QualParams;
import org.checkerframework.checker.qualparam.Wildcard;

import org.checkerframework.checker.tainting.qual.*;

public class TaintingAnnotationConverter implements QualifierParameterAnnotationConverter<Tainting> {
    private Map<String, Wildcard<Tainting>> lookup;
    private CombiningOperation<Tainting> lubOp;

    public TaintingAnnotationConverter() {
        QualVar<Tainting> mainVar = new QualVar<>("Main", Tainting.UNTAINTED, Tainting.TAINTED);

        lookup = new HashMap<>();
        lookup.put(Untainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED));
        lookup.put(Tainted.class.getName(), new Wildcard<>(Tainting.TAINTED));
        lookup.put(UseMain.class.getName(), new Wildcard<>(mainVar));
        lookup.put(ExtendsUntainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED, Tainting.UNTAINTED));
        lookup.put(ExtendsTainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED, Tainting.TAINTED));
        lookup.put(ExtendsMain.class.getName(), new Wildcard<>(
                    new GroundQual<>(Tainting.UNTAINTED), mainVar));

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
            default:
                break;
        }

        return result;
    }
}
