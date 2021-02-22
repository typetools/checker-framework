// Issue 1147 https://github.com/typetools/checker-framework/issues/1147

import java.util.StringJoiner;

public class Issue1147 {

    public static void main(String[] args) {

        StringJoiner sj = new StringJoiner(",");

        sj.add("a");

        sj.add(null);

        System.out.println(sj);
    }
}
