import org.checkerframework.common.value.qual.IntRange;

class UsesIntRange {
    void do_things() {
        @IntRange(from = 3, to = 20000) int x = NeedsIntRange.range(true);
        @IntRange(from = 3L, to = 20000) int x = NeedsIntRange.range(true);
    }
}
