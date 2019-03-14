import java.util.*;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class KeyForDiamond {
    private final Map<Integer, Double> map = new HashMap<>();

    public void method() {
        Map<@KeyFor("map") Integer, Double> paths1; // does not compile
        Map<@KeyFor("map") Integer, Double> paths = new HashMap<>(); // does not compile
        Set<@KeyFor("map") Integer> set = new HashSet<>(); // compiles
        List<@KeyFor("map") Integer> list = new ArrayList<>(); // does not compile
    }

    public static void main(String[] args) {
        KeyForDiamond t = new KeyForDiamond();
    }
}
