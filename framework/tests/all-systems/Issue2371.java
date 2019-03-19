// @skip-test
public class Issue2371<T extends IssueNew<T>> {
    void method(Issue2371<? extends Object> i) {
        other(i);
    }

    void other(Issue2371<?> e) {}
}
