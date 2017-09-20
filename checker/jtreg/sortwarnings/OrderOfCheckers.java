package index;

import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.SameLenBottom;
import org.checkerframework.checker.index.qual.SearchIndexBottom;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.common.value.qual.BottomVal;

/** This class tests that errors issued on the same tree are sorted by checker. */
public class OrderOfCheckers {
    void test(int[] y) {
        @GTENegativeOne @UpperBoundBottom @SearchIndexBottom @BottomVal int @BottomVal @SameLenBottom [] x = y;
    }
}
