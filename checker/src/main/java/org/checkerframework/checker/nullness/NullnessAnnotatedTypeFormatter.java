package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;

import java.util.Set;

/** A DefaultAnnotatedTypeFormatter that prints null literals without their annotations. */
public class NullnessAnnotatedTypeFormatter extends DefaultAnnotatedTypeFormatter {
    public NullnessAnnotatedTypeFormatter(
            boolean printVerboseGenerics, boolean printInvisibleQualifiers) {
        super(
                new NullnessFormattingVisitor(
                        new DefaultAnnotationFormatter(),
                        printVerboseGenerics,
                        printInvisibleQualifiers));
    }

    protected static class NullnessFormattingVisitor extends FormattingVisitor {

        public NullnessFormattingVisitor(
                AnnotationFormatter annoFormatter,
                boolean printVerboseGenerics,
                boolean defaultInvisiblesSetting) {
            super(annoFormatter, printVerboseGenerics, defaultInvisiblesSetting);
        }

        @Override
        public String visitNull(AnnotatedNullType type, Set<AnnotatedTypeMirror> visiting) {
            if (type.getAnnotation(Nullable.class) != null) {
                // The null type will be understood as nullable by readers (I hope), therefore omit
                // the annotations if they are @Nullable.
                // Note: The visitTypeVariable will still print lower bounds with Null kind as
                // "Void"
                if (!currentPrintInvisibleSetting) {
                    return "null (NullType)";
                }
            }

            return super.visitNull(type, visiting);
        }
    }
}
