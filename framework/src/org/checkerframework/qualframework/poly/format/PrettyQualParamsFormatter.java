package org.checkerframework.qualframework.poly.format;

import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.PolyQual.Combined;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.Wildcard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Formats a {@link QualParams} into the double chevron 《Q》 output format.
 */
public class PrettyQualParamsFormatter<Q> implements QualParamsFormatter<Q> {

    private final Set<?> invisibleQualifiers;

    public PrettyQualParamsFormatter(Set<?> invisibleQualifiers) {
        this.invisibleQualifiers = invisibleQualifiers;
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

        StringBuilder sb = new StringBuilder();
        if (printPrimary && params.getPrimary() != null) {
            String primary = format(params.getPrimary(), printInvisible);
            if (primary != null) {
                sb.append(primary);
            }
        }

        if (params.size() > 0) {
            sb.append("《");
            boolean first = true;
            for (Entry<String, Wildcard<Q>> entry : params.entrySet()) {

                // TOP and BOTTOM of QualParams hierarchy will have null avlues
                String value = "";
                if (entry.getValue() != null) {
                    value = format(entry.getValue(), printInvisible);
                    if (value == null) {
                        value = "";
                    }
                }

                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(entry.getKey());
                sb.append("=");

                sb.append(value);
            }
            sb.append("》");
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return null;
        }
    }

    @Override
    public String format(PolyQual<Q> polyQual, boolean printInvisible) {

        if (polyQual == null) {
            return null;

        } else if (polyQual instanceof Combined) {
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
            sb.append(combined.getOp());
            sb.append("(");

            boolean first = true;
            for (String formattedVar : formattedVars) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(formattedVar);
            }

            sb.append(")");
            return sb.toString();

        } else if (polyQual instanceof GroundQual) {
            return formatQual(((GroundQual<Q>) polyQual).getQualifier(), printInvisible);

        } else if (polyQual instanceof QualVar) {
            QualVar<Q> qualVar = (QualVar<Q>) polyQual;

            String lower = formatQual(qualVar.getLowerBound(), printInvisible);
            String upper = formatQual(qualVar.getUpperBound(), printInvisible);

            // The bounds have been suppressed, so don't create an output range.
            if (lower == null && upper == null) {
                return qualVar.getName();
            } else {
                lower = lower == null? "" : lower;
                upper = upper == null? "" : upper;
                return qualVar.getName()+ " ∈ [" + lower + ".." + upper + "])";
            }

        } else {
            ErrorReporter.errorAbort("Unknown PolyQual Subclass: " + polyQual);
            return ""; // Dead code
        }
    }

    private String format(Wildcard<Q> wildcard, boolean printInvisible) {
        if (wildcard.isEmpty()) {
            return "ø";

        } else if (wildcard.getLowerBound().equals(wildcard.getUpperBound())) {
            return format(wildcard.getLowerBound(), printInvisible);

        } else {
            String upper = format(wildcard.getUpperBound(), printInvisible);
            String lower = format(wildcard.getLowerBound(), printInvisible);
            if (upper == null && lower == null) {
                return "?";
            } else {
                lower = lower == null? "" : lower;
                upper = upper == null? "" : upper;
                return "? ∈ [" + lower + ".." + upper + "]";
            }
        }
    }

    private String formatQual(Q qual, boolean printInvisible) {
        if (printInvisible || !invisibleQualifiers.contains(qual)) {
            return "@" + qual.toString();
        } else {
            return null;
        }
    }
}
