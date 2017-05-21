// Test case for Issue 946
// https://github.com/typetools/checker-framework/issues/946
// @below-java8-jdk-skip-test

// @skip-test
interface Supply946<R> {
    R supply();
}

class Issue946 {
    class MethodRefInnerA {
        // this line of code causes a crash in CF
        Supply946<MethodRefInnerB> constructorReferenceField = MethodRefInnerB::new;

        MethodRefInnerA(Issue946 Issue946.this) {
            // this line of code also causes a crash in CF
            Supply946<MethodRefInnerB> constructorReference = MethodRefInnerB::new;
        }

        void M() {
            // and so does this line
            Supply946<MethodRefInnerB> constructorReference = MethodRefInnerB::new;
        }
    }

    class MethodRefInnerB {
        MethodRefInnerB(Issue946 Issue946.this) {}
    }
}
