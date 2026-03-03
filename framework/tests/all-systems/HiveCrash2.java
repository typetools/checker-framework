@SuppressWarnings("all") // Just check for crashes.
public class HiveCrash2 {

  protected final MqttMessageEncoder<?>[] encoders = new MqttMessageEncoder[16];

  HiveCrash2(Mqtt3ConnectEncoder connectEncoder) {
    encoders[0] = connectEncoder;
  }

  public static class Mqtt3ConnectEncoder extends MqttMessageEncoder<MqttStatefulConnect> {}

  static class MqttStatefulConnect implements MqttMessage {}

  public abstract static class MqttMessageEncoder<M extends MqttMessage> {}

  public interface MqttMessage {}
}
