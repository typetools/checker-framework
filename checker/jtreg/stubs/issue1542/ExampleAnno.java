package issue1542;

public class ExampleAnno {
    public enum MyEnum {
        A,
        B,
        C;
    }

    @interface DoubleExample {
        double value();
    }

    @interface FloatExample {
        float value();
    }

    @interface ShortExample {
        short value();
    }

    @interface IntExample {
        int value();
    }

    @interface LongExample {
        long value();
    }

    @interface CharExample {
        char value();
    }

    @interface StringExample {
        String value();
    }

    @interface ClassExample {
        Class<?> value();
    }

    @interface MyEnumExample {
        MyEnum value();
    }

    @interface DoubleArrayExample {
        double[] value();
    }

    @interface FloatArrayExample {
        float[] value();
    }

    @interface ShortArrayExample {
        short[] value();
    }

    @interface IntArrayExample {
        int[] value();
    }

    @interface LongArrayExample {
        long[] value();
    }

    @interface CharArrayExample {
        char[] value();
    }

    @interface StringArrayExample {
        String[] value();
    }

    @interface ClassArrayExample {
        Class<?>[] value();
    }

    @interface MyEnumArrayExample {
        MyEnum[] value();
    }
}
