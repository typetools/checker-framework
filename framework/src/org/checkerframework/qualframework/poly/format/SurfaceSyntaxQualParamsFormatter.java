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
            if (config.shouldPrintAnnotation(anno, printInvisible)) {
                return anno.toString();
            } else {
                return null;
            }
        } else if (params == config.getQualBottom()) {
            AnnotationParts anno = config.getTargetTypeSystemAnnotation(config.getBottom());
            if (config.shouldPrintAnnotation(anno, printInvisible)) {
                return anno.toString();
            } else {
                return null;
            }
        }

        // Primary
        boolean printedPrimary = false;
        if (printPrimary && params.getPrimary() != null) {
            List<AnnotationParts> annos = createAnnotations(params.getPrimary(), printInvisible);
            for (AnnotationParts anno : annos) {
                if (printedPrimary) {
                    sb.append(" ");
                } else {
                    printedPrimary = true;
                }
                sb.append(anno.toString());
            }
        }

        // Qualifier Parameters
        boolean addSpace = printedPrimary;
        for (Entry<String, Wildcard<Q>> entry : params.entrySet()) {
            List<AnnotationParts> annos = createAnnotations(entry.getValue(), entry.getKey(), printInvisible);
            for (AnnotationParts anno : annos) {
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
        StringBuffer sb = new StringBuffer();
        List<AnnotationParts> annos = createAnnotations(polyQual, printInvisible);
        boolean first = false;
        for (AnnotationParts anno : annos) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(anno);
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return null;
        }
    }

    /**
     * Transform a wildcard into a List of AnnotationParts.
     *
     * This method uses {@link #createAnnotations(org.checkerframework.qualframework.poly.PolyQual, boolean)} to create
     * the annotation parts for each bounds.
     *
     * @param wildcard the Wildcard
     * @param paramName the name of the qualifier parameter the wildcard was a value for
     * @param printInvisible flag to enable printing invisible qualifiers
     * @return a List of AnnotationParts that correspond wildcard
     */
    private List<AnnotationParts> createAnnotations(Wildcard<Q> wildcard, String paramName, boolean printInvisible) {
        if (wildcard.isEmpty()) {
            ErrorReporter.errorAbort("Unable to convert wildcard: " + wildcard);
        }

        List<AnnotationParts> results = new ArrayList<>();

        List<AnnotationParts> upper = createAnnotations(wildcard.getUpperBound(), printInvisible);
        Map<AnnotationParts, org.checkerframework.qualframework.poly.qual.Wildcard> bounds =
                new HashMap<>();

        for (AnnotationParts part: upper) {
            part.putQuoted("param", paramName);
            bounds.put(part, org.checkerframework.qualframework.poly.qual.Wildcard.EXTENDS);
        }
        results.addAll(upper);

        List<AnnotationParts> lower = createAnnotations(wildcard.getLowerBound(), printInvisible);
        for (AnnotationParts part: lower) {
            part.putQuoted("param", paramName);
            // If we have both an Upper and Lower entry for the annotation, we can omit the wildcard
            if (upper.contains(part)) {
                bounds.remove(part);
            } else {
                bounds.put(part, org.checkerframework.qualframework.poly.qual.Wildcard.SUPER);
                results.add(part);
            }
        }

        List<AnnotationParts> filteredResults = new ArrayList<>();
        for (AnnotationParts anno : results) {
            if (bounds.containsKey(anno)) {
                anno.put("wildcard", createWildcardString(bounds.get(anno)));
            }

            if (config.shouldPrintAnnotation(anno, printInvisible)) {
                filteredResults.add(anno);
            }
        }

        return filteredResults;
    }

    /**
     * Create a List of AnnotationParts that correspond to a polyQual.
     *
     * Instances of Combined may result in multiple AnnotationsParts because Combined PolyQuals are created
     * when multiple annotations are present on a type.
     *
     * @param polyQual the PolyQual
     * @param printInvisible flag to enable printing invisible qualifiers
     * @return a List of AnnotationParts corresponding to PolyQual
     */
    private List<AnnotationParts> createAnnotations(PolyQual<Q> polyQual, boolean printInvisible) {

        List<AnnotationParts> result = new ArrayList<>();

        if (polyQual == null) {
            return result;

        } else if (polyQual instanceof Combined) {

            Combined<Q> combined = (Combined<Q>) polyQual;
            for (QualVar<Q> var : combined.getVars()) {
                List<AnnotationParts> anno = createAnnotations(var, printInvisible);
                result.addAll(anno);
            }

            AnnotationParts anno = config.getTargetTypeSystemAnnotation(combined.getGround());
            if (anno != null) {
                result.add(anno);
            }

        } else if (polyQual instanceof GroundQual) {
            AnnotationParts anno = config.getTargetTypeSystemAnnotation(((GroundQual<Q>) polyQual).getQualifier());
            if (anno != null) {
                result.add(anno);
            }

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
                anno.putQuoted("range", " ∈ [" + lower + ".." + upper + "]");
            }
            result.add(anno);

        } else {

            ErrorReporter.errorAbort("Unknown PolyQual Subclass: " + polyQual.getClass());
            return result; // Dead code
        }

        List<AnnotationParts> filteredResults = new ArrayList<>();
        for (AnnotationParts anno : result) {
            if (config.shouldPrintAnnotation(anno, printInvisible)) {
                filteredResults.add(anno);
            }
        }
        return filteredResults;
    }

    private String formatQual(Q qual, boolean printInvisible) {
        AnnotationParts anno = config.getTargetTypeSystemAnnotation(qual);
        if (anno != null && config.shouldPrintAnnotation(anno, printInvisible)) {
            return anno.toString();
        } else {
            return null;
        }
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AnnotationParts that = (AnnotationParts) o;

            if (!fields.equals(that.fields)) return false;
            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + fields.hashCode();
            return result;
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
