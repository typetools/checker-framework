import java.util.ArrayList;
import java.util.List;

public class EnumStaticBlock {
    public enum Section {
        ME,
        OTHER;
        private static final List<Integer> l = new ArrayList<>();

        static {
            for (int i = 0; i < 10; ++i) {
                l.add(i);
            }
        }
    }
}
