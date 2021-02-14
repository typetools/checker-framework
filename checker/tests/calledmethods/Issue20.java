// test case for issue 20: https://github.com/kelloggm/object-construction-checker/issues/20

import java.util.Map;

public class Issue20<E> {

    private boolean enableProtoAnnotations;

    @SuppressWarnings({"unchecked"})
    private <T, O extends Message, E extends ProtoElement> T getProtoExtension(
            E element, GeneratedExtension<O, T> extension) {
        // Use this method as the chokepoint for all field annotations processing, so we can
        // toggle on/off annotations processing in one place.
        if (!enableProtoAnnotations) {
            return null;
        }
        return (T) element.getOptionFields().get(extension.getDescriptor());
    }

    // stubs of relevant classes
    private class Message {}

    private class ProtoElement {
        public Map<FieldDescriptor, Object> getOptionFields() {
            return null;
        }
    }

    private class FieldDescriptor {}

    private class GeneratedExtension<O, T> {
        public FieldDescriptor getDescriptor() {
            return null;
        }
    }
}
