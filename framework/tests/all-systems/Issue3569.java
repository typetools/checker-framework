public abstract class Issue3569 {
    public interface MyInterface {}

    public abstract <T extends MyInterface> T getT();

    protected <K> K getK(Issue3569 ab) {
        return ab.getT();
    }
}
