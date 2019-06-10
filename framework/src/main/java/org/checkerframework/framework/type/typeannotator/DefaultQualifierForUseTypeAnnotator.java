package org.checkerframework.framework.type.typeannotator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import org.checkerframework.framework.qual.DefaultQualifierForUse;
import org.checkerframework.framework.qual.NoDefaultQualifierForUse;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.CollectionUtils;

public class DefaultQualifierForUseTypeAnnotator extends TypeAnnotator {

    public DefaultQualifierForUseTypeAnnotator(AnnotatedTypeFactory typeFactory) {
        super(typeFactory);
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Void aVoid) {
        Element element = type.getUnderlyingType().asElement();
        Set<AnnotationMirror> annosToApply = getDefaultAnnosForUses(element);
        type.addMissingAnnotations(annosToApply);
        return super.visitDeclared(type, aVoid);
    }

    Map<Element, Set<AnnotationMirror>> elementToDefaults = CollectionUtils.createLRUCache(100);

    public void clearCache() {
        elementToDefaults.clear();
    }

    protected Set<AnnotationMirror> getDefaultAnnosForUses(Element element) {
        if (elementToDefaults.containsKey(element)) {
            return elementToDefaults.get(element);
        }
        Set<AnnotationMirror> explictAnnos = getExplicitAnnos(element);
        AnnotationMirrorSet defaultAnnos = getSupportedDefaultAnnosForUse(element);
        AnnotationMirrorSet noDefaultAnnos = getSupportedNoDefaultHierarchy(element);
        AnnotationMirrorSet annosToApply = new AnnotationMirrorSet();

        for (AnnotationMirror top : typeFactory.getQualifierHierarchy().getTopAnnotations()) {
            if (noDefaultAnnos.contains(top)) {
                continue;
            }
            AnnotationMirror defaultAnno =
                    typeFactory
                            .getQualifierHierarchy()
                            .findAnnotationInHierarchy(defaultAnnos, top);
            if (defaultAnno != null) {
                annosToApply.add(defaultAnno);
            } else {
                AnnotationMirror explict =
                        typeFactory
                                .getQualifierHierarchy()
                                .findAnnotationInHierarchy(explictAnnos, top);
                if (explict != null) {
                    annosToApply.add(explict);
                }
            }
        }
        elementToDefaults.put(element, annosToApply);
        return annosToApply;
    }

    protected Set<AnnotationMirror> getExplicitAnnos(Element element) {
        AnnotatedTypeMirror explicitAnnoOnDecl = typeFactory.fromElement(element);
        return explicitAnnoOnDecl.getAnnotations();
    }

    AnnotationMirrorSet getSupportedDefaultAnnosForUse(Element element) {
        AnnotationMirror defaultQualifier =
                typeFactory.getDeclAnnotation(element, DefaultQualifierForUse.class);
        if (defaultQualifier == null) {
            return new AnnotationMirrorSet();
        }
        return supportedAnnos(defaultQualifier);
    }

    AnnotationMirrorSet getSupportedNoDefaultHierarchy(Element element) {
        AnnotationMirror noDefaultQualifier =
                typeFactory.getDeclAnnotation(element, NoDefaultQualifierForUse.class);
        if (noDefaultQualifier == null) {
            return new AnnotationMirrorSet();
        }
        return supportedAnnos(noDefaultQualifier);
    }

    AnnotationMirrorSet supportedAnnos(AnnotationMirror defaultQualifier) {
        List<Name> annoClassNames =
                AnnotationUtils.getElementValueClassNames(defaultQualifier, "value", true);
        AnnotationMirrorSet supportAnnos = new AnnotationMirrorSet();
        for (Name annoName : annoClassNames) {
            AnnotationMirror anno =
                    AnnotationBuilder.fromName(typeFactory.getElementUtils(), annoName);
            if (typeFactory.isSupportedQualifier(anno)) {
                supportAnnos.add(anno);
            }
        }
        return supportAnnos;
    }
}
