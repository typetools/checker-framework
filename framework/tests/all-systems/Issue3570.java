@SuppressWarnings("all") // Check for crashes.
class Issue3570 {
    public interface Freezable<T extends Freezable> extends Cloneable {}

    public static final class Key<T extends Keyed> extends Iced<Key<T>> implements Comparable {
        @Override
        public int compareTo(Object o) {
            return 0;
        }
    }

    public abstract static class Keyed<T extends Keyed> extends Iced<T> {}

    public abstract static class Lockable<T extends Lockable<T>> extends Keyed<T> {}

    public abstract static class Iced<D extends Iced>
            implements Freezable<D>, java.io.Externalizable {
        @Override
        public void readExternal(java.io.ObjectInput ois)
                throws java.io.IOException, ClassNotFoundException {}

        @Override
        public void writeExternal(java.io.ObjectOutput oos) throws java.io.IOException {}
    }

    public abstract static class Model<
                    M extends Model<M, P, O>, P extends Model.Parameters, O extends Model.Output>
            extends Lockable<M> {
        public P _parms;

        public abstract static class Parameters extends Iced<Parameters> {
            public Key<Frame> _train;
        }

        public abstract static class Output extends Iced {}
    }

    public static class Frame extends Lockable<Frame> {}

    public abstract static class Schema<I extends Iced, S extends Schema<I, S>> extends Iced {}

    public static class SchemaV3<I extends Iced, S extends SchemaV3<I, S>> extends Schema<I, S> {}

    public static class KeyV3<I extends Iced, S extends KeyV3<I, S, K>, K extends Keyed>
            extends SchemaV3<I, KeyV3<I, S, K>> {
        public KeyV3(Key key) {}

        public static class FrameKeyV3 extends KeyV3<Iced, FrameKeyV3, Frame> {
            public FrameKeyV3(Key<Frame> key) {
                super(key);
            }
        }
    }

    public static class ModelSchemaBaseV3<
                    M extends Model<M, ?, ?>, S extends ModelSchemaBaseV3<M, S>>
            extends SchemaV3<M, S> {
        public KeyV3.FrameKeyV3 data_frame;

        public ModelSchemaBaseV3(M m) {
            this.data_frame = new KeyV3.FrameKeyV3(m._parms._train);
        }
    }
}
