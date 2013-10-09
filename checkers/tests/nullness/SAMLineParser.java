// @skip-test Crashes the Checker Framework, but skipped to avoid breaking the build

public class SAMLineParser {

    private int x;

    private String makeErrorString() {
        return ""
            + (this.x <= 0 ? "" : this.x);
    }

}
