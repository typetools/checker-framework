import java.util.List;

public class StringValCrash {

    void foo() {
        List<String> path = null;
        System.out.print(path.size() + "...");
    }
}
