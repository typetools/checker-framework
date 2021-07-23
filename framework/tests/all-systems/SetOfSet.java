package wildcards;

@SuppressWarnings({
    "initialization",
    "initializedfields:contracts.postcondition"
}) // field isn't set
public class SetOfSet<E> {

    public void addAll(SetOfSet<? extends E> s) {
        E[] svalues = s.values;
    }

    protected E[] values;
}
