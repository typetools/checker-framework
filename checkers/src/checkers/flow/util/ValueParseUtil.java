package checkers.flow.util;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import checkers.flow.analysis.checkers.CFAbstractStore.FieldAccess;
import checkers.flow.analysis.checkers.CFAbstractStore.Receiver;
import checkers.flow.cfg.node.Node;
import checkers.util.ElementUtils;
import checkers.util.TypesUtils;

/**
 * A collection of helper methods to parse a string that represents a restricted
 * Java expression. Such expressions can be found in annotations (e.g., to
 * specify a precondition).
 * 
 * @author Stefan Heule
 * 
 */
public class ValueParseUtil {

    public static/* @Nullable */Receiver parse(String s, Node receiverNode, Receiver receiver) {
        System.out.println(s + " - "+receiverNode);
        if (true) { // TODO: check field syntax
            TypeMirror receiverType = receiverNode.getType();
            TypeElement elType = TypesUtils.elementFromTypeMirror(receiverType);
            VariableElement fieldElement = ElementUtils.findFieldInType(elType, s);
            return new FieldAccess(receiver, receiverType, fieldElement);
        } else {
            assert false; // TODO: error message
            return null;
        }
    }

}
