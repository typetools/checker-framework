package checkers.flow.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.source.Result;
import checkers.util.ElementUtils;

/**
 * A collection of helper methods to parse a string that represents a restricted
 * Java expression. Such expressions can be found in annotations (e.g., to
 * specify a pre- or postcondition).
 * 
 * @author Stefan Heule
 * 
 */
public class FlowExpressionParseUtil {

    /**
     * An exception that indicates a parse error. It contains a {@link Result}
     * that can be used for error reporting.
     */
    public static class FlowExpressionParseException extends Exception {
        private static final long serialVersionUID = 1L;

        protected final Result result;

        public FlowExpressionParseException(Result result) {
            this.result = result;
        }

        public Result getResult() {
            return result;
        }
    }

    /**
     * Parse a string and return its representation as a
     * {@link FlowExpression.Receiver}, or throw an
     * {@link FlowExpressionParseException}. The expression is assumed to be
     * used in the context of a method.
     * 
     * @param s
     *            The string to parse.
     * @param receiverType
     *            The type of the receiver that this expression might refer to.
     * @param receiver
     *            The receiver to be used in the result (if it occurs).
     * @param arguments
     *            The arguments of the method.
     * @throws FlowExpressionParseException
     */
    public static/* @Nullable */FlowExpressions.Receiver parse(String s,
            TypeMirror receiverType, Receiver receiver, List<Receiver> arguments)
            throws FlowExpressionParseException {

        Matcher identifierMatcher = Pattern.compile("[a-z_$][a-z_$0-9]*")
                .matcher(s);
        Matcher selfMatcher = Pattern.compile("this|#0").matcher(s);
        Matcher parameterMatcher = Pattern.compile("#([1-9]+[0-9]*)")
                .matcher(s);

        if (identifierMatcher.matches()) {

            // this literal
            if (selfMatcher.matches()) {
                return new ThisReference(receiverType);
            }

            // field of a the receiver (implicit self reference as receiver)
            assert receiverType instanceof DeclaredType;
            Element el = ((DeclaredType) receiverType).asElement();
            assert el instanceof TypeElement;
            TypeElement elType = (TypeElement) el;
            VariableElement fieldElement = ElementUtils.findFieldInType(elType,
                    s);
            return new FieldAccess(receiver, receiverType, fieldElement);

        } else if (parameterMatcher.matches()) {
            // parameter syntax
            int idx = -1;
            try {
                idx = Integer.parseInt(parameterMatcher.group(1));
            } catch (NumberFormatException e) {
                // cannot occur by the way the pattern is defined (matches only
                // numbers)
            }
            if (idx > arguments.size()) {
                throw new FlowExpressionParseException(Result.failure(
                        "flowexpr.parse.index.too.big", Integer.toString(idx)));
            }
            return arguments.get(idx - 1);
        } else {
            throw new FlowExpressionParseException(Result.failure(
                    "flowexpr.parse.error", s));
        }
    }
}
