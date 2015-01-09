package org.checkerframework.qualframework.poly.format;

import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.PolyQual.Combined;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.Wildcard;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Need to make the names @Wild @Var customizable?
 */
public abstract class SurfaceSyntaxQualParamsFormatter<Q> implements QualParamsFormatter<Q> {

    // Determines if invisible qualifiers will be printed.
    protected final boolean printInvisibleQualifiers;

    public SurfaceSyntaxQualParamsFormatter(
            boolean printInvisibleQualifiers) {

        this.printInvisibleQualifiers = printInvisibleQualifiers;
    }

    protected abstract boolean shouldPrintAnnotation(AnnotationParts anno);

    protected abstract AnnotationParts getTargetTypeSystemAnnotation(Q q);

    protected abstract Q getBottom();

    protected abstract Q getTop();

    protected abstract QualParams<Q> getQualTop();

    protected abstract QualParams<Q> getQualBottom();

    @Override
    public String format(QualParams<Q> params) {
        return format(params, true);
    }

    @Override
    public String format(QualParams<Q> params, boolean printPrimary) {
        StringBuffer sb = new StringBuffer();

        if (params == getQualTop()) {
            return getTargetTypeSystemAnnotation(getTop()).toString();
        } else if (params == getQualBottom()) {
            return getTargetTypeSystemAnnotation(getBottom()).toString();
        }

        boolean printedPrimary = false;
        if (printPrimary && params.getPrimary() != null) {
            AnnotationParts anno = createAnnotation(params.getPrimary());
            if (shouldPrintAnnotation(anno)) {
                printedPrimary = true;
                sb.append(anno.toString());
            }
        }

        boolean addSpace = printedPrimary;
        for (Entry<String, Wildcard<Q>> entry : params.entrySet()) {

            AnnotationParts anno = createAnnotation(entry.getValue(), entry.getKey());
            if (anno != null && shouldPrintAnnotation(anno)) {
                if (addSpace) {
                    sb.append(" ");
                } else {
                    addSpace = true;
                }
                sb.append(anno.toString());
            }
        }

        return sb.toString();
    }

    public String format(PolyQual<Q> polyQual) {
        AnnotationParts anno = createAnnotation(polyQual);
        if (anno != null && shouldPrintAnnotation(anno)) {
            return anno.toString();
        } else {
            return "";
        }
    }

    // TODO: Shared code here with Wildcard
    private AnnotationParts createAnnotation(PolyQual<Q> polyQual) {

        if (polyQual == null) {
            return null;
        } else if (polyQual instanceof Combined) {
            AnnotationParts anno = new AnnotationParts("Combine");
            Combined<Q> combined = (Combined<Q>) polyQual;
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for (QualVar<Q> var : combined.getVars()) {
                sb.append(format(var));
                sb.append(", ");
            }
            sb.append(getTargetTypeSystemAnnotation(combined.getGround()).toString());
            sb.append(")");
            anno.putQuoted(combined.getOp().toString(), sb.toString());
            return anno;

        } else if (polyQual instanceof GroundQual) {
            AnnotationParts anno = getTargetTypeSystemAnnotation(((GroundQual<Q>)polyQual).getQualifier());
            return anno;

        } else if (polyQual instanceof QualVar) {
            QualVar<Q> qualVar = (QualVar<Q>) polyQual;

            if(qualVar.getLowerBound() != getBottom()
                    || qualVar.getUpperBound() != qualVar.getUpperBound()) {

                ErrorReporter.errorAbort("Did not expect a poly qual with non-trivial bounds:" + polyQual);
            }
            AnnotationParts anno = new AnnotationParts("Var");
            anno.putQuoted("arg", qualVar.getName());
            return anno;

        } else {
            ErrorReporter.errorAbort("Unknown PolyQual Subclass:" + polyQual.getClass());
            return null;
        }
    }

    private AnnotationParts createAnnotation(Wildcard<Q> wildcard, String paramName) {

        if (wildcard.isEmpty()) {
            ErrorReporter.errorAbort("Unable to convert wildcard: " + wildcard);
        }

        AnnotationParts result;
        if (wildcard.getLowerBound() instanceof QualVar
                || wildcard.getUpperBound() instanceof QualVar) {

            org.checkerframework.qualframework.poly.qual.Wildcard wildcardType;
            String argName;

            if (wildcard.getLowerBound() == wildcard.getUpperBound()) {
                wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.NONE;
                argName = ((QualVar<Q>)wildcard.getLowerBound()).getName();
            } else if (wildcard.getLowerBound() == getBottom()) {
                wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.EXTENDS;
                argName = ((QualVar<Q>)wildcard.getUpperBound()).getName();
            } else if (wildcard.getUpperBound() == getTop()) {
                wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.SUPER;
                argName = ((QualVar<Q>)wildcard.getLowerBound()).getName();
            } else {
                ErrorReporter.errorAbort("Unable to convert typevar: " + wildcard);
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

            org.checkerframework.qualframework.poly.qual.Wildcard wildcardType;
            Q groundQual;

            if (wildcard.getLowerBound() instanceof GroundQual &&
                    ((GroundQual) wildcard.getLowerBound()).getQualifier() == getBottom()
                    && wildcard.getUpperBound() instanceof GroundQual &&
                    ((GroundQual) wildcard.getUpperBound()).getQualifier()== getTop()) {

                AnnotationParts anno = new AnnotationParts("Wild");
                anno.putQuoted("param", paramName);
                result = anno;

            } else {

                if (wildcard.getLowerBound() == wildcard.getUpperBound()) {
                    wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.NONE;
                    groundQual = ((GroundQual<Q>) wildcard.getLowerBound()).getQualifier();
                } else if (wildcard.getLowerBound() instanceof GroundQual &&
                        ((GroundQual) wildcard.getLowerBound()).getQualifier() == getBottom()) {
                    wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.EXTENDS;
                    groundQual = ((GroundQual<Q>) wildcard.getUpperBound()).getQualifier();
                } else if (wildcard.getUpperBound() instanceof GroundQual &&
                        ((GroundQual) wildcard.getUpperBound()).getQualifier() == getTop()) {
                    wildcardType = org.checkerframework.qualframework.poly.qual.Wildcard.SUPER;
                    groundQual = ((GroundQual<Q>) wildcard.getLowerBound()).getQualifier();
                } else {
                    ErrorReporter.errorAbort("Unable to convert wildcard: " + wildcard);
                    wildcardType = null; // Dead code
                    groundQual = null; // Dead code
                }

                AnnotationParts anno = getTargetTypeSystemAnnotation(groundQual);
                anno.putQuoted("param", paramName);
                if (wildcardType != org.checkerframework.qualframework.poly.qual.Wildcard.NONE) {
                    anno.put("wildcard", createWildcardString(wildcardType));
                }
                result = anno;
            }
        } else  {
           // TODO: Combine PolyQuals
           result = null;
        }

        return result;
    }

    private String createWildcardString(org.checkerframework.qualframework.poly.qual.Wildcard wildcardType) {
        return wildcardType.getDeclaringClass().getSimpleName() + "." + wildcardType;
    }

    public String format(Q qual) {
        ErrorReporter.errorAbort("Unexpected call to format: " + qual);
        return null; // Dead code
    }

    /**
     * Hold fields for an annotation and generate a String.
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
