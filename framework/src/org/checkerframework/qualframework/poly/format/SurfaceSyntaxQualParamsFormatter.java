package org.checkerframework.qualframework.poly.format;

import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.PolyQual.Combined;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.Wildcard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * SurfaceSyntaxQualParamsFormatter formats QualParams qualifiers into their annotation equivalent.
 *
 * Not all Qualifiers can be converted into annotations that could be written by the user, so the output
 * is not exact.
*/
public class SurfaceSyntaxQualParamsFormatter<Q> implements QualParamsFormatter<Q> {

    /* Object to hold the type-system specific information required to conver the quals to annotations. */
    private final SurfaceSyntaxFormatterConfiguration<Q> config;

    public SurfaceSyntaxQualParamsFormatter(SurfaceSyntaxFormatterConfiguration<Q> config) {
        this.config = config;
    }

    @Override
    public String format(QualParams<Q> params) {
        return format(params, true, true);
    }

    @Override
    public String format(QualParams<Q> params, boolean printInvisible) {
        return format(params, true, printInvisible);
    }

    @Override
    public String format(QualParams<Q> params, boolean printPrimary, boolean printInvisible) {
        StringBuffer sb = new StringBuffer();

        // Special exception for the top and bottom of the hierarchy.
        if (params == config.getQualTop()) {
            AnnotationParts anno = config.getTargetTypeSystemAnnotation(config.getTop());
            if (anno != null && config.shouldPrintAnnotation(printInvisible, anno)) {
                return anno.toString();
            } else {
                return null;
            }
        } else if (params == config.getQualBottom()) {
            AnnotationParts anno = config.getTargetTypeSystemAnnotation(config.getBottom());
            if (anno != null && config.shouldPrintAnnotation(printInvisible, anno)) {
                return anno.toString();
            } else {
                return null;
            }
        }

        // Primary
        boolean printedPrimary = false;
        if (printPrimary && params.getPrimary() != null) {
            AnnotationParts anno = createAnnotation(params.getPrimary(), printInvisible);
            if (anno != null && config.shouldPrintAnnotation(printInvisible, anno)) {
                printedPrimary = true;
                sb.append(anno.toString());
            }
        }

        // Qualifier Parameters
        boolean addSpace = printedPrimary;
        for (Entry<String, Wildcard<Q>> entry : params.entrySet()) {
            AnnotationParts anno = createAnnotation(entry.getValue(), entry.getKey(), printInvisible);
            if (anno != null && config.shouldPrintAnnotation(printInvisible, anno)) {
                if (addSpace) {
                    sb.append(" ");
                } else {
                    addSpace = true;
                }
                sb.append(anno.toString());
            }
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return null;
        }
    }

    @Override
    public String format(PolyQual<Q> polyQual, boolean printInvisible) {
        AnnotationParts anno = createAnnotation(polyQual, printInvisible);
        if (anno != null && config.shouldPrintAnnotation(printInvisible, anno)) {
            return anno.toString();
        } else {
            return null;
        }
    }

    private String formatQual(Q qual, boolean printInvisible) {
        AnnotationParts anno = config.getTargetTypeSystemAnnotation(qual);
        if (anno != null && config.shouldPrintAnnotation(printInvisible, anno)) {
            return anno.toString();
        } else {
            return null;
        }
    }

    private AnnotationParts createAnnotation(PolyQual<Q> polyQual, boolean printInvisible) {

        if (polyQual == null) {
            return null;

        } else if (polyQual instanceof Combined) {
            AnnotationParts anno = new AnnotationParts("Combine");
            Combined<Q> combined = (Combined<Q>) polyQual;
            // See if any of the vars should be printed
            List<String> formattedVars = new ArrayList<>();
            for (QualVar<Q> var : combined.getVars()) {
                String formatted = format(var, printInvisible);
                if (formatted != null) {
                    formattedVars.add(formatted);
                }
            }
            String formatted = formatQual(combined.getGround(), printInvisible);
            if (formatted != null) {
                formattedVars.add(formatted);
            }
            if (formattedVars.size() == 0) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("(");
            boolean first = true;
            for (String formattedVar :formattedVars) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(formattedVar);
            }
            sb.append(")");
            anno.putQuoted(combined.getOp().toString(), sb.toString());
            return anno;

        } else if (polyQual instanceof GroundQual) {
            AnnotationParts anno = config.getTargetTypeSystemAnnotation(((GroundQual<Q>) polyQual).getQualifier());
            return anno;

        } else if (polyQual instanceof QualVar) {

            QualVar<Q> qualVar = (QualVar<Q>) polyQual;

            String lower = formatQual(qualVar.getLowerBound(), printInvisible);
            String upper = formatQual(qualVar.getUpperBound(), printInvisible);

            AnnotationParts anno = new AnnotationParts("Var");
            anno.putQuoted("arg", qualVar.getName());

            // This is a range, there is no real annotation equivalent, so use a "range" field.
            if (lower != null || upper != null) {
                lower = lower == null? "" : lower;
                upper = upper == null? "" : upper;
                anno.putQuoted("range", qualVar.getName() + " ∈ [" + lower + ".." + upper + "]");
            }

            return anno;
        } else {

            ErrorReporter.errorAbort("Unknown PolyQual Subclass: " + polyQual.getClass());
            return null; // Dead code
        }
    }

    private AnnotationParts createAnnotation(Wildcard<Q> wildcard, String paramName, boolean printInvisible) {

        if (wildcard.isEmpty()) {
            ErrorReporter.errorAbort("Unable to convert wildcard: " + wildcard);
        }

        AnnotationParts result;
        if (wildcard.getLowerBound() instanceof QualVar
                || wildcard.getUpperBound() instanceof QualVar) {
            // Qualifier variables (or bounded wildcards)

            org.checkerframework.qualframework.poly.qual.Wildcard wildcardType;
            String argName;

            if (wildcard.getLowerBound() == wildcard.getUpperBound()) {
                wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.NONE;
                argName = ((QualVar<Q>)wildcard.getLowerBound()).getName();
            } else if (wildcard.getLowerBound() == config.getBottom()) {
                wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.EXTENDS;
                argName = ((QualVar<Q>)wildcard.getUpperBound()).getName();
            } else if (wildcard.getUpperBound() == config.getTop()) {
                wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.SUPER;
                argName = ((QualVar<Q>)wildcard.getLowerBound()).getName();
            } else {
                ErrorReporter.errorAbort("Unable to create string representation of typevar: " + wildcard);
                wildcardType = null; // Dead code
                argName = null; // Dead code
            }

            AnnotationParts anno = new AnnotationParts("Var");
            anno.putQuoted("param", paramName);
            anno.putQuoted("arg", argName);
            if (wildcardType != org.checkerframework.qualframework.poly.qual.Wildcard.NONE) {
                anno.put("wildcard", createWildcardString(wildcardType));
            }

            result = anno;

        } else if (wildcard.getLowerBound() instanceof GroundQual
                || wildcard.getUpperBound() instanceof GroundQual) {
            // Ground quals, bounded wildcard, unbounded wildcard

            if (wildcard.getLowerBound() instanceof GroundQual &&
                    ((GroundQual) wildcard.getLowerBound()).getQualifier() == config.getBottom()
                    && wildcard.getUpperBound() instanceof GroundQual &&
                    ((GroundQual) wildcard.getUpperBound()).getQualifier()== config.getTop()) {

                AnnotationParts anno = new AnnotationParts("Wild");
                anno.putQuoted("param", paramName);
                result = anno;

            } else {

                Q groundQual;
                org.checkerframework.qualframework.poly.qual.Wildcard wildcardType;
                if (wildcard.getLowerBound() == wildcard.getUpperBound()) {

                    wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.NONE;
                    groundQual = ((GroundQual<Q>) wildcard.getLowerBound()).getQualifier();

                } else if (wildcard.getLowerBound() instanceof GroundQual
                       && ((GroundQual) wildcard.getLowerBound()).getQualifier() == config.getBottom()) {

                    wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.EXTENDS;
                    groundQual = ((GroundQual<Q>) wildcard.getUpperBound()).getQualifier();

                } else if (wildcard.getUpperBound() instanceof GroundQual
                       && ((GroundQual) wildcard.getUpperBound()).getQualifier() == config.getTop()) {

                    wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.SUPER;
                    groundQual = ((GroundQual<Q>) wildcard.getLowerBound()).getQualifier();

                } else {
                    ErrorReporter.errorAbort("Unable to create string representation of: " + wildcard);
                    wildcardType = null; // Dead code
                    groundQual = null; // Dead code
                }

                AnnotationParts anno = config.getTargetTypeSystemAnnotation(groundQual);
                anno.putQuoted("param", paramName);
                if (wildcardType != org.checkerframework.qualframework.poly.qual.Wildcard.NONE) {
                    anno.put("wildcard", createWildcardString(wildcardType));
                }
                result = anno;
            }

        } else  {
            // Both of these are Combine PolyQuals.
            if (wildcard.getUpperBound() == wildcard.getLowerBound()) {
                result = createAnnotation(wildcard.getUpperBound(), printInvisible);
            } else {
                AnnotationParts upper = createAnnotation(wildcard.getUpperBound(), printInvisible);
                AnnotationParts lower = createAnnotation(wildcard.getLowerBound(), printInvisible);
                AnnotationParts anno = new AnnotationParts("Wild");
                anno.putQuoted("lower", lower.toString());
                anno.putQuoted("upper", upper.toString());
                result = anno;
            }
        }

        return result;
    }

    /**
     * Format a wildcard enum to include the Wildcard classname.
     */
    private String createWildcardString(org.checkerframework.qualframework.poly.qual.Wildcard wildcardType) {
        return wildcardType.getDeclaringClass().getSimpleName() + "." + wildcardType;
    }

    /**
     * Object to generate an annotation String from an Annotation name and a map of values.
     *
     * We generate annotation output for annotations that don't actually exist, so we cant use something like
     * AnnotationBuilder which requires the element of the annotation.
     *
     */
    public static class AnnotationParts {

        private String name;
        private Map<String, Object> fields = new HashMap<>();

        public AnnotationParts(String name) {
            this.name = name;
        }

        public void putQuoted(String key, String value) {
            fields.put(key, "\"" + value + "\"");
        }

        public void put(String key, String value) {
            fields.put(key, value);
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("@");
            sb.append(name);

            if (fields.size() == 1 && fields.get("value") != null) {
                sb.append("(");
                sb.append(fields.get("value"));
                sb.append(")");

            } else if (fields.size() > 0) {
                sb.append("(");
                boolean first = true;
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                }
                sb.append(")");
            }
            return sb.toString();
        }
    }
}
