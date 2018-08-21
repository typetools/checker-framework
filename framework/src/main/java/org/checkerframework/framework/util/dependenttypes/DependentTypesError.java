package org.checkerframework.framework.util.dependenttypes;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;

// TODO: The design is gross, both because this is returned instead of thrown, and because errors
// are propagated in strings and then unparsed later.  The Checker Framework should report the
// errors earlier rather than propagating them within strings.
/**
 * Helper class for creating dependent type annotation error strings.
 *
 * <p>IMPORTANT: This is not an Exception. It is a regular class that is returned, not thrown.
 */
public class DependentTypesError {

    /** How elements of this class are formatted. */
    private static final String FORMAT_STRING = "[error for expression: %s; error: %s]";
    /** Regular expression for unparsing string representations of this class (gross). */
    private static final Pattern ERROR_PATTERN =
            Pattern.compile("\\[error for expression: (.*); error: (.*)\\]");
    /**
     * Returns whether or not the given expression string is an error. That is, whether it is a
     * string that was generate by this class.
     *
     * @param expression expression string to test
     * @return wheter or not the given expressions string is an error
     */
    public static boolean isExpressionError(String expression) {
        return expression.startsWith("[error");
    }

    /** The expression that is unparseable or otherwise problematic. */
    public final String expression;
    /** An error message about that expression. */
    public final String error;

    public DependentTypesError(String expression, String error) {
        this.expression = expression;
        this.error = error;
    }

    public DependentTypesError(String expression, FlowExpressionParseException e) {
        StringBuilder buf = new StringBuilder();
        List<Result.DiagMessage> msgs = e.getResult().getDiagMessages();

        for (Result.DiagMessage msg : msgs) {
            buf.append(msg.getArgs()[0]);
        }
        this.error = buf.toString();
        this.expression = expression;
    }

    /** Create a DependentTypesError by parsing a printed one. */
    public DependentTypesError(String error) {
        Matcher matcher = ERROR_PATTERN.matcher(error);
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
        return String.format(FORMAT_STRING, expression, error);
    }
}
