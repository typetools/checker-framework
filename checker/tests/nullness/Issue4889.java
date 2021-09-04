import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

class Issue4889 {
    void f(@Nullable String s) {
        Objects.toString(s, "").toString();
    }

    void g(@Nullable String s) {
        @NonNull String x = Objects.toString(s, "");
        @Nullable String y = Objects.toString(s, null);
    }
}
