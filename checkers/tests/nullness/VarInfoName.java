
public abstract class VarInfoName {

    public abstract <T> T accept(Visitor<T> v);

    public abstract static class Visitor<T> {}

    public abstract static class BooleanAndVisitor
        extends Visitor<Boolean>
    {
        private boolean result;
        
        public BooleanAndVisitor(VarInfoName name) {
            result = (name.accept(this) != null);
        }
    }
}


