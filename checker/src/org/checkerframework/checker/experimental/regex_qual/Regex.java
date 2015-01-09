package org.checkerframework.checker.experimental.regex_qual;

/**
 *
 * Qualifier for the Regex-Qual type system.
 *
 * The static instances TOP and BOTTOM are used as the top and bottom of the hierarchy.
 *
 * {@link Regex.PartialRegex} is used to track string values that are not value regex.
 *
 * {@link Regex.RegexVal} is used to track valid regex values with a count of the number of groups.
 *
 */
public abstract class Regex {

    /* Top qualifier. */
    public static final Regex TOP = new Regex() {
        @Override
        public String toString() {
            return "RegexTop";
        }
    };

    /* Bottom qualifier. */
    public static final Regex BOTTOM = new Regex() {
        @Override
        public String toString() {
            return "RegexBot";
        }
    };

    public static class PartialRegex extends Regex {

        // The string value that is not a valid regex.
        private final String partialValue;

        public PartialRegex(String partialValue) {
            this.partialValue = partialValue;
        }

        public String getPartialValue() {
            return partialValue;
        }

        @Override
        public boolean isPartialRegex() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PartialRegex that = (PartialRegex) o;

            if (!partialValue.equals(that.partialValue)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return partialValue.hashCode();
        }

        @Override
        public String toString() {
            return "PartialRegex(\"" + partialValue + "\")";
        }
    }

    public static class RegexVal extends Regex {

        // The number of regex groups available.
        private final int count;

        public RegexVal(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RegexVal regexVal = (RegexVal) o;

            if (count != regexVal.count) return false;

            return true;
        }

        @Override
        public boolean isRegexVal() {
            return true;
        }

        @Override
        public int hashCode() {
            return count;
        }

        @Override
        public String toString() {
            if (count > 0) {
                return "Regex(" + count + ")";
            } else {
                return "Regex";
            }

        }
    }

    public boolean isRegexVal() {
        return false;
    }

    public boolean isPartialRegex() {
        return false;
    }

    @Override
    public String toString() {
        return "Regex";
    }
}