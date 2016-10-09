package org.checkerframework.qualframework.base.format;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.TypeMirrorConverter;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.format.PrettyQualParamsFormatter;
import org.checkerframework.qualframework.qual.QualifierKey;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collection;

/**
 * DefaultQualifiedTypeFormatter formats QualifiedTypeMirrors into Strings.
 *
 * This implementation used a component AnnotatedTypeFormatter to drive the formatting
 * and an AnnotationFormatter that converts @QualifierKey annotations to the qualifier, which
 * is then formatted by a QualFormatter.
 */
public class DefaultQualifiedTypeFormatter<Q, QUAL_FORMATTER extends QualFormatter<Q>> implements
        QualifiedTypeFormatter<Q> {

    protected final TypeMirrorConverter<Q> converter;
    protected final QUAL_FORMATTER qualFormatter;
    protected final boolean defaultPrintInvisibleQualifiers;

    protected final AnnotatedTypeFormatter adapter;
    protected final AnnotationFormatter annoAdapter;
    protected final boolean defaultPrintVerboseGenerics;

    public DefaultQualifiedTypeFormatter(
            QUAL_FORMATTER qualFormatter,
            TypeMirrorConverter<Q> converter,
            boolean defaultPrintVerboseGenerics,
            boolean defaultPrintInvisibleQualifiers) {

        this.converter = converter;
        this.defaultPrintVerboseGenerics = defaultPrintVerboseGenerics;
        this.defaultPrintInvisibleQualifiers = defaultPrintInvisibleQualifiers;
        this.qualFormatter = qualFormatter;
        this.annoAdapter = createAnnotationFormatter();
        this.adapter = createAnnotatedTypeFormatter(annoAdapter);
    }

    /**
     * Create the AnnotatedTypeFormatter to be used as the underling formatter. This formatter
     * should use formatter as its AnnotationFormatter.
     *
     * @param annotationFormatter an AnnotationFormatter that is configured to printout qualifiers using
     *                            qualFormatter
     * @return the AnnotatedTypeFormatter
     */
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter(AnnotationFormatter annotationFormatter) {
        return new DefaultAnnotatedTypeFormatter(annotationFormatter, defaultPrintVerboseGenerics, defaultPrintInvisibleQualifiers);
    }

    @Override
    public String format(QualifiedTypeMirror<Q> qtm) {
        return adapter.format(converter.getAnnotatedType(qtm));
    }

    @Override
    public String format(QualifiedTypeMirror<Q> qtm, boolean printInvisibles) {
        return adapter.format(converter.getAnnotatedType(qtm), printInvisibles);
    }

    @Override
    public QUAL_FORMATTER getQualFormatter() {
        return qualFormatter;
    }

    protected AnnotationFormatter createAnnotationFormatter() {
        return new AnnoToQualFormatter();
    }

    /**
     * Formats an @QualifierKey annotation by looking up the corresponding {@link QualParams} and
     * formatting it using a {@link PrettyQualParamsFormatter}.
     */
    protected class AnnoToQualFormatter extends DefaultAnnotationFormatter {

        @SideEffectFree
        public String formatAnnotationString(Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            for (AnnotationMirror obj : annos) {
                if (obj == null) {
                    ErrorReporter.errorAbort("Found unexpected null AnnotationMirror " +
                            "when formatting annotation mirror: " + annos);
                }

                if (!AnnotationUtils.areSameByClass(obj, QualifierKey.class)) {
                    ErrorReporter.errorAbort("Tried to format something other than an @QualifierKey annotation: " + obj);
                } else {
                    Q qual = converter.getQualifier(obj);
                    String result = qualFormatter.format(qual, printInvisible);
                    if (result != null) {
                        sb.append(result);
                        sb.append(" ");
                    }
                }
            }
            return sb.toString();
        }

        @Override
        protected void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
            if (!AnnotationUtils.areSameByClass(am, QualifierKey.class)) {
                ErrorReporter.errorAbort("Tried to format something other than an @QualifierKey annotation: " + am);
            } else {
                Q qual = converter.getQualifier(am);
                String result = qualFormatter.format(qual);
                if (result != null) {
                    sb.append(result);
                }
            }
        }
    }
}
