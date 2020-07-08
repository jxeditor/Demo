package com.test.demand;

import com.typesafe.config.Config;

import java.util.List;

/**
 * 不可变对象
 * 构建{@link ConfBuilder#build()}
 * <p>
 * 具体使用示例参考 com.skuld.service.tools.config.ConfigsTest
 * TODO Config 默认不支持序列化，实现可参考 config.root().render(ConfigRenderOptions.defaults())后获取JSON字符串进行序列化反序列化操作
 *
 * @author wei.Li by 2019-04-19
 */
public final class Conf {

    /**
     * 私有化
     */
    private Config config;

    private Conf(Config config) {
        this.config = config;
    }

    static Conf createConf(Config config) {
        return new Conf(config);
    }

    private Config getConfig() {
        return config;
    }

    /**
     * 合并配置文件
     *
     * @param conf skuldConf
     * @return SkuldConf
     */
    public Conf withFallbackSkuldConf(Conf conf) {
        return createConf(conf.config.withFallback(this.getConfig()));
    }

    /**
     * 获取指定 path 下内容作为新的配置（不包含 path 本身）
     *
     * @param path path
     * @return Conf
     */
    public Conf getPath(String path) {
        return createConf(this.getConfig().getConfig(path));
    }

    /**
     * 获取指定 path 下内容作为新的配置（包含 path 本身）
     *
     * @param path path
     * @return Conf
     */
    public Conf withOnlyPath(String path) {
        return createConf(this.getConfig().withOnlyPath(path));
    }

    /**
     * 排除指定 path 下内容作为新的配置
     *
     * @param path path
     * @return Conf
     */
    public Conf withPath(String path) {
        return createConf(this.getConfig().withoutPath(path));
    }

    /**
     * 格式化打印输出
     *
     * @return Conf
     */
    public Conf print() {
        return this;
    }

    /**
     * 转换 JSON 字符串
     *
     * @return JSON 字符串
     */
    public String json() {
        return this.getConfig().root().render(ConfBuilder.CONFIG_RENDER_OPTIONS_JSON);
    }

    /*
   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   has 方法
   请使用合并配置文件方法进行新增
   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

    public boolean hasPath(String path) {
        return this.getConfig().hasPath(path);
    }

    /*
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     set 方法
     目前不支持，请使用合并配置文件方法进行新增
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /*
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     get 方法
     需要扩展时根据 Config API 接口扩展即可
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public boolean getBoolean(String path) {
        return this.getConfig().getBoolean(path);
    }

    public boolean getBoolean(String path, boolean defaultVal) {
        return hasPath(path) ? this.getBoolean(path) : defaultVal;
    }

    public Number getNumber(String path) {
        return this.getConfig().getNumber(path);
    }

    public int getInt(String path) {
        return this.getConfig().getInt(path);
    }

    public long getLong(String path) {
        return this.getConfig().getLong(path);
    }

    public long getLong(String path, long defaultVal) {
        return hasPath(path) ? this.getLong(path) : defaultVal;
    }

    public double getDouble(String path) {
        return this.getConfig().getDouble(path);
    }

    public String getString(String path) {
        return this.getConfig().getString(path);
    }

    public String getString(String path, String defaultVal) {
        return hasPath(path) ? this.getString(path) : defaultVal;
    }

    /**
     * 使用如下写法抛出异常
     * System.setProperty("list.property", """[ "val1", "val2"]""")
     * That was throwing
     * com.typesafe.config.ConfigException$WrongType: system properties: list.property has type STRING rather than LIST
     * <p>
     * 正确设置方式
     * System.setProperty("list.property.0", "val1")
     * System.setProperty("list.property.1", "val2")
     *
     * @param path path
     * @return List
     */
    public List<String> getStringList(String path) {
        return this.getConfig().getStringList(path);
    }


    @Override
    public String toString() {
        return json();
    }
}
