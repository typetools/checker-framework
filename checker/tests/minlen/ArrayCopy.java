import org.checkerframework.checker.index.qual.MinLen;

class ArrayCopy {

    void copy(int @MinLen(1) [] nums) {
        int[] nums_copy = new int[nums.length];
        System.arraycopy(nums, 0, nums_copy, 0, nums.length);
        nums = nums_copy;
    }
}
