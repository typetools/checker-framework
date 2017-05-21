// @skip-test until we bring list support back

class ListAddInfiniteLoop {

    void ListLoop(List<Integer> list) {
        while (true) {
            list.add(4);
        }
    }
}
