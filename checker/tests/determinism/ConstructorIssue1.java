import org.checkerframework.checker.propkey.qual.*;

class ConstructorIssue1 {
    @PropertyKey ConstructorIssue1() {}
}

class ConstructorIssue1Client {
    void test() {
        @UnknownPropertyKey ConstructorIssue1 propObj2 = new @UnknownPropertyKey ConstructorIssue1();
    }
}
