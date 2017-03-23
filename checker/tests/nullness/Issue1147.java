// Issue 1147 https://github.com/typetools/checker-framework/issues/1147
import java.util.StringJoiner;

class Issue1147 {

    public static void main(String[] args) {

        StringJoiner sj = new StringJoiner(",");

        sj.add("a");
        // Nullness Checker raises false positive warning
        sj.add(null);

        System.out.println(sj);
    }
}
