public class TypeVarsArrays<T> {
    private T[] array;

    public void triggerBug(int index, T val) {
        array[index] = val;
        array[index] = null;
    }
}
