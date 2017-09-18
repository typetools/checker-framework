import org.checkerframework.common.value.qual.*;

public class Index115 {

    public static void main(String[] args) {
        if ((args.length > 1) && (args[1].equals("foo"))) {
            System.out.println("First argument is foo");
        }
    }

    public static void main2(String... args) {
        if ((args.length > 1) && (args[1].equals("foo"))) {
            System.out.println("First argument is foo");
        }
    }

    public static void main3(String @ArrayLen({1, 2}) [] args) {
        if ((args.length > 1) && (args[1].equals("foo"))) {
            System.out.println("First argument is foo");
        }
    }

    public static void main4(String @ArrayLen({1, 2}) ... args) {
        if ((args.length > 1) && (args[1].equals("foo"))) {
            System.out.println("First argument is foo");
        }
    }
}
