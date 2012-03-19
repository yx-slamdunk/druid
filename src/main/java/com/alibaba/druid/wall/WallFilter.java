package com.alibaba.druid.wall;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.alibaba.druid.DruidRuntimeException;
import com.alibaba.druid.filter.FilterAdapter;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.logging.Log;
import com.alibaba.druid.logging.LogFactory;
import com.alibaba.druid.proxy.jdbc.CallableStatementProxy;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;
import com.alibaba.druid.proxy.jdbc.DataSourceProxy;
import com.alibaba.druid.proxy.jdbc.PreparedStatementProxy;
import com.alibaba.druid.proxy.jdbc.ResultSetProxy;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.druid.wall.spi.MySqlWallProvider;
import com.alibaba.druid.wall.spi.OracleWallProvider;
import com.alibaba.druid.wall.spi.SQLServerProvider;

public class WallFilter extends FilterAdapter implements WallFilterMBean {

    private final static Log LOG            = LogFactory.getLog(WallFilter.class);

    private boolean          inited         = false;

    private WallProvider     provider;

    private String           dbType;

    private WallConfig       config;

    private volatile boolean logViolation   = false;
    private volatile boolean throwException = true;

    @Override
    public void init(DataSourceProxy dataSource) {
        this.dataSource = dataSource;

        String dbType = this.dbType;

        if (this.dbType == null || this.dbType.trim().length() == 0) {
            this.dbType = dataSource.getDbType();
        }

        if (JdbcUtils.MYSQL.equals(dbType)) {
            provider = new MySqlWallProvider(config);

            if (config == null) {
                config = new WallConfig(MySqlWallProvider.DEFAULT_CONFIG_DIR);
            }
        } else if (JdbcUtils.ORACLE.equals(dbType)) {
            provider = new OracleWallProvider(config);

            if (config == null) {
                config = new WallConfig(OracleWallProvider.DEFAULT_CONFIG_DIR);
            }
        } else if (JdbcUtils.SQL_SERVER.equals(dbType)) {
            provider = new SQLServerProvider(config);

            if (config == null) {
                config = new WallConfig(SQLServerProvider.DEFAULT_CONFIG_DIR);
            }
        } else {
            throw new IllegalStateException("dbType not support : " + dbType);
        }

        this.inited = true;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public boolean isLogViolation() {
        return logViolation;
    }

    public void setLogViolation(boolean logViolation) {
        this.logViolation = logViolation;
    }

    public boolean isThrowException() {
        return throwException;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    public void clearProviderCache() {
        if (provider != null) {
            provider.clearCache();
        }
    }

    public Set<String> getProviderWhiteList() {
        if (provider == null) {
            return Collections.emptySet();
        }

        return provider.getWhiteList();
    }

    public WallProvider getProvider() {
        return provider;
    }

    public WallConfig getConfig() {
        return config;
    }

    public void setConfig(WallConfig config) {
        this.config = config;
    }

    public boolean isInited() {
        return inited;
    }

    public void checkInit() {
        if (inited) {
            throw new DruidRuntimeException("wall filter is inited");
        }
    }

    @Override
    public ConnectionProxy connection_connect(FilterChain chain, Properties info) throws SQLException {
        return chain.connection_connect(info);
    }

    @Override
    public PreparedStatementProxy connection_prepareStatement(FilterChain chain, ConnectionProxy connection, String sql)
                                                                                                                        throws SQLException {
        check(connection, sql);
        return chain.connection_prepareStatement(connection, sql);
    }

    @Override
    public PreparedStatementProxy connection_prepareStatement(FilterChain chain, ConnectionProxy connection,
                                                              String sql, int autoGeneratedKeys) throws SQLException {
        check(connection, sql);
        return chain.connection_prepareStatement(connection, sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatementProxy connection_prepareStatement(FilterChain chain, ConnectionProxy connection,
                                                              String sql, int resultSetType, int resultSetConcurrency)
                                                                                                                      throws SQLException {
        check(connection, sql);
        return chain.connection_prepareStatement(connection, sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatementProxy connection_prepareStatement(FilterChain chain, ConnectionProxy connection,
                                                              String sql, int resultSetType, int resultSetConcurrency,
                                                              int resultSetHoldability) throws SQLException {
        check(connection, sql);
        return chain.connection_prepareStatement(connection, sql, resultSetType, resultSetConcurrency,
                                                 resultSetHoldability);
    }

    @Override
    public PreparedStatementProxy connection_prepareStatement(FilterChain chain, ConnectionProxy connection,
                                                              String sql, int[] columnIndexes) throws SQLException {
        check(connection, sql);
        return chain.connection_prepareStatement(connection, sql, columnIndexes);
    }

    @Override
    public PreparedStatementProxy connection_prepareStatement(FilterChain chain, ConnectionProxy connection,
                                                              String sql, String[] columnNames) throws SQLException {
        check(connection, sql);
        return chain.connection_prepareStatement(connection, sql, columnNames);
    }

    @Override
    public CallableStatementProxy connection_prepareCall(FilterChain chain, ConnectionProxy connection, String sql)
                                                                                                                   throws SQLException {
        check(connection, sql);
        return chain.connection_prepareCall(connection, sql);
    }

    @Override
    public CallableStatementProxy connection_prepareCall(FilterChain chain, ConnectionProxy connection, String sql,
                                                         int resultSetType, int resultSetConcurrency)
                                                                                                     throws SQLException {
        check(connection, sql);
        return chain.connection_prepareCall(connection, sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatementProxy connection_prepareCall(FilterChain chain, ConnectionProxy connection, String sql,
                                                         int resultSetType, int resultSetConcurrency,
                                                         int resultSetHoldability) throws SQLException {
        check(connection, sql);
        return chain.connection_prepareCall(connection, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    // //////////////

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_execute(statement, sql);
    }

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql, int autoGeneratedKeys)
                                                                                                                    throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_execute(statement, sql, autoGeneratedKeys);
    }

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql, int columnIndexes[])
                                                                                                                  throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_execute(statement, sql, columnIndexes);
    }

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql, String columnNames[])
                                                                                                                   throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_execute(statement, sql, columnNames);
    }

    @Override
    public ResultSetProxy statement_executeQuery(FilterChain chain, StatementProxy statement, String sql)
                                                                                                         throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_executeQuery(statement, sql);
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_executeUpdate(statement, sql);
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, int autoGeneratedKeys)
                                                                                                                      throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_executeUpdate(statement, sql, autoGeneratedKeys);
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, int columnIndexes[])
                                                                                                                    throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_executeUpdate(statement, sql, columnIndexes);
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, String columnNames[])
                                                                                                                     throws SQLException {
        check(statement.getConnectionProxy(), sql);
        return chain.statement_executeUpdate(statement, sql, columnNames);
    }

    public void check(ConnectionProxy connection, String sql) throws SQLException {
        List<Violation> violations = provider.check(sql);

        if (violations.size() > 0) {
            if (isLogViolation()) {
                LOG.error("sql injection violation : " + sql);
            }

            if (throwException) {
                throw new SQLException("sql injection violation : " + sql);
            }
        }
    }

}
