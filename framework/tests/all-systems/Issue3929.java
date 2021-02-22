import java.util.ArrayList;
import java.util.List;

public class Issue3929 {

    public void endElement(DefaultKeyedValues3929 arg) {
        for (Object o : arg.getKeys()) {}
    }
}

class DefaultKeyedValues3929<K extends Comparable<K>> {
    public List<K> getKeys() {
        return new ArrayList<>();
    }
}
