// Earlier versions of the Checker Framework exhibited exponentially-long run time on this code due
// to the long chain of `@This` methods.

import org.checkerframework.common.returnsreceiver.qual.*;

class ReturnsReceiverPerformance {

  static class Builder {
    @This Builder m01() {
      return this;
    }

    @This Builder m02() {
      return this;
    }

    @This Builder m03() {
      return this;
    }

    @This Builder m04() {
      return this;
    }

    @This Builder m05() {
      return this;
    }

    @This Builder m06() {
      return this;
    }

    @This Builder m07() {
      return this;
    }

    @This Builder m08() {
      return this;
    }

    @This Builder m09() {
      return this;
    }

    @This Builder m10() {
      return this;
    }

    @This Builder m11() {
      return this;
    }

    @This Builder m12() {
      return this;
    }

    @This Builder m13() {
      return this;
    }

    @This Builder m14() {
      return this;
    }

    @This Builder m15() {
      return this;
    }

    @This Builder m16() {
      return this;
    }

    @This Builder m17() {
      return this;
    }

    @This Builder m18() {
      return this;
    }

    @This Builder m19() {
      return this;
    }

    @This Builder m20() {
      return this;
    }

    @This Builder m21() {
      return this;
    }

    @This Builder m22() {
      return this;
    }

    @This Builder m23() {
      return this;
    }

    @This Builder m24() {
      return this;
    }

    @This Builder m25() {
      return this;
    }

    @This Builder m26() {
      return this;
    }

    @This Builder m27() {
      return this;
    }

    @This Builder m28() {
      return this;
    }

    @This Builder m29() {
      return this;
    }

    Object build() {
      return new Object();
    }
  }

  Object go() {
    return new Builder()
        .m01()
        .m02()
        .m03()
        .m04()
        .m05()
        .m06()
        .m07()
        .m08()
        .m09()
        .m10()
        .m11()
        .m12()
        .m13()
        .m14()
        .m15()
        .m16()
        .m17()
        .m18()
        .m19()
        .m20()
        .m21()
        .m22()
        .m23()
        .m24()
        .m25()
        .m26()
        .m27()
        .m28()
        .m29()
        .build();
  }
}
