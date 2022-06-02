// Test case for issue #58: https://tinyurl.com/cfissue/58
// This does not test what #58 wanted. See
// https://github.com/eisop/checker-framework/issues/242
// for follow-up discussions.

import org.checkerframework.checker.regex.qual.Regex;

class StringBuilderToStringPolyRegex {

    void createPattern(final @Regex(1) StringBuilder regex) {
        @Regex(1) String s = regex.toString();
    }
}
