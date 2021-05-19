import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

final class ZookeeperTernaryCrash {

  private static List<SubjectName> getSubjectAltNames(final X509Certificate cert) {
    try {
      final Collection<List<?>> entries = cert.getSubjectAlternativeNames();
      if (entries == null) {
        return Collections.emptyList();
      }
      final List<SubjectName> result = new ArrayList<SubjectName>();
      for (List<?> entry : entries) {
        final Integer type = entry.size() >= 2 ? (Integer) entry.get(0) : null;
        if (type != null) {
          if (type == SubjectName.DNS || type == SubjectName.IP) {
            final Object o = entry.get(1);
            if (o instanceof String) {
              result.add(new SubjectName((String) o, type));
            } else if (o instanceof byte[]) {
              // TODO ASN.1 DER encoded form
            }
          }
        }
      }
      return result;
    } catch (final CertificateParsingException ignore) {
      return Collections.emptyList();
    }
  }

  private static final class SubjectName {

    static final int DNS = 2;
    static final int IP = 7;

    private final String value;
    private final int type;

    static SubjectName IP(final String value) {
      return new SubjectName(value, IP);
    }

    static SubjectName DNS(final String value) {
      return new SubjectName(value, DNS);
    }

    SubjectName(final String value, final int type) {
      if (type != DNS && type != IP) {
        throw new IllegalArgumentException("Invalid type: " + type);
      }
      this.value = Objects.requireNonNull(value);
      this.type = type;
    }

    public int getType() {
      return type;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
