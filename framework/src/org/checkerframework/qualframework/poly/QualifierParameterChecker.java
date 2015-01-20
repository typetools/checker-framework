package org.checkerframework.qualframework.poly;

import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.poly.format.PrettyQualifiedTypeFormatter;
import org.checkerframework.qualframework.base.format.DefaultQualifiedTypeFormatter;
import org.checkerframework.qualframework.base.format.QualifiedTypeFormatter;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxFormatterConfiguration;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter;

/**
 * QualifierParameterChecker extends Checker to configure QualifiedTypeFormatters
 * specific to QualParams qualifiers.
 */
public abstract class QualifierParameterChecker<Q> extends Checker<QualParams<Q>> {

    @Override
    public QualifiedTypeFormatter<QualParams<Q>> createQualifiedTypeFormatter() {

        if (getContext().getOptionConfiguration().hasOption("printQualifierParametersAsAnnotations")) {
            SurfaceSyntaxFormatterConfiguration<Q> config = createSurfaceSyntaxFormatterConfiguration();
            if (config != null) {
                SurfaceSyntaxQualParamsFormatter<Q> formatter = new SurfaceSyntaxQualParamsFormatter<Q>(config);
                return new DefaultQualifiedTypeFormatter<>(
                        formatter,
                        getContext().getCheckerAdapter().getTypeMirrorConverter(),
                        getContext().getOptionConfiguration().hasOption("printAllQualifiers")
                );
            }
        }

        return new PrettyQualifiedTypeFormatter<>(
                getContext().getCheckerAdapter().getTypeMirrorConverter(),
                getInvisibleQualifiers(),
                getContext().getOptionConfiguration().hasOption("printAllQualifiers")
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
