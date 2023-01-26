class NullReceiverTest {
  public static void testReceiver(NullReceiverTest nrt) {
    nrt.nullReceiver();
  }

  public static NullReceiverTest nullReceiver() {
    return new NullReceiverTest();
  }
}
