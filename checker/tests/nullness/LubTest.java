import org.checkerframework.checker.nullness.qual.*;

public class LubTest {

    @Nullable String str;

    public void setStr(@Nullable String text) {
        str = text;
    }

    public @Nullable String getStr() {
        return str;
    }

    public void ok(@Nullable LubTest t) {
        if (t == null) {
            this.setStr("");
        } else {
            this.setStr(t.getStr());
        }
    }

    public void notok(@Nullable LubTest t) {
        this.setStr((t == null) ? "" : t.getStr());
    }
}
