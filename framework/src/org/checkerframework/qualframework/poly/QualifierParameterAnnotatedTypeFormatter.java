package org.checkerframework.qualframework.poly;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.qualframework.base.TypeMirrorConverter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class QualifierParameterAnnotatedTypeFormatter extends DefaultAnnotatedTypeFormatter {


    public QualifierParameterAnnotatedTypeFormatter(TypeMirrorConverter<? extends QualParams<?>> converter,
            boolean printInvisibleQualifiers) {

        super(new QualifierParameterFormattingVisitor(
                converter,
                new QualifierParameterAnnotationFormatter(converter),
                printInvisibleQualifiers));
    }

    protected static class QualifierParameterAnnotationFormatter extends DefaultAnnotationFormatter {

        private final TypeMirrorConverter<? extends QualParams<?>> converter;

        public QualifierParameterAnnotationFormatter(TypeMirrorConverter<? extends QualParams<?>> converter) {
            this.converter = converter;
        }

        @Override
        protected void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
            if (AnnotationUtils.areSameByClass(am, TypeMirrorConverter.Key.class)) {
                sb.append("@" + converter.getQualifier(am).getPrimary().toString());
            } else{
                super.formatAnnotationMirror(am, sb);
            }
        }
    }

    protected static class QualifierParameterFormattingVisitor extends FormattingVisitor {

        private final TypeMirrorConverter<? extends QualParams<?>> converter;

        public QualifierParameterFormattingVisitor(TypeMirrorConverter<? extends QualParams<?>> converter,
                AnnotationFormatter annoFormatter,
                boolean defaultInvisiblesSetting) {
            super(annoFormatter, defaultInvisiblesSetting);
            this.converter = converter;
        }

        @Override
        public String visitDeclared(AnnotatedDeclaredType type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();
            if (type.isDeclaration()) {
                sb.append("/*DECL*/ ");
            }
            final Element typeElt = type.getUnderlyingType().asElement();
            String smpl = typeElt.getSimpleName().toString();
            if (smpl.isEmpty()) {
                // For anonymous classes smpl is empty - toString
                // of the element is more useful.
                smpl = typeElt.toString();
            }
            sb.append(annoFormatter.formatAnnotationString(type.getAnnotations(), currentPrintInvisibleSetting));
            sb.append(smpl);

            for (AnnotationMirror anno : type.getAnnotations()) {
                if (AnnotationUtils.areSameByClass(anno, TypeMirrorConverter.Key.class)) {
                    QualParams<?> qual = converter.getQualifier(anno);
                    if (qual.size() > 0) {
                        sb.append("<<");
                        boolean first = true;
                        for (Entry<String, ? extends Wildcard<?>> entry : qual.entrySet()) {
                            if (!first) {
                                sb.append(",");
                            } else {
                                first = false;
                            }
                            sb.append(entry.getKey() + "=" +
                                    (entry.getValue() != null ? "@" + entry.getValue() : null));
                        }
                        sb.append(">>");
                    }
                }
            }

            final List<AnnotatedTypeMirror> typeArgs = type.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                sb.append("<");

                boolean isFirst = true;
                for (AnnotatedTypeMirror typeArg : typeArgs) {
                    if (!isFirst) sb.append(", ");
                    sb.append(visit(typeArg, visiting));
                    isFirst = false;
                }
                sb.append(">");
            }
            return sb.toString();
        }

    }


}