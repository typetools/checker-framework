import java.util.List;

class Delta<E> {
    List<E> field;

    Delta(List<E> field) {
        this.field = ImmutableList.copyOf(field);
    }
}
