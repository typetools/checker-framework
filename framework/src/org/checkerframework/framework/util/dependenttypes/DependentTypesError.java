package org.checkerframework.framework.util.dependenttypes;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;

/** Helper class for creating dependent type annotation error strings. */
public class DependentTypesError {
    private static final String formatString = "[error for expression: %s; error: %s]";

    private static final Pattern errorPattern =
            Pattern.compile("\\[error for expression: (.*); error: (.*)\\]");

    /**
     * Returns whether or not the given expression string is an error. That is whether it is a
     * string that was generate by this class.
     *
     * @param expression expression string to test
     * @return wheter or not the given expressions string is an error
     */
    public static boolean isExpressionError(String expression) {
        return expression.startsWith("[error");
    }

    public final String expression;
    public final String error;

    public DependentTypesError(String expression, String error) {
        this.expression = expression;
        this.error = error;
    }

    public DependentTypesError(String expression, FlowExpressionParseException e) {
        StringBuffer buf = new StringBuffer();
        List<Result.DiagMessage> msgs = e.getResult().getDiagMessages();

        for (Result.DiagMessage msg : msgs) {
            buf.append(msg.getArgs()[0]);
        }
        this.error = buf.toString();
        this.expression = expression;
    }

    public DependentTypesError(String error) {
        Matcher matcher = errorPattern.matcher(error);
        if (matcher.matches()) {
            assert matcher.groupCount() == 2;
            this.expression = matcher.group(1);
            this.error = matcher.group(2);
        } else {
            this.expression = "";
            this.error = error;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DependentTypesError that = (DependentTypesError) o;

        return expression.equals(that.expression) && error.equals(that.error);
    }

    @Override
    public int hashCode() {
        int result = expression.hashCode();
        result = 31 * result + error.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format(formatString, expression, error);
    }
}
