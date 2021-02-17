// @skip-test until we bring list support back

public class ListAddInfiniteLoop {

    void ListLoop(List<Integer> list) {
        while (true) {
            list.add(4);
        }
    }
}
