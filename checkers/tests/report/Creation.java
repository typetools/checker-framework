import checkers.util.report.quals.*;

class Creation {
    class TestOne {
        TestOne() {}

        @ReportCreation
        TestOne(int i) {}
    }

    @ReportCreation
    class TestAll{
        TestAll() {}
        TestAll(int i) {}
    }

    void test() {
        //:: error: (creation)
        new TestAll();
        //:: error: (creation)
        new TestAll(4);

        new TestOne();
        //:: error: (creation)
        new TestOne(4);
    }

    class TestSub extends TestAll {}

    void testSub() {
        //:: error: (creation)
        new TestSub();
    }
}