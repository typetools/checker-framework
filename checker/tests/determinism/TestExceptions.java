import org.checkerframework.checker.determinism.qual.*;

public class TestExceptions {
    public int myParse(String num) {
        try {
            int n = Integer.parseInt(num);
            return n;
        } catch (NumberFormatException e) {
            System.out.println(e.toString());
            return 0;
        }
    }
}
