package checkers.units;

import checkers.units.quals.*;

/**
 * Utility methods to generate annotated types and to convert between them.
 */
@SuppressWarnings("units")
// TODO: add fromTo methods for all useful unit combinations.
// TODO: instead or in addition to the int version, should we add long,
// float, or double versions??
public class UnitsTools {
    // Lengths
    public static @mm int toMilliMeter(int mm) { return mm; }
    public static @m int toMeter(int m) { return m; }
    public static @km int toKiloMeter(int km) { return km; }
    
    public static @m int fromMilliMeterToMeter(@mm int mm) { return mm / 1000; }
    public static @mm int fromMeterToMilliMeter(@m int m) { return m * 1000; }
    public static @km int fromMeterToKiloMeter(@m int m) { return m / 1000; }
    public static @m int fromKiloMeterToMeter(@km int km) { return km * 1000; }
    
    // Area
    public static @mm2 int toMilliMeterSquared(int mm2) { return mm2; }
    public static @m2 int toMeterSquared(int m2) { return m2; }
    public static @km2 int toKiloMeterSquared(int km2) { return km2; }
        
    // Time
    public static @s int toSecond(int s) { return s; }
    public static @min int toMinute(int min) { return min; }
    public static @h int toHour(int h) { return h; }
    
    public static @min int fromSecondToMinute(@s int s) { return s / 60; }
    public static @s int fromMinuteToSecond(@min int min) { return min * 60; }
    public static @h int fromMinuteToHour(@min int min) { return min / 60; }
    public static @min int fromHourToMinute(@h int h) { return h * 60; }
    
    // Speed
    public static @mPERs int toMeterPerSecond(int ms) { return ms; }
    public static @kmPERh int toKiloMeterPerHour(int kmh) { return kmh; }
        
    // Current
    public static @A int toAmpere(int a) { return a; }
    
    // Luminance
    public static @cd int toCandela(int cd) { return cd; }
    
    // Mass
    public static @g int toGram(int g) { return g; }
    public static @kg int toKiloGram(int kg) { return kg; }
    
    public static @kg int fromGramToKiloGram(@g int g) { return g / 1000; }
    public static @g int fromKiloGramToGram(@kg int kg) { return kg * 1000; }
    
    // Substance
    public static @mol int toMole(int mol) { return mol; }
    
    // Temperature
    public static @K int toKelvin(int k) { return k; }
    public static @C int toCelsius(int c) { return c; }
    
    public static @C int fromKelvinToCelsius(@K int k) { return k - (int)273.15; }
    public static @K int fromCelsiusToKelvin(@C int c) { return c + (int)273.15; }
   
}