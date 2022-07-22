public abstract class MutableList<T> extends List<T> {
    @Override
    abstract void retainAll(List<?> other);
}
