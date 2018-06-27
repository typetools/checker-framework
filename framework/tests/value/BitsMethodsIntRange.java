public class BitsMethodsIntRange {
    void caseInteger(int index,char val1, char val2){
        char[] arr1 = new char[33];
        char[] arr2 = new char[33];
        arr1[Integer.numberOfLeadingZeros(index)] = val1;
        arr2[Integer.numberOfTrailingZeros(index)] = val2;
    }

    void caseLong(int index, char val1, char val2) {
        char[] arr1 = new char[65];
        char[] arr2 = new char[65];
        arr1[Long.numberOfLeadingZeros(index)] = val1;
        arr2[Long.numberOfTrailingZeros(index)] = val2;
    }
}
