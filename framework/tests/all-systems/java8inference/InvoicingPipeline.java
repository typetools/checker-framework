import java.io.Serializable;

public class InvoicingPipeline {

    public static class PCollection<T> implements PValue {

        public <OutputT extends POutput> OutputT apply(
                String name, PTransform<? super PCollection<T>, OutputT> t) {
            return null;
        }
    }

    public abstract static class PTransform<InputT extends PInput, OutputT extends POutput> {
        public abstract void expand(InputT input);
    }

    static interface PInput {}

    static interface POutput {}

    interface PValue extends PInput, POutput {}

    static class BillingEvent {

        InvoiceGroupingKey getInvoiceGroupingKey() {
            return null;
        }
    }

    public static class MapElements<InputT, OutputT>
            extends PTransform<PCollection<? extends InputT>, PCollection<OutputT>> {
        public static <OutputT> MapElements<?, OutputT> into(
                final TypeDescriptor<OutputT> outputType) {
            return null;
        }

        public static <InputT, OutputT> MapElements<InputT, OutputT> via(
                final ProcessFunction<InputT, OutputT> fn) {
            return null;
        }

        public void expand(PCollection<? extends InputT> input) {}
    }

    static class TypeDescriptor<T> {
        public static <T> TypeDescriptor<T> of(Class<T> type) {
            return null;
        }
    }

    static class InvoiceGroupingKey {}

    @FunctionalInterface
    public interface ProcessFunction<InputT, OutputT> extends Serializable {
        OutputT apply(InputT input) throws Exception;
    }

    private static class GenerateInvoiceRows
            extends PTransform<PCollection<BillingEvent>, PCollection<String>> {
        @Override
        public void expand(PCollection<BillingEvent> input) {
            input.apply(
                    "Map to invoicing key",
                    MapElements.into(TypeDescriptor.of(InvoiceGroupingKey.class))
                            .via(BillingEvent::getInvoiceGroupingKey));
        }
    }
}
