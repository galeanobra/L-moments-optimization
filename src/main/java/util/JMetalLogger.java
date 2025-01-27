package util;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class provides some facilities to manage loggers. One might use the
 * static logger of this class or use its own, custom logger. Also, we provide
 * the static method for configuring the loggers
 * easily. This method is automatically called before any use of the static
 * logger, but if you want it to apply on other loggers it is preferable to call
 * it explicitly at the beginning of your main() method.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 * @author Matthieu Vergne <matthieu.vergne@gmail.com>
 */
public class JMetalLogger implements Serializable {

    public static final Logger logger = Logger.getLogger(JMetalLogger.class.getName());

    static {
        /*
         * Configure the loggers with the default configuration. If the
         * configuration method is called manually, this default configuration
         * will be called before anyway, leading to 2 configuration calls,
         * although only the last one is considered. This is a trade off to
         * ensure that, if this method is not called manually, then it is still
         * called automatically and as soon as we use jMetalLogger, in order to
         * have at least the default configuration.
         */
        try {
            configureLoggers();
        } catch (IOException e) {
            throw new RuntimeException("Impossible to configure the loggers in a static way", e);
        }
    }

    /**
     * This method provides a single-call method to configure the {@link Logger}
     * instances. A default configuration is considered, enriched with a custom
     * property file for more convenient logging. The custom file is considered
     * after the default configuration, so it can override it if necessary. The
     * custom file might be provided as an argument of this method, otherwise we
     * look for a file named "jMetal.log.ini". If no custom file is provided,
     * then only the default configuration is considered.
     */
    public static void configureLoggers() throws IOException {
        // Prepare default configuration
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printer = new PrintStream(stream);
        printer.println(".level = INFO");
        printer.println("handlers = java.util.logging.ConsoleHandler");
        printer.println("formatters = java.util.logging.SimpleFormatter");
        printer.println("java.util.logging.SimpleFormatter.format = %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$s: %5$s [%2$s]%6$s%n");
        printer.println("java.util.logging.ConsoleHandler.level = ALL");

        LogManager manager = LogManager.getLogManager();
        manager.readConfiguration(IOUtils.toInputStream(new String(stream.toByteArray(), Charset.forName("UTF-8"))));
    }
}
