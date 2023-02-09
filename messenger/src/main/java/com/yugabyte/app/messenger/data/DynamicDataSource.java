package com.yugabyte.app.messenger.data;

import java.util.HashMap;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Component
@Scope("singleton")
public class DynamicDataSource extends AbstractRoutingDataSource {

    private enum YugabyteConnectionType {
        STANDARD, REPLICA, GEO
    }

    public final static Integer CURRENT_DATA_SOURCE_KEY = 1;

    private String dsClassName;
    private String primaryEndpoint;
    private int portNumber;
    private String username;
    private String password;
    private String schemaName;
    private String databaseName;
    private String yugabyteConnType;
    private String additionalEndpoints;
    private int maxPoolSize;
    private String sslMode;

    private HashMap<Object, Object> dataSources = new HashMap<>();

    public DynamicDataSource(
            @Value("${dataSourceClassName}") String dsClassName,
            @Value("${dataSource.serverName}") String primaryEndpoint,
            @Value("${dataSource.portNumber:5433}") int portNumber,
            @Value("${dataSource.user}") String username,
            @Value("${dataSource.password}") String password,
            @Value("${dataSource.databaseName}") String databaseName,
            @Value("${dataSource.sslMode:disable}") String sslMode,
            @Value("${spring.datasource.hikari.maximum-pool-size:5}") int maxPoolSize,
            @Value("${spring.datasource.hikari.schema:public}") String schemaName,
            @Value("${dataSource.additionalEndpoints:}") String additionalEndpoints,
            @Value("${yugabytedb.connection.type:standard}") String yugabyteConnType) {
        this.dsClassName = dsClassName;
        this.primaryEndpoint = primaryEndpoint;
        this.portNumber = portNumber;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
        this.sslMode = sslMode;
        this.maxPoolSize = maxPoolSize;
        this.schemaName = schemaName;
        this.yugabyteConnType = yugabyteConnType;
        this.additionalEndpoints = additionalEndpoints;

        setTargetDataSources(dataSources);

        initDataSource();

        afterPropertiesSet();
    }

    private void initDataSource() {
        HikariConfig cfg = new HikariConfig();

        cfg.setDataSourceClassName(dsClassName);
        cfg.setSchema(schemaName);
        cfg.setMaximumPoolSize(maxPoolSize);

        Properties dsProps = new Properties();

        dsProps.setProperty("serverName", primaryEndpoint);
        dsProps.setProperty("portNumber", String.valueOf(portNumber));
        dsProps.setProperty("databaseName", databaseName);
        dsProps.setProperty("user", username);
        dsProps.setProperty("password", password);

        if (!sslMode.equals("disable")) {
            dsProps.setProperty("ssl", "true");
            dsProps.setProperty("sslMode", sslMode);
        }

        if (!additionalEndpoints.isBlank())
            dsProps.setProperty("additionalEndpoints", additionalEndpoints);

        cfg.setDataSourceProperties(dsProps);

        if (isReplicaConnection()) {
            System.out.println("Setting read only session characteristics for the replica connection");

            cfg.setConnectionInitSql(
                    "set session characteristics as transaction read only;" +
                            "set yb_read_from_followers = true;");
        }

        System.out.printf(
                "Initializing new data source with the primary endpoint '%s' %n and additional endpoints '%s'%n",
                primaryEndpoint, additionalEndpoints);

        HikariDataSource ds = new HikariDataSource(cfg);

        dataSources.put(CURRENT_DATA_SOURCE_KEY, ds);
        setDefaultTargetDataSource(ds);

        System.out.printf("Initialized new data source for '%s' connection%n", primaryEndpoint);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return CURRENT_DATA_SOURCE_KEY;
    }

    public void createNewDataSource(String url, String username, String password, String yugabyteConnType) {
        this.primaryEndpoint = url;
        this.username = username;
        this.password = password;
        this.yugabyteConnType = yugabyteConnType;

        initDataSource();
        afterPropertiesSet();
    }

    public boolean isReplicaConnection() {
        return yugabyteConnType.equalsIgnoreCase(YugabyteConnectionType.REPLICA.name());
    }
}
