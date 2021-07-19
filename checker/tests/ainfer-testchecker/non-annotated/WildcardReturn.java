// This test ensures that generated stub files handle methods that have an
// inferred wildcard return type correctly.

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class WildcardReturn {
  public Set<Object> getCredentialIdsForUsername(String username) {
    return getRegistrationsByUsername(username).stream()
        .map(registration -> registration.toString())
        .collect(Collectors.toSet());
  }

  public Collection<Object> getRegistrationsByUsername(String username) {
    return null;
  }
}
