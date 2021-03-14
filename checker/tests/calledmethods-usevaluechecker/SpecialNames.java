// This test ensures that special handling for method names in DescribeImagesRequest isn't used for
// other classes with the same names.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

public class SpecialNames {
  @This SpecialNames withFilters() {
    return this;
  }

  void setFilters() {}

  @This SpecialNames withFilters(SpecialNames f) {
    return this;
  }

  void setFilters(SpecialNames f) {}

  @This SpecialNames withName() {
    return this;
  }

  @This SpecialNames withName(String f) {
    return this;
  }

  SpecialNames() {}

  SpecialNames(String x) {}

  static void test(SpecialNames s) {
    // :: error: assignment.type.incompatible
    @CalledMethods("withOwners") SpecialNames x = s.withFilters(new SpecialNames().withName("owner"));
  }

  static void test2(SpecialNames s) {
    s.setFilters(new SpecialNames("owner"));
    // :: error: assignment.type.incompatible
    @CalledMethods("withOwners") SpecialNames x = s;
  }

  static void test3(SpecialNames s) {
    // :: error: assignment.type.incompatible
    @CalledMethods("withOwners") SpecialNames x = s.withFilters(new SpecialNames().withName("owner"));
  }

  static void test4(SpecialNames s) {
    s.setFilters(new SpecialNames("owner"));
    // :: error: assignment.type.incompatible
    @CalledMethods("withOwners") SpecialNames x = s;
  }

  static void testForCrashes(SpecialNames s) {
    s.setFilters();
    s.withFilters();

    s.setFilters(new SpecialNames().withName());
  }
}
