package org.checkerframework.qualframework.poly;

import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.poly.format.PrettyQualifiedTypeFormatter;
import org.checkerframework.qualframework.base.format.DefaultQualifiedTypeFormatter;
import org.checkerframework.qualframework.base.format.QualifiedTypeFormatter;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxFormatterConfiguration;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter;
import org.checkerframework.qualframework.util.QualifierContext;

/**
 * QualifierParameterChecker extends Checker to configure QualifiedTypeFormatters
 * specific to QualParams qualifiers.
 */
public abstract class QualifierParameterChecker<Q> extends Checker<QualParams<Q>> {

    @Override
    public QualifiedTypeFormatter<QualParams<Q>> createQualifiedTypeFormatter() {

        QualifierContext<QualParams<Q>> context = getContext();

        boolean printVerboseGenerics = context.getOptionConfiguration().hasOption("printVerboseGenerics");
        boolean printAllQualifiers   = context.getOptionConfiguration().hasOption("printAllQualifiers");

        if (context.getOptionConfiguration().hasOption("printQualifierParametersAsAnnotations")) {
            SurfaceSyntaxFormatterConfiguration<Q> config = createSurfaceSyntaxFormatterConfiguration();
            if (config != null) {
                SurfaceSyntaxQualParamsFormatter<Q> formatter = new SurfaceSyntaxQualParamsFormatter<Q>(config);
                return new DefaultQualifiedTypeFormatter<>(
                        formatter,
                        context.getCheckerAdapter().getTypeMirrorConverter(),
                        printVerboseGenerics,
                        printAllQualifiers
                );
            }
        }

        return new PrettyQualifiedTypeFormatter<>(
                context.getCheckerAdapter().getTypeMirrorConverter(),
                getInvisibleQualifiers(),
                printVerboseGenerics,
                printAllQualifiers
        );
    }

    /**
     * Type systems should override this method to provide a configuration that is required to format
     * qualifiers in an annotation syntax.
     *
     * Returning null will disable the "printQualifierParametersAsAnnotations" output mode.
     *
     * @return the configuration
     */
    protected SurfaceSyntaxFormatterConfiguration<Q> createSurfaceSyntaxFormatterConfiguration() {
        return null;
    }
}
