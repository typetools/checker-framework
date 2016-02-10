// @skip-test

// Test case for issue #577: https://github.com/typetools/checker-framework/issues/577
class Banana extends Apple<int[]> {
    class InnerBanana extends InnerApple {
        @Override
        void foo(int[] array) {
        }
    }
}

class Apple<T> {
    class InnerApple {
        void foo(T param) {
        }
    }
}