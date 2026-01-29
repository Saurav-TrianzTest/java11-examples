package io.github.biezhi.java11.string;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * String.repeat(int)
 * String.lines()
 * String.strip()
 * String.stripLeading()
 * String.stripTrailing()
 * String.isBlank()
 *
 * Cloud-ready version with structured logging for AWS CloudWatch
 *
 * @author biezhi
 * @date 2018/7/10
 */
public class Example {

    private static final Logger logger = LoggerFactory.getLogger(Example.class);

    /**
     * Write provided {@code String} in header. Note that this
     * implementation uses {@code String.repeat(int)}.
     *
     * Cloud improvement: Uses SLF4J structured logging instead of System.out
     *
     * @param headerText Title of header.
     */
    private static void writeHeader(final String headerText) {
        final String headerSeparator = "=".repeat(headerText.length() + 4);

        logger.info("\n{}", headerSeparator);
        logger.info("{}", headerText);
        logger.info("{}", headerSeparator);
    }


    /**
     * Demonstrate method {@code String.lines()} added with JDK 11.
     */
    public static void demonstrateStringLines() {
        String originalString = "Hello\nWorld\n123";

        String stringWithoutLineSeparators = originalString.replaceAll("\\n", "\\\\n");

        writeHeader("String.lines() on '" + stringWithoutLineSeparators + "'");

        originalString.lines().forEach(line -> logger.info("{}", line));
    }

    /**
     * Demonstrate method {@code String.strip()} added with JDK 11.
     */
    public static void demonstrateStringStrip() {
        String originalString = "  biezhi.me  23333  ";

        writeHeader("String.strip() on '" + originalString + "'");
        logger.info("'{}'", originalString.strip());
    }

    /**
     * Demonstrate method {@code String.stripLeading()} added with JDK 11.
     */
    public static void demonstrateStringStripLeading() {
        String originalString = "  biezhi.me  23333  ";

        writeHeader("String.stripLeading() on '" + originalString + "'");
        logger.info("'{}'", originalString.stripLeading());
    }

    /**
     * Demonstrate method {@code String.stripTrailing()} added with JDK 11.
     */
    public static void demonstrateStringStripTrailing() {
        String originalString = "  biezhi.me  23333  ";

        writeHeader("String.stripTrailing() on '" + originalString + "'");
        logger.info("'{}'", originalString.stripTrailing());
    }

    /**
     * Demonstrate method {@code String.isBlank()} added with JDK 11.
     */
    public static void demonstrateStringIsBlank() {
        writeHeader("String.isBlank()");

        String emptyString = "";
        logger.info("空字符串    -> {}", emptyString.isBlank());

        String onlyLineSeparator = System.getProperty("line.separator");
        logger.info("换行符     -> {}", onlyLineSeparator.isBlank());

        String tabOnly = "\t";
        logger.info("Tab 制表符 -> {}", tabOnly.isBlank());

        String spacesOnly = "   ";
        logger.info("空格       -> {}", spacesOnly.isBlank());
    }


    public static void lines() {
        writeHeader("String.lines()");

        String str = "Hello \n World, I,m\nbiezhi.";

        logger.info("{}", str.lines().collect(Collectors.toList()));
    }

    public static void main(String[] args) {
//        writeHeader("User-Agent\tMozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5)");
//        demonstrateStringLines();
//        demonstrateStringStrip();
//        demonstrateStringStripLeading();
//        demonstrateStringStripTrailing();
//        demonstrateStringIsBlank();
        lines();
    }

}
