// full test case for Issue 573
// a short testcase for this issue is Issue573.java in framework/tests/all-systems/java8/lambda
// https://github.com/typetools/checker-framework/issues/573
// @below-java8-jdk-skip-test

import java.time.chrono.*;

import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.TemporalAdjusters.nextOrSame;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Issue573FullTest implements Chronology {
    static final Comparator<ChronoLocalDateTime<? extends ChronoLocalDate>> DATE_TIME_ORDER =
        (Comparator<ChronoLocalDateTime<? extends ChronoLocalDate>> & Serializable) (dateTime1, dateTime2) -> {
            int cmp = Long.compare(dateTime1.toLocalDate().toEpochDay(), dateTime2.toLocalDate().toEpochDay());
            if (cmp == 0) {
                cmp = Long.compare(dateTime1.toLocalTime().toNanoOfDay(), dateTime2.toLocalTime().toNanoOfDay());
            }
            return cmp;
        };

    static final Comparator<ChronoZonedDateTime<?>> INSTANT_ORDER =
            (Comparator<ChronoZonedDateTime<?>> & Serializable) (dateTime1, dateTime2) -> {
                int cmp = Long.compare(dateTime1.toEpochSecond(), dateTime2.toEpochSecond());
                if (cmp == 0) {
                    cmp = Long.compare(dateTime1.toLocalTime().getNano(), dateTime2.toLocalTime().getNano());
                }
                return cmp;
            };

    /**
     * Map of available calendars by ID.
     */
    private static final ConcurrentHashMap<String, Chronology> CHRONOS_BY_ID = new ConcurrentHashMap<>();
    /**
     * Map of available calendars by calendar type.
     */
    private static final ConcurrentHashMap<String, Chronology> CHRONOS_BY_TYPE = new ConcurrentHashMap<>();

    /**
     * Register a Chronology by its ID and type for lookup by {@link #of(String)}.
     * Chronologies must not be registered until they are completely constructed.
     * Specifically, not in the constructor of Chronology.
     *
     * @param chrono the chronology to register; not null
     * @return the already registered Chronology if any, may be null
     */
    static Chronology registerChrono(Chronology chrono) {
        return registerChrono(chrono, chrono.getId());
    }

    /**
     * Register a Chronology by ID and type for lookup by {@link #of(String)}.
     * Chronos must not be registered until they are completely constructed.
     * Specifically, not in the constructor of Chronology.
     *
     * @param chrono the chronology to register; not null
     * @param id the ID to register the chronology; not null
     * @return the already registered Chronology if any, may be null
     */
    static Chronology registerChrono(Chronology chrono, String id) {
        Chronology prev = CHRONOS_BY_ID.putIfAbsent(id, chrono);
        if (prev == null) {
            String type = chrono.getCalendarType();
            if (type != null) {
                CHRONOS_BY_TYPE.putIfAbsent(type, chrono);
            }
        }
        return prev;
    }

    /**
     * Initialization of the maps from id and type to Chronology.
     * The ServiceLoader is used to find and register any implementations
     * of {@link java.time.chrono.Issue573FullTest} found in the bootclass loader.
     * The built-in chronologies are registered explicitly.
     * Calendars configured via the Thread's context classloader are local
     * to that thread and are ignored.
     * <p>
     * The initialization is done only once using the registration
     * of the IsoChronology as the test and the final step.
     * Multiple threads may perform the initialization concurrently.
     * Only the first registration of each Chronology is retained by the
     * ConcurrentHashMap.
     * @return true if the cache was initialized
     */
    private static boolean initCache() {
        if (CHRONOS_BY_ID.get("ISO") == null) {
            // Initialization is incomplete

            // Register built-in Chronologies
            registerChrono(HijrahChronology.INSTANCE);
            registerChrono(JapaneseChronology.INSTANCE);
            registerChrono(MinguoChronology.INSTANCE);
            registerChrono(ThaiBuddhistChronology.INSTANCE);

            // Register Chronologies from the ServiceLoader
            @SuppressWarnings("rawtypes")
            ServiceLoader<Issue573FullTest> loader =  ServiceLoader.load(Issue573FullTest.class, null);
            for (Issue573FullTest chrono : loader) {
                String id = chrono.getId();
                if (id.equals("ISO") || registerChrono(chrono) != null) {
                    // Log the attempt to replace an existing Chronology
                    System.err.println("Ignoring duplicate Chronology, from ServiceLoader configuration "  + id);
                }
            }

            // finally, register IsoChronology to mark initialization is complete
            registerChrono(IsoChronology.INSTANCE);
            return true;
        }
        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Chronology} from a chronology ID or
     * calendar system type.
     * <p>
     * See {@link Chronology#of(String)}.
     *
     * @param id  the chronology ID or calendar system type, not null
     * @return the chronology with the identifier requested, not null
     * @throws java.time.DateTimeException if the chronology cannot be found
     */
    static Chronology of(String id) {
        Objects.requireNonNull(id, "id");
        do {
            Chronology chrono = of0(id);
            if (chrono != null) {
                return chrono;
            }
            // If not found, do the initialization (once) and repeat the lookup
        } while (initCache());

        // Look for a Chronology using ServiceLoader of the Thread's ContextClassLoader
        // Application provided Chronologies must not be cached
        @SuppressWarnings("rawtypes")
        ServiceLoader<Chronology> loader = ServiceLoader.load(Chronology.class);
        for (Chronology chrono : loader) {
            if (id.equals(chrono.getId()) || id.equals(chrono.getCalendarType())) {
                return chrono;
            }
        }
        throw new DateTimeException("Unknown chronology: " + id);
    }

    /**
     * Obtains an instance of {@code Chronology} from a chronology ID or
     * calendar system type.
     *
     * @param id  the chronology ID or calendar system type, not null
     * @return the chronology with the identifier requested, or {@code null} if not found
     */
    private static Chronology of0(String id) {
        Chronology chrono = CHRONOS_BY_ID.get(id);
        if (chrono == null) {
            chrono = CHRONOS_BY_TYPE.get(id);
        }
        return chrono;
    }

    /**
     * Returns the available chronologies.
     * <p>
     * Each returned {@code Chronology} is available for use in the system.
     * The set of chronologies includes the system chronologies and
     * any chronologies provided by the application via ServiceLoader
     * configuration.
     *
     * @return the independent, modifiable set of the available chronology IDs, not null
     */
    static Set<Chronology> getAvailableChronologies() {
        initCache();       // force initialization
        HashSet<Chronology> chronos = new HashSet<>(CHRONOS_BY_ID.values());

        /// Add in Chronologies from the ServiceLoader configuration
        @SuppressWarnings("rawtypes")
        ServiceLoader<Chronology> loader = ServiceLoader.load(Chronology.class);
        for (Chronology chrono : loader) {
            chronos.add(chrono);
        }
        return chronos;
    }

    /**
     * Adds a field-value pair to the map, checking for conflicts.
     * <p>
     * If the field is not already present, then the field-value pair is added to the map.
     * If the field is already present and it has the same value as that specified, no action occurs.
     * If the field is already present and it has a different value to that specified, then
     * an exception is thrown.
     *
     * @param field  the field to add, not null
     * @param value  the value to add, not null
     * @throws java.time.DateTimeException if the field is already present with a different value
     */
    void addFieldValue(Map<TemporalField, Long> fieldValues, ChronoField field, long value) {
        Long old = fieldValues.get(field);  // check first for better error message
        if (old != null && old.longValue() != value) {
            throw new DateTimeException("Conflict found: " + field + " " + old + " differs from " + field + " " + value);
        }
        fieldValues.put(field, value);
    }
}

