package typearginfer;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Issue6891 {

  @SuppressWarnings("optional") // true positives, but this test is not about Optional
  private Map<String, String> func(Set<Entity> entities) {

    return Optional.ofNullable(entities)
        .map(t -> t.stream().collect(Collectors.toMap(Entity::getName, Entity::getId)))
        .orElse(Collections.emptyMap());
  }

  abstract class Entity {

    public abstract String getId();

    public abstract String getName();
  }
}
