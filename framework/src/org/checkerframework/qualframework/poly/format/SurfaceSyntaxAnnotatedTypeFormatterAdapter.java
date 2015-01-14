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

/**
 * SurfaceSyntaxAnnotatedTypeFormatterAdapter is used to format {@link AnnotatedTypeMirror}s
 * that have @Key qualifiers into a String that contains the annotations that when written would
 * correspond to the type being formatted.
 *
 * Not all types can be created by writing annotations; in those cases the output from
 * the qualifier toString is used.
 */
public class SurfaceSyntaxAnnotatedTypeFormatterAdapter extends DefaultAnnotatedTypeFormatter {

    public <T> SurfaceSyntaxAnnotatedTypeFormatterAdapter(
            TypeMirrorConverter<? extends QualParams<T>> converter,
            SurfaceSyntaxQualParamsFormatter<T> formatter,
            boolean printInvisibleQualifiers) {

        super(new FormattingVisitor(new AnnotationFormatter<T>(converter, formatter),
                    printInvisibleQualifiers));
    }

    /**
     * Custom annotation formatter that converts annotations to a Qualifier and then uses
     * SurfaceSyntaxQualParamsFormatter to create a String output.
     */
    public static class AnnotationFormatter<Q> extends DefaultAnnotationFormatter {

        private final TypeMirrorConverter<? extends QualParams<Q>> converter;
        private final SurfaceSyntaxQualParamsFormatter<Q> formatter;

        public AnnotationFormatter(
                TypeMirrorConverter<? extends QualParams<Q>> converter,
                SurfaceSyntaxQualParamsFormatter<Q> formatter) {
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
                // Only add a space if the previous produced output.
                if (sb.length() > lenBefore) {
                    sb.append(" ");
                }
            }
            return sb.toString();
        }

        @Override
        protected void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
            if (AnnotationUtils.areSameByClass(am, TypeMirrorConverter.Key.class)) {
                QualParams<Q> poly = converter.getQualifier(am);
                sb.append(formatter.format(poly));
            } else{
                super.formatAnnotationMirror(am, sb);
            }
        }
    }

}