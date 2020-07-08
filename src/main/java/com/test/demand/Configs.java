package com.test.demand;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * @author wei.Li by 2019-04-10
 */
public class Configs {

    /**
     * 系统参数 -D properties
     * eg.
     * -Dskuld.config.size=10
     * ...
     */
    private static final Config SYSTEM_PROPERTIES = ConfigFactory.systemProperties();
    /**
     * 系统环境变量
     */
    private static final Config SYSTEM_ENVIRONMENT = ConfigFactory.systemEnvironment();

    private Configs() {
    }

    /**
     * System properties config.
     *
     * @return the config
     */
    public static Config systemProperties() {
        return SYSTEM_PROPERTIES;
    }

    /**
     * System environment config.
     *
     * @return the config
     */
    public static Config systemEnvironment() {
        return SYSTEM_ENVIRONMENT;
    }

    /**
     * This should return the current executing user path
     *
     * @return String execution directory
     */
    public static String getExecutionDirectory() {
        return SYSTEM_PROPERTIES.getString("user.dir");
    }

}
