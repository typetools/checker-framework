// Test case for Issue 2739.
// https://github.com/typetools/checker-framework/issues/2739
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class EnumToAdd<E extends Enum<E> & EnumToAdd.EppEnum>
        extends XmlAdapter<EnumToAdd.EnumShim, E> {
    public interface EppEnum {
        String getXmlName();
    }

    static class EnumShim {
        @XmlAttribute String s;
    }

    @Override
    public E unmarshal(EnumShim shim) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final EnumShim marshal(E enumeration) {
        EnumShim shim = new EnumShim();
        shim.s = enumeration.getXmlName();
        return shim;
    }
}
