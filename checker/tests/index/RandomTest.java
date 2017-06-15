import java.util.Random;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class RandomTest {
    @SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test() {
        Random rand = new Random();
        int[] a = new int[8];
        @LTLengthOf("a") double d1 = Math.random() * a.length;
        @LTLengthOf("a") int deref = (int) (Math.random() * a.length);
        @LTLengthOf("a") int deref2 = (int) (rand.nextDouble() * a.length);
        @LTLengthOf("a") int deref3 = rand.nextInt(a.length);
    }
}
