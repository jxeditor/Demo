package com.test.flink;

import org.apache.flink.table.api.TableException;
import org.apache.flink.table.factories.TableFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Optional;

/**
 * @author XiaShuai on 2020/6/11.
 */
public class Demo {
    public static void main(String[] args) {
        List<TableFactory> tableFactories = discoverFactories(Optional.empty());
        for (int i = 0; i < tableFactories.size(); i++) {
            System.out.println(tableFactories.get(i).toString());
        }
    }

    private static List<TableFactory> discoverFactories(Optional<ClassLoader> classLoader) {
        try {
            List<TableFactory> result = new LinkedList();
            ClassLoader cl = (ClassLoader) classLoader.orElse(Thread.currentThread().getContextClassLoader());
            ServiceLoader.load(TableFactory.class, cl).iterator().forEachRemaining(result::add);
            return result;
        } catch (ServiceConfigurationError var3) {
            throw new TableException("Could not load service provider for table factories.", var3);
        }
    }
}
