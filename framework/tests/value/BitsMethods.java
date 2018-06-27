public class BitsMethods {
        void caseInteger(int index,char val1, char val2){
            char[] arr1 = new char[32];
            char[] arr2 = new char[32];
            arr1[Integer.numberOfLeadingZeros(index)] = val1;
            arr2[Integer.numberOfTrailingZeros(index)] = val2;
        }

        void caseLong(int index, char val1, char val2) {
            char[] arr1 = new char[64];
            char[] arr2 = new char[64];
            arr1[Long.numberOfLeadingZeros(index)] = val1;
            arr2[Long.numberOfTrailingZeros(index)] = val2;
        }
}
