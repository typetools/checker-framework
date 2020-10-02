import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

abstract class ComparableMapEntry<T1, T2>
        implements Comparable<ComparableMapEntry<T1, T2>>, Map.Entry<T1, T2>, Serializable {}
