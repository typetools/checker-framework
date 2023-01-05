package org.checkerframework.checker.units.qual;

/**
 * SI prefixes.
 *
 * <p>From <a
 * href="https://en.wikipedia.org/wiki/SI_prefix">http://en.wikipedia.org/wiki/SI_prefix</a>:
 *
 * <pre>
 * yotta   Y   1000^8     10^24    1000000000000000000000000   Septillion      Quadrillion     1991
 * zetta   Z   1000^7     10^21    1000000000000000000000      Sextillion      Trilliard       1991
 * exa     E   1000^6     10^18    1000000000000000000         Quintillion     Trillion        1975
 * peta    P   1000^5     10^15    1000000000000000            Quadrillion     Billiard        1975
 * tera    T   1000^4     10^12    1000000000000               Trillion        Billion         1960
 * giga    G   1000^3     10^9     1000000000                  Billion         Milliard        1960
 * mega    M   1000^2     10^6     1000000                     Million                         1960
 * kilo    k   1000^1     10^3     1000                        Thousand                        1795
 * hecto   h   1000^2/3   10^2     100                         Hundred                         1795
 * deca    da  1000^1/3   10^1     10                          Ten                             1795
 *             1000^0     10^0     1                           One
 * deci    d   1000^-1/3  10^-1    0.1                         Tenth                           1795
 * centi   c   1000^-2/3  10^-2    0.01                        Hundredth                       1795
 * milli   m   1000^-1    10^-3    0.001                       Thousandth                      1795
 * micro   my  1000^-2    10^-6    0.000001                    Millionth                       1960
 * nano    n   1000^-3    10^-9    0.000000001                 Billionth       Milliardth      1960
 * pico    p   1000^-4    10^-12   0.000000000001              Trillionth      Billionth       1960
 * femto   f   1000^-5    10^-15   0.000000000000001           Quadrillionth   Billiardth      1964
 * atto    a   1000^-6    10^-18   0.000000000000000001        Quintillionth   Trillionth      1964
 * zepto   z   1000^-7    10^-21   0.000000000000000000001     Sextillionth    Trilliardth     1991
 * yocto   y   1000^-8    10^-24   0.000000000000000000000001  Septillionth    Quadrillionth   1991
 * </pre>
 *
 * @checker_framework.manual #units-checker Units Checker
 */
public enum Prefix {
  /** SI prefix for 10^24. */
  yotta,
  /** SI prefix for 10^21. */
  zetta,
  /** SI prefix for 10^18. */
  exa,
  /** SI prefix for 10^15. */
  peta,
  /** SI prefix for 10^12. */
  tera,
  /** SI prefix for 10^9. */
  giga,
  /** SI prefix for 10^6. */
  mega,
  /** SI prefix for 10^3. */
  kilo,
  /** SI prefix for 10^2. */
  hecto,
  /** SI prefix for 10^1. */
  deca,
  /** SI prefix for 10^0, or 1. */
  one,
  /** SI prefix for 10^-1. */
  deci,
  /** SI prefix for 10^-2. */
  centi,
  /** SI prefix for 10^-3. */
  milli,
  /** SI prefix for 10^-6. */
  micro,
  /** SI prefix for 10^-9. */
  nano,
  /** SI prefix for 10^-12. */
  pico,
  /** SI prefix for 10^-15. */
  femto,
  /** SI prefix for 10^-18. */
  atto,
  /** SI prefix for 10^-21. */
  zepto,
  /** SI prefix for 10^-24. */
  yocto
}
