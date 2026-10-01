// Test case for https://github.com/typetools/checker-framework/issues/4690

import java.io.IOException;

class Issue4690 {

  enum BloomType {
    NONE,
    ROW,
    ROWCOL,
    ROWPREFIX_FIXED_LENGTH
  }

  static class Bytes {
    static int toInt(byte[] bytes) {
      return 0;
    }

    static String toStringBinary(byte[] bytes) {
      return "str";
    }
  }

  static class StoreFileWriter {
    BloomType bloomType;
    byte[] bloomParam = null;

    static boolean isTraceEnabled() {
      return true;
    }

    static void trace(String s) {}

    StoreFileWriter(BloomType bloomType, Object path) throws IOException {
      if (isTraceEnabled()) {
        trace(
            "Bloom filter type for "
                + path
                + ": "
                + this.bloomType
                + ", param: "
                + (bloomType == BloomType.ROWPREFIX_FIXED_LENGTH
                    ? Bytes.toInt(bloomParam)
                    : Bytes.toStringBinary(bloomParam))
                + ", "
                + getClass().getSimpleName());
      }
      switch (bloomType) {
        case ROW:
          break;
        case ROWCOL:
          break;
        case ROWPREFIX_FIXED_LENGTH:
          break;
        default:
          throw new IOException(
              "Invalid Bloom filter type: " + bloomType + " (ROW or ROWCOL or ROWPREFIX expected)");
      }
    }
  }
}
