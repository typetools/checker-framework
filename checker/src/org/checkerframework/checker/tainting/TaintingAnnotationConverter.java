package org.checkerframework.checker.tainting;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;


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

    private String getAnnotationTypeName(AnnotationMirror anno) {
        Element elt = anno.getAnnotationType().asElement();
        if (elt instanceof QualifiedNameable) {
            QualifiedNameable nameable = (QualifiedNameable)elt;
            return nameable.getQualifiedName().toString();
        } else {
            return null;
        }
    }

    @Override
    public QualParams<Tainting> fromAnnotations(Collection<? extends AnnotationMirror> annos) {
        Map<String, Wildcard<Tainting>> params = new HashMap<>();
        for (AnnotationMirror anno : annos) {
            String name = "Main";

            String annoName = getAnnotationTypeName(anno);
            Wildcard<Tainting> value = lookup.get(annoName);

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
        String name = getAnnotationTypeName(anno);
        return lookup.containsKey(name);
    }

    @Override
    public Set<String> getDeclaredParameters(Element elt) {
        Set<String> result = new HashSet<>();
        for (Annotation a : elt.getAnnotationsByType(TaintingParam.class)) {
            result.add(((TaintingParam)a).value());
        }
        return result;
    }
}
