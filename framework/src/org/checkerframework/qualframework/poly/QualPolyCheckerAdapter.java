package org.checkerframework.qualframework.poly;

import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.poly.format.DefaultQualParamsAnnotatedTypeFormatterAdapter;
import org.checkerframework.qualframework.poly.format.DefaultQualParamsFormatter;
import org.checkerframework.qualframework.poly.format.QualParamsFormatter;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxAnnotatedTypeFormatterAdapter;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter;

import java.util.Arrays;

/**
 * Checker adapter that Qual-Poly type systems should extend.
 *
 * @param <Q> The ground qual
 */
public class QualPolyCheckerAdapter<Q> extends CheckerAdapter<QualParams<Q>> {

    /**
     * Constructs a {@link CheckerAdapter} from an underlying qualifier-based
     * {@link Checker}.
     *
     * @param underlying
     */
    public QualPolyCheckerAdapter(Checker<QualParams<Q>> underlying) {
        super(underlying);
    }

    /**
     * Create a custom {@link AnnotatedTypeFormatter} that has special rules
     * for printing qualified types.
     *
     * @param printInvisibleQualifiers true if invisible qualifier should be printed
     * @return an AnnotatedTypeFormatter
     */
    @Override
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter(boolean printInvisibleQualifiers) {

        if (true) {
            SurfaceSyntaxQualParamsFormatter<Q> qualFormatter =
                    createSurfaceSyntaxQualParamsFormatter(printInvisibleQualifiers);

            if (qualFormatter != null) {
                return new SurfaceSyntaxAnnotatedTypeFormatterAdapter(getTypeMirrorConverter(), qualFormatter,
                        printInvisibleQualifiers);
            }
        }

        return new DefaultQualParamsAnnotatedTypeFormatterAdapter(
                getTypeMirrorConverter(),
                new DefaultQualParamsFormatter<Q>(printInvisibleQualifiers),
                printInvisibleQualifiers);

    }

    protected SurfaceSyntaxQualParamsFormatter<Q> createSurfaceSyntaxQualParamsFormatter(
            boolean printInvisibleQualifiers) {

        return null;
    }
}
