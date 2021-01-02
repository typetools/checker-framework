import org.checkerframework.checker.signedness.qual.*;

public class Cast {

    static final Object object = 1;

    void client() {
        objectiveParameter(object);
    }

    void objectiveParameter(Object object) {
        // :: error: (argument.type.incompatible)
        integralParameter((Integer) object);
    }

    // This passes when object is initialized within objectiveArgument().
    void objectiveArgument() {
        Object object = -3;
        integralParameter((Integer) object);
    }

    void integralParameter(int x) {}
}
