package org.checkerframework.qualframework.poly.format;

import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.qualframework.base.TypeMirrorConverter;
import org.checkerframework.qualframework.base.format.DefaultQualifiedTypeFormatter;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.QualParams;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.List;
import java.util.Set;

/**
 * PrettyQualifiedTypeFormatter formats QualifiedTypeMirrors with QualParams qualifiers
 * into Strings.
 *
 * PrettyQualifiedTypeFormatter prints the primary qualifier of a QualParams before a
 * declared type's name, and the map of qualifier parameters after the declared type's name.
 */
public class PrettyQualifiedTypeFormatter<Q> extends DefaultQualifiedTypeFormatter<QualParams<Q>, PrettyQualParamsFormatter<Q>> {

    public PrettyQualifiedTypeFormatter(
            TypeMirrorConverter<QualParams<Q>> converter,
            Set<?> invisibleQualifiers,
            boolean useOldFormat,
            boolean defaultPrintInvisibleQualifiers) {

        super(new PrettyQualParamsFormatter<Q>(invisibleQualifiers), converter, useOldFormat, defaultPrintInvisibleQualifiers);
    }

    @Override
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter(AnnotationFormatter annotationFormatter) {
        return new QualParamsAnnoTypeFormatter<Q>(converter, qualFormatter,
                annotationFormatter, useOldFormat, defaultPrintInvisibleQualifiers);
    }

    /**
     * QualParamsAnnoTypeFormatter is an DefaultAnnotatedTypeFormatter that overrides the visitDeclared
     * method in order to pretty print QualParams.
     */
    private static class QualParamsAnnoTypeFormatter<Q> extends DefaultAnnotatedTypeFormatter {

        protected QualParamsAnnoTypeFormatter(
                TypeMirrorConverter<QualParams<Q>> converter,
                QualParamsFormatter<Q> formatter,
                org.checkerframework.framework.util.AnnotationFormatter annoFormatter,
                boolean printVerboseGenerics,
                boolean defaultPrintInvisibleQualifier) {
            super(new FormattingVisitor<Q>(converter, annoFormatter, formatter, printVerboseGenerics, defaultPrintInvisibleQualifier));
        }

        protected static class FormattingVisitor<Q> extends DefaultAnnotatedTypeFormatter.FormattingVisitor {

            private final TypeMirrorConverter<QualParams<Q>> converter;
            private final QualParamsFormatter<Q> formatter;

            public FormattingVisitor(
                    TypeMirrorConverter<QualParams<Q>> converter,
                    org.checkerframework.framework.util.AnnotationFormatter annoFormatter,
                    QualParamsFormatter<Q>  formatter,
                    boolean printVerboseGenerics,
                    boolean defaultInvisiblesSetting) {

                super(annoFormatter, printVerboseGenerics, defaultInvisiblesSetting);
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
                        String result = formatter.format(qual, currentPrintInvisibleSetting);
                        if (result != null) {
                            sb.append(result);
                            sb.append(" ");
                        }
                    }
                }

                // Print out type name
                sb.append(smpl);

                // Finally print out qualifier parameters
                boolean first = true;
                for (AnnotationMirror anno : type.getAnnotations()) {
                    if (AnnotationUtils.areSameByClass(anno, TypeMirrorConverter.Key.class)) {
                        QualParams<Q> qual = converter.getQualifier(anno);
                        String result = formatter.format(qual, false, currentPrintInvisibleSetting);
                        if (result != null) {
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
    }
}
