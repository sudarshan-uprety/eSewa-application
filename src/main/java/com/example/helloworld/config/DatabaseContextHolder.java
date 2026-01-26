package com.example.helloworld.config;

public class DatabaseContextHolder {
    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    public static void set(DataSourceType dataSourceType) {
        CONTEXT.set(dataSourceType);
    }

    public static DataSourceType get() {
        return CONTEXT.get();
    }

    public static void reset() {
        CONTEXT.remove();
    }
}
