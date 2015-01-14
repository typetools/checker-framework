package org.checkerframework.qualframework.poly.format;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.base.TypeMirrorConverter;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.QualParams;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * DefaultQualParamsAnnotatedTypeFormatterAdapter is used to format {@link AnnotatedTypeMirror}s
 * that have @Key qualifiers into the double chevron {@code &lt;&lt; Q &gt;&ht} output format.
 */
public class DefaultQualParamsAnnotatedTypeFormatterAdapter extends DefaultAnnotatedTypeFormatter {

    public <T> DefaultQualParamsAnnotatedTypeFormatterAdapter(
            TypeMirrorConverter<? extends QualParams<T>> converter,
            DefaultQualParamsFormatter<T> formatter,
            boolean printInvisibleQualifiers) {

        super(new FormattingVisitor<T>(
                    converter,
                    new AnnotationFormatter<T>(converter, formatter),
                    formatter,
                    printInvisibleQualifiers));
    }

    protected static class FormattingVisitor<Q> extends DefaultAnnotatedTypeFormatter.FormattingVisitor {

        private final TypeMirrorConverter<? extends QualParams<Q>> converter;
        private final DefaultQualParamsFormatter<Q> formatter;

        public FormattingVisitor(
                TypeMirrorConverter<? extends QualParams<Q>> converter,
                org.checkerframework.framework.util.AnnotationFormatter annoFormatter,
                DefaultQualParamsFormatter<Q> formatter,
                boolean defaultInvisiblesSetting) {

            super(annoFormatter, defaultInvisiblesSetting);

            this.converter = converter;
            this.formatter = formatter;
        }

        /**
         * visitDeclared changes the supertype behavior to print primary qualifiers before the class name
         * and the qualifier parameters inside double chevrons after the class name.
         */
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

            // Print out primary qualifiers first
            for (AnnotationMirror anno : type.getAnnotations()) {

                // Print out any qualifier parameters (without printing primary).
                if (AnnotationUtils.areSameByClass(anno, TypeMirrorConverter.Key.class)) {
                    PolyQual<Q> qual = converter.getQualifier(anno).getPrimary();
                    String result = formatter.format(qual);
                    if (result.length() > 0) {
                        sb.append(result);
                        sb.append(" ");
                    }
                }
            }

            // Print out type name
            sb.append(smpl);

            // Now print out all qual params, without printing primary qual.
            boolean first = true;
            for (AnnotationMirror anno : type.getAnnotations()) {
                if (AnnotationUtils.areSameByClass(anno, TypeMirrorConverter.Key.class)) {
                    QualParams<Q> qual = converter.getQualifier(anno);
                    String result = formatter.format(qual, false);
                    if (result.length() > 0) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ");
                        }
                        sb.append(result);
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

    /**
     * Formats an @Key annotation by looking up the corresponding {@link QualParams} and
     * formatting it using a {@link DefaultQualParamsFormatter}.
     */
    public static class AnnotationFormatter<Q> extends DefaultAnnotationFormatter {

        private final TypeMirrorConverter<? extends QualParams<Q>> converter;
        private final DefaultQualParamsFormatter<Q> formatter;

        public AnnotationFormatter(
                TypeMirrorConverter<? extends QualParams<Q>> converter,
                DefaultQualParamsFormatter<Q> formatter) {

            this.converter = converter;
            this.formatter = formatter;
        }

        @SideEffectFree
        public String formatAnnotationString(Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (AnnotationMirror obj : annos) {
                if (obj == null) {
                    ErrorReporter.errorAbort("AnnotatedTypeMirror.formatAnnotationString: found null AnnotationMirror!");
                }

                if (!AnnotationUtils.areSameByClass(obj, TypeMirrorConverter.Key.class)) {
                    ErrorReporter.errorAbort("Tried to format something other than an @Key annotation: " + obj);
                } else {
                    QualParams<Q> qual = converter.getQualifier(obj);
                    String result = formatter.format(qual);
                    if (result.length() > 0) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ");
                        }
                        sb.append(result);
                    }
                }
            }
            return sb.toString();
        }

        @Override
        protected void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
            if (!AnnotationUtils.areSameByClass(am, TypeMirrorConverter.Key.class)) {
                ErrorReporter.errorAbort("Tried to format something other than an @Key annotation: " + am);
            } else {
                QualParams<Q> poly = converter.getQualifier(am);
                sb.append(formatter.format(poly));
            }
        }
    }
}