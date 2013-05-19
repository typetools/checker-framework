package checkers.javari;

import javax.lang.model.element.*;
import checkers.types.AnnotatedClassType;

/**
 * Represents the state of the visitor: mutability state of current
 * method, current class, and whether errors and warnings should be
 * reported.
 */
class VisitorState {

    static enum State {
        READONLY, MUTABLE, THIS_MUTABLE, RO_MAYBE;

        /**
         * Determines whether the state expression could be assigned
         * to the state variable.
         *
         * @param variable a State
         * @param expression a State
         * @return true if expression has at least as much mutability
         * as variable, false otherwise.
         */
        public static final boolean assignableTo(State variable,
                                                 State expression) {
            if (variable.equals(expression)) return true;
            if (expression.equals(READONLY) || variable.equals(MUTABLE))
                return false;
            return true;
        }
    }

    State state, classState;
    boolean constructor = false;
    AnnotatedClassType annotatedClassType;

    VisitorState(State state) {
        this.state = state;
        classState = state;
    }

    void setClassState(State s) {
        state = s;
        classState = s;
    }

}
