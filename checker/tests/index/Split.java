// @skip-test TODO: reinstate before merge

public class Split {
    Pattern p = Pattern.compile(".*");

    void test() {
        @MinLen(1) String[] s = p.split("sdf");
    }
}
