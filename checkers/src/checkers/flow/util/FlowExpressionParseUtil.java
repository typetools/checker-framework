package checkers.flow.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.flow.cfg.node.Node;
import checkers.util.ElementUtils;
import checkers.util.TypesUtils;

/**
 * A collection of helper methods to parse a string that represents a restricted
 * Java expression. Such expressions can be found in annotations (e.g., to
 * specify a pre- or postcondition).
 * 
 * @author Stefan Heule
 * 
 */
public class FlowExpressionParseUtil {

    public static/* @Nullable */FlowExpressions.Receiver parse(String s,
            Node receiverNode, Receiver receiver) {

        Pattern identifierPattern = Pattern.compile("[a-z_$][a-z_$0-9]*");
        Matcher identifierMatcher = identifierPattern.matcher(s);

        if (identifierMatcher.matches()) {
            TypeMirror receiverType = receiverNode.getType();

            // this literal
            if (s.equals("this")) {
                return new ThisReference(receiverType);
            }

            // field of a the receiver (implicit self reference as receiver)
            TypeElement elType = TypesUtils.elementFromTypeMirror(receiverType);
            VariableElement fieldElement = ElementUtils.findFieldInType(elType,
                    s);
            return new FieldAccess(receiver, receiverType, fieldElement);
        } else {
            // TODO: real error handling
            throw new RuntimeException("Cannot parse expression '" + s + "'.");
        }
    }

}
