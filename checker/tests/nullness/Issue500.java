import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue500<M> {
    public Issue500(@Nullable List<M> list) {
        if (list instanceof ArrayList<?>) {
        }
    }
    public Issue500(@Nullable AbstractList<M> list) {
        if (list instanceof ArrayList<?>) {
        }
    }
}
