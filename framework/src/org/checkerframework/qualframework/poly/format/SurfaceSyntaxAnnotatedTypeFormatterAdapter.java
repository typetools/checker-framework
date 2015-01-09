package org.checkerframework.qualframework.poly.format;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.base.TypeMirrorConverter;
import org.checkerframework.qualframework.poly.QualParams;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SurfaceSyntaxAnnotatedTypeFormatterAdapter extends DefaultAnnotatedTypeFormatter {

    public <T> SurfaceSyntaxAnnotatedTypeFormatterAdapter(
            TypeMirrorConverter<? extends QualParams<T>> converter,
            SurfaceSyntaxQualParamsFormatter<T> formatter,
            boolean printInvisibleQualifiers) {

        super(new FormattingVisitor<T>(
                    converter,
                    new AnnotationFormatter<T>(converter, formatter),
                    formatter,
                    printInvisibleQualifiers));
    }

    protected static class FormattingVisitor<T> extends DefaultAnnotatedTypeFormatter.FormattingVisitor {

        private final TypeMirrorConverter<? extends QualParams<T>> converter;
        private final SurfaceSyntaxQualParamsFormatter<T> formatter;

        public FormattingVisitor(
                TypeMirrorConverter<? extends QualParams<T>> converter,
                org.checkerframework.framework.util.AnnotationFormatter annoFormatter,
                SurfaceSyntaxQualParamsFormatter<T> formatter,
                boolean defaultInvisiblesSetting) {

            super(annoFormatter, defaultInvisiblesSetting);

            this.converter = converter;
            this.formatter = formatter;
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

    protected static class AnnotationFormatter<T> extends DefaultAnnotationFormatter {

        private final TypeMirrorConverter<? extends QualParams<T>> converter;
        private final SurfaceSyntaxQualParamsFormatter<T> formatter;

        public AnnotationFormatter(
                TypeMirrorConverter<? extends QualParams<T>> converter,
                SurfaceSyntaxQualParamsFormatter<T> formatter) {
            this.converter = converter;
            this.formatter = formatter;
        }

        @SideEffectFree
        public String formatAnnotationString(Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            for (AnnotationMirror obj : annos) {
                if (obj == null) {
                    ErrorReporter.errorAbort("AnnotatedTypeMirror.formatAnnotationString: found null AnnotationMirror!");
                }
                if (isInvisibleQualified(obj) && !printInvisible) {
                    continue;
                }
                int lenBefore = sb.length();
                formatAnnotationMirror(obj, sb);
                if (sb.length() > lenBefore) {
                    sb.append(" ");
                }
            }
            return sb.toString();
        }

        @Override
        protected void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
            if (AnnotationUtils.areSameByClass(am, TypeMirrorConverter.Key.class)) {
                QualParams<T> poly = converter.getQualifier(am);
                sb.append(formatter.format(poly));
            } else{
                super.formatAnnotationMirror(am, sb);
            }
        }
    }

}