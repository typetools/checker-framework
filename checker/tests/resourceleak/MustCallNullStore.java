import java.nio.ByteBuffer;

// input exposing a bug where store after the return is null
class MustCallNullStore {

  int inflateDirect(ByteBuffer src, ByteBuffer dst) throws java.io.IOException {

    ByteBuffer presliced = dst;
    if (dst.position() > 0) {
      dst = dst.slice();
    }

    int n = 0;
    try {
      presliced.position(presliced.position() + n);
    } finally {
    }
    return n;
  }
}
