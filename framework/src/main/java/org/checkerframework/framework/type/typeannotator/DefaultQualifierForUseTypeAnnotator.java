package org.checkerframework.framework.type.typeannotator;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import org.checkerframework.framework.qual.DefaultQualifierForUse;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class DefaultQualifierForUseTypeAnnotator extends TypeAnnotator {

    public DefaultQualifierForUseTypeAnnotator(AnnotatedTypeFactory typeFactory) {
        super(typeFactory);
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Void aVoid) {
        Element element = type.getUnderlyingType().asElement();
        // TODO: deal with multiple @DefaultQualifierForUse for different hierarchies.
        AnnotationMirror defaultQualifier =
                typeFactory.getDeclAnnotation(element, DefaultQualifierForUse.class);
        if (defaultQualifier == null) {
            // No DefaultQualifierForUse, use the explicit type qualifier if one exists.
            AnnotatedTypeMirror explicitAnnoOnDecl = typeFactory.fromElement(element);
            Set<AnnotationMirror> explicitAnnos = explicitAnnoOnDecl.getAnnotations();
            type.addMissingAnnotations(explicitAnnos);
        } else {
            boolean hasDefault =
                    !AnnotationUtils.getElementValue(defaultQualifier, "none", Boolean.class, true);
            if (hasDefault) {
                List<Name> annoClasses =
                        AnnotationUtils.getElementValueClassNames(defaultQualifier, "value", true);
                for (Name annoClass : annoClasses) {
                    AnnotationMirror anno =
                            AnnotationBuilder.fromName(typeFactory.getElementUtils(), annoClass);
                    type.addMissingAnnotations(Collections.singleton(anno));
                }
            }
        }
        return super.visitDeclared(type, aVoid);
    }
}
