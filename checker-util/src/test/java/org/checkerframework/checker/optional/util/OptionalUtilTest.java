package org.checkerframework.checker.optional.util;

import java.util.Optional;
import org.checkerframework.checker.optional.qual.Present;
import org.junit.Assert;
import org.junit.Test;

public final class OptionalUtilTest {

  @Test
  public void test_castPresent() {

    Optional<String> nonEmptyOpt = Optional.of("non-empty");
    Optional<String> emptyOpt = Optional.empty();

    Assert.assertFalse(nonEmptyOpt.isEmpty());
    @Present Optional<String> foo = OptionalUtil.castPresent(nonEmptyOpt);
    Assert.assertEquals(foo.get(), "non-empty");

    Assert.assertTrue(emptyOpt.isEmpty());
    Assert.assertThrows(Error.class, () -> OptionalUtil.castPresent(emptyOpt));
  }
}
