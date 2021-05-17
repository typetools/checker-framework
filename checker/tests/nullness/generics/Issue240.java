// Test case for Issue 240:
// https://github.com/typetools/checker-framework/issues/240

import org.checkerframework.checker.nullness.qual.Nullable;

class I<A> {}

// This should compile, because the implicit upper
// bound of I is "@Nullable Object"
class Use extends I<@Nullable String> {}

class I2<A extends Object> {}

// This use must be an error.
// :: error: (type.argument)
class Use2 extends I2<@Nullable String> {}
