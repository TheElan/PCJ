/*
 * This file is the part of the PCJ Library
 */
package org.pcj.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for PCJ.
 *
 * <p>
 * Configuration reads System properties.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Configuration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    /**
     * pcj.backlog (int) default: 4096
     */
    final public static int BACKLOG_COUNT;
    /**
     * pcj.retry.count (int) default: 9
     */
    final public static int RETRY_COUNT;
    /**
     * pcj.retry.delay (int) default: 6
     */
    final public static int RETRY_DELAY;
    /**
     * pcj.port (int) default: 8091
     */
    final public static int DEFAULT_PORT;
    /**
     * pcj.chunksize (short) default: 1408
     */
    final public static short CHUNK_SIZE;
    /**
     * pcj.init.maxtime (int) default: max(60, (pcj.retry.count+1) * pcj.retry.delay)
     */
    final public static int INIT_MAXTIME;

    static {
        BACKLOG_COUNT = getPropertyInt("pcj.backlog", 4096);
        RETRY_COUNT = getPropertyInt("pcj.retry.count", 9);
        RETRY_DELAY = getPropertyInt("pcj.retry.delay", 6);
        DEFAULT_PORT = getPropertyInt("pcj.port", 8091);
        CHUNK_SIZE = getPropertyShort("pcj.chunksize", (short) (1408));
        INIT_MAXTIME = getPropertyInt("pcj.init.maxtime", Math.max((RETRY_COUNT + 1) * RETRY_DELAY, 60));

        LOGGER.log(Level.CONFIG, "BACKLOG_COUNT: {0,number,#}", BACKLOG_COUNT);
        LOGGER.log(Level.CONFIG, "RETRY_COUNT:   {0,number,#}", RETRY_COUNT);
        LOGGER.log(Level.CONFIG, "RETRY_DELAY:   {0,number,#}", RETRY_DELAY);
        LOGGER.log(Level.CONFIG, "DEFAULT_PORT:  {0,number,#}", DEFAULT_PORT);
        LOGGER.log(Level.CONFIG, "CHUNK_SIZE:    {0,number,#}", CHUNK_SIZE);
        LOGGER.log(Level.CONFIG, "INIT_MAXTIME:  {0,number,#}", INIT_MAXTIME);
    }

    private static int getPropertyInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.CONFIG, "Unable to parse to int: " + name, ex);
        }
        return defaultValue;
    }

    private static short getPropertyShort(String name, short defaultValue) {
        try {
            return Short.parseShort(System.getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.CONFIG, "Unable to parse to short: " + name, ex);
        }
        return defaultValue;
    }
}
