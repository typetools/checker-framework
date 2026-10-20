// Test case for https://github.com/typetools/checker-framework/issues/4708

import java.text.NumberFormat;

public class Issue4708 {
  // The suppression recommended by the comment on ThreadLocal in the annotated JDK.
  @SuppressWarnings("nullness:type.argument") // initialValue returns non-null
  static final class NonNullThreadLocal extends ThreadLocal<NumberFormat> {
    @Override
    protected NumberFormat initialValue() {
      final NumberFormat format = NumberFormat.getInstance();
      format.setMaximumFractionDigits(2);
      format.setMinimumFractionDigits(2);
      return format;
    }
  }

  // :: error: [type.argument]
  static final class UnsuppressedThreadLocal extends ThreadLocal<NumberFormat> {
    @Override
    protected NumberFormat initialValue() {
      return NumberFormat.getInstance();
    }
  }
}
