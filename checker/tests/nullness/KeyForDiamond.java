import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.*;

public class KeyForDiamond {
    private final Map<Integer, Double> map = new HashMap<>();

    public void method() {
        Map<@KeyFor("map") Integer, Double> paths = new HashMap<>();
        Set<@KeyFor("map") Integer> set = new HashSet<>();
        List<@KeyFor("map") Integer> list = new ArrayList<>();
    }

    public static void main(String[] args) {
        KeyForDiamond t = new KeyForDiamond();
    }
}
