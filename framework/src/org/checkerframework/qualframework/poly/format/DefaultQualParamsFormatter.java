package org.checkerframework.qualframework.poly.format;

import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.PolyQual.Combined;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.Wildcard;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

public class DefaultQualParamsFormatter<Q> implements QualParamsFormatter<Q> {

    // A list of qualifier to not be printed.
    private final List<Q> suppressedQualifiers;
    private final boolean printInvisibleQualifiers;

    public DefaultQualParamsFormatter(boolean printInvisibleQualifiers) {
        this(printInvisibleQualifiers, Collections.<Q>emptyList());
    }

    public DefaultQualParamsFormatter(boolean printInvisibleQualifiers, List<Q> suppressedQualifiers) {
        this.suppressedQualifiers = suppressedQualifiers;
        this.printInvisibleQualifiers = printInvisibleQualifiers;
    }

    public String format(QualParams<Q> params) {
        return format(params, true);
    }

    public String format(QualParams<Q> params, boolean printPrimary) {
        StringBuffer sb = new StringBuffer();

        if (printPrimary) {
            sb.append(format(params.getPrimary()));
        }

        if (params.size() > 0) {
            sb.append("《");
            boolean first = true;
            for (Entry<String, Wildcard<Q>> entry : params.entrySet()) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(entry.getKey() + "=" + format(entry.getValue()));
            }
            sb.append("》");
        }

        return sb.toString();
    }

    public String format(PolyQual<Q> polyQual) {
        if (polyQual instanceof Combined) {
            Combined<Q> combined = (Combined<Q>) polyQual;
            StringBuilder sb = new StringBuilder();
            sb.append(combined.getOp());
            sb.append("(");
            for (QualVar<Q> var : combined.getVars()) {
                sb.append(format(var));
                sb.append(", ");
            }
            sb.append(format(combined.getGround()));
            sb.append(")");
            return sb.toString();

        } else if (polyQual instanceof GroundQual) {
            return format(((GroundQual<Q>) polyQual).getQualifier());

        } else if (polyQual instanceof QualVar) {
            QualVar<Q> qualVar = (QualVar<Q>) polyQual;

            String lower = format(qualVar.getLowerBound());
            String upper = format(qualVar.getUpperBound());

            // The bounds have been supressed, so don't create range.
            if (lower.length() == 0 && upper.length() == 0) {
                return qualVar.getName();
            } else {
                return qualVar.getName()+ " ∈ [" + lower + ".." + upper + "])";
            }

        } else {
            ErrorReporter.errorAbort("Unknown PolyQual Subclass:" + polyQual.getClass());
            return "";
        }
    }

    public String format(Wildcard<Q> wildcard) {
        if (wildcard.isEmpty()) {
            return "ø";
        } else if (wildcard.getLowerBound().equals(wildcard.getUpperBound())) {
            return format(wildcard.getLowerBound());
        } else {
            String upper = format(wildcard.getUpperBound());
            String lower = format(wildcard.getLowerBound());
            if (upper.length() == 0 && lower.length() == 0) {
                return "?";
            } else {
                return "? ∈ [" + lower + ".." + upper + "]";
            }
        }
    }

    public String format(Q qual) {
        if (!printInvisibleQualifiers
          && suppressedQualifiers.contains(qual)) {

            return "";
        } else {
            return qual.toString();
        }
    }
}
