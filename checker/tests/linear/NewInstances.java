import org.checkerframework.checker.linear.qual.*;

public class NewInstances {

    public static void main(String[] args) {
        // assigning a literal to a @Linear type is valid
        @Linear String s = "linear";

        // assigning a new instance to a @Linear type is valid
        @Linear Object o = new Object();

        System.out.println(s);
        System.out.println(o);
    }
}
