package inference.guava;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("") // Just check for crashes.
public class Bug1<B> {
    @SuppressWarnings("type.inference.not.same")
    public void method1(Map<? extends Class<? extends B>, ? extends B> map) {
        Map<Class<? extends B>, B> copy = new LinkedHashMap<>(map);
        for (Entry<? extends Class<? extends B>, B> entry : copy.entrySet()) {
            cast(entry.getKey(), entry.getValue());
        }
    }

    private static <X, T extends X> T cast(Class<T> type, X value) {
        throw new RuntimeException();
    }
}
