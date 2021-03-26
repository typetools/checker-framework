import java.io.Serializable;

public class Issue3032 {
  public static class PCollection<T> implements PValue {
    public <OutputT extends POutput> OutputT apply(
        String name, PTransform<? super PCollection<T>, OutputT> t) {
      throw new RuntimeException();
    }
  }

  public abstract static class PTransform<InputT extends PInput, OutputT extends POutput> {}

  interface PInput {}

  interface POutput {}

  interface PValue extends PInput, POutput {}

  static class BillingEvent {
    InvoiceGroupingKey getInvoiceGroupingKey() {
      throw new RuntimeException();
    }
  }

  public static class MapElements<InputT, OutputT>
      extends PTransform<PCollection<? extends InputT>, PCollection<OutputT>> {
    public static <InputT, OutputT> MapElements<InputT, OutputT> via(
        final ProcessFunction<InputT, OutputT> fn) {
      throw new RuntimeException();
    }
  }

  static class InvoiceGroupingKey {}

  @FunctionalInterface
  public interface ProcessFunction<InputT, OutputT> extends Serializable {
    OutputT apply(InputT input) throws Exception;
  }

  private static class GenerateInvoiceRows
      extends PTransform<PCollection<BillingEvent>, PCollection<String>> {
    public void expand(PCollection<BillingEvent> input) {
      input.apply("Map to invoicing key", MapElements.via(BillingEvent::getInvoiceGroupingKey));
    }
  }
}
