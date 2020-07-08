package com.test.demand;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Github> https://github.com/typesafehub/config
 * <p>
 * ##支持配置文件格式
 * .properties  (支持${v}变量替换)
 * .json        (测试，不支持${v}变量替换)
 * .conf        (HOCON) (支持${v}变量替换)
 * 对于 maven 按环境自动装配配置文件${VAL}，推荐使用.properties
 * <p>
 * ## 关于 withFallback 方法说明
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * 1. a.withFallback(b) //a和b合并，如果有相同的key，以a为准
 * 2. a.withOnlyPath(String path) //只取a里的path下的配置
 * 3. a.withoutPath(String path) //只取a里出path外的配置
 * <p>
 * 参考 link Builder#with... 相关方法正确合并配置文件，目前未提供
 * <p>
 * ## 推荐配置参数优先级
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * 代码级别 Config > 配置文件*.conf > system_properties > system_environment > default.conf
 * <p>
 * ## 仅识别固定前缀字符变量，使用{@link ConfBuilder#usePrefix}
 * ## 识别环境配置，使用{@link ConfBuilder#systemEnvValName}
 * Config 内部实现类
 *
 * @author wei.Li
 */
public class ConfBuilder {

    /**
     * 输出 config 内容为字符串相关配置
     * 主要包括是否添加注释、格式化、是否为 json 格式
     * https://www.programcreek.com/scala/com.typesafe.config.ConfigRenderOptions
     */
    static final ConfigRenderOptions CONFIG_RENDER_OPTIONS_JSON = ConfigRenderOptions.defaults()
        .setComments(false)
        .setOriginComments(false)
        .setFormatted(true)
        .setJson(true);
    private static final String SYSTEM_ENV_VAL_NAME_FORMAT = "%env";

    /**
     * 固定前缀字段
     * eg. usePrefix=conf
     * 则最终解析后仅返回以 conf 开头变量
     */
    private String usePrefix;
    /**
     * 使用环境变量名称替换路径参数
     * 提取环境变量名称优先级 System.getProperties > System.getenv
     * eg.
     * path=/conf/%env/app.conf , systemEnvValName=env env=dev , 结果为 path=/conf/dev/app.conf
     * path=/conf/app-%env.conf , systemEnvValName=env env=dev , 结果为 path=/conf/app-dev.conf
     *
     * @see #buildEnvPath(String)
     */
    private String systemEnvValName;
    /**
     * 私有化
     */
    private Config config = ConfigFactory.empty();
    /**
     * 是否完成构建
     */
    private boolean build = false;

    private ConfBuilder() {
    }

    private ConfBuilder(String usePrefix) {
        this.usePrefix = usePrefix;
    }

    private ConfBuilder(String usePrefix, String systemEnvValName) {
        this.usePrefix = usePrefix;
        this.systemEnvValName = systemEnvValName;
    }

    public static ConfBuilder create() {
        return new ConfBuilder();
    }

    public static ConfBuilder create(String usePrefix) {
        return new ConfBuilder(usePrefix);
    }

    public static ConfBuilder create(String usePrefix, String systemEnvValName) {
        return new ConfBuilder(usePrefix, systemEnvValName);
    }

    protected Config getConfig() {
        return this.config;
    }

    /**
     * 路径内容替换
     * 提换逻辑参考 {@link #systemEnvValName} 详细描述
     *
     * @param path path
     * @return path
     */
    private String buildEnvPath(String path) {
        if (systemEnvValName == null) {
            return path;
        }
        final String p = System.getProperty(systemEnvValName);
        final String e = System.getenv(systemEnvValName);
        if (p == null && e == null) {
            throw new NullPointerException("systemEnvValName " + systemEnvValName + " null");
        }

        if (!path.contains(SYSTEM_ENV_VAL_NAME_FORMAT)) {
            throw new IllegalArgumentException("path not found " + SYSTEM_ENV_VAL_NAME_FORMAT + " , path:" + path);
        }
        return p == null ?
            path.replaceAll(SYSTEM_ENV_VAL_NAME_FORMAT, e) :
            path.replaceAll(SYSTEM_ENV_VAL_NAME_FORMAT, p);
    }

    /**
     * Resource 文件
     *
     * @param resource the resource
     * @return the builder
     */
    public ConfBuilder withFallbackResource(String resource) {
        final Config resourceConfig = ConfigFactory.parseResources(this.buildEnvPath(resource));
        final String empty = resourceConfig.entrySet().isEmpty() ? " contains no values" : "";
        this.config = this.config.withFallback(resourceConfig);
        return this;
    }

    /**
     * string/json 字符串.
     *
     * @param str the str
     * @return the config helper
     */
    public ConfBuilder withFallbackString(String str) {
        final Config mapConfig = ConfigFactory.parseString(str);
        final String empty = mapConfig.entrySet().isEmpty() ? " contains no values" : "";
        this.config = this.config.withFallback(mapConfig);
        return this;
    }

    /**
     * map 格式数据.
     *
     * @param values the values
     * @return the config helper
     */
    public ConfBuilder withFallbackMap(Map<String, Object> values) {
        final Config mapConfig = ConfigFactory.parseMap(values);
        final String empty = mapConfig.entrySet().isEmpty() ? " contains no values" : "";
        this.config = this.config.withFallback(mapConfig);
        return this;
    }

    /**
     * 系统参数.
     *
     * @return the builder
     */
    public ConfBuilder withFallbackSystemProperties() {
        this.config = this.config.withFallback(Configs.systemProperties());
        return this;
    }

    /**
     * 系统环境变量参数.
     *
     * @return the builder
     */
    public ConfBuilder withFallbackSystemEnvironment() {
        this.config = this.config.withFallback(Configs.systemEnvironment());
        return this;
    }

    /**
     * 指定路径配置文件
     *
     * @param path the path
     * @return the builder
     */
    public ConfBuilder withFallbackOptionalFile(String path) {
        final File secureConfFile = new File(this.buildEnvPath(path));
        if (secureConfFile.exists()) {
            this.config = this.config.withFallback(ConfigFactory.parseFile(secureConfFile));
        } else {
        }
        return this;
    }

    /**
     * 相对路径配置文件
     *
     * @param path the path
     * @return the builder
     */
    public ConfBuilder withFallbackOptionalRelativeFile(String path) {
        return this.withFallbackOptionalFile(Configs.getExecutionDirectory() + path);
    }

    /**
     * 配置
     *
     * @param config the config
     * @return the builder
     */
    private ConfBuilder withFallbackConfig(Config config) {
        this.config = this.config.withFallback(config);
        return this;
    }

    /**
     * 构建最终配置.
     * resolve > filter usePrefix
     */
    public synchronized Conf build() {

        if (this.build) {
            return Conf.createConf(this.getConfig());
        }

        // Resolve substitutions.
        this.config = this.config.resolve();

        //过滤固定前缀变量
        if (this.usePrefix != null) {
            final Set<Map.Entry<String, ConfigValue>> entries = config.entrySet();
            final Map<String, ConfigValue> cp = new HashMap<>(entries.size());
            entries.forEach(e -> {
                final String key = e.getKey();
                if (key.startsWith(this.usePrefix)) {
                    cp.put(key, e.getValue());
                }
            });

            final Config configCp = ConfigFactory.parseMap(cp);
            configCp.root().render(CONFIG_RENDER_OPTIONS_JSON);
            this.config = configCp;
        }
        this.build = true;
        return Conf.createConf(this.getConfig());
    }

}
