package com.example.achievement.config;

import java.sql.Types;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.type.StandardBasicTypes;

public class SQLiteDialect extends Dialect {

    private final LimitHandler limitHandler = new AbstractLimitHandler() {
        @Override
        public String processSql(String sql, org.hibernate.engine.spi.RowSelection selection) {
            if (selection == null) {
                return sql;
            }
            int maxRows = selection.getMaxRows() != null ? selection.getMaxRows() : 0;
            int firstRow = selection.getFirstRow() != null ? selection.getFirstRow() : 0;
            return sql + (maxRows > 0 ? " limit " + maxRows : "") +
                   (firstRow > 0 ? " offset " + firstRow : "");
        }

        @Override
        public boolean supportsLimit() {
            return true;
        }

        @Override
        public boolean supportsLimitOffset() {
            return true;
        }

        @Override
        public boolean bindLimitParametersInReverseOrder() {
            return false;
        }

        @Override
        public boolean bindLimitParametersFirst() {
            return false;
        }

        @Override
        public boolean useMaxForLimit() {
            return false;
        }

        @Override
        public boolean forceLimitUsage() {
            return false;
        }
    };

    public SQLiteDialect() {
        registerColumnType(Types.BIT, "integer");
        registerColumnType(Types.TINYINT, "tinyint");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.FLOAT, "float");
        registerColumnType(Types.REAL, "real");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.NUMERIC, "numeric");
        registerColumnType(Types.DECIMAL, "decimal");
        registerColumnType(Types.CHAR, "char");
        registerColumnType(Types.VARCHAR, "varchar");
        registerColumnType(Types.LONGVARCHAR, "longvarchar");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "timestamp");
        registerColumnType(Types.BINARY, "blob");
        registerColumnType(Types.VARBINARY, "blob");
        registerColumnType(Types.LONGVARBINARY, "blob");
        registerColumnType(Types.BLOB, "blob");
        registerColumnType(Types.CLOB, "clob");
        registerColumnType(Types.BOOLEAN, "integer");

        registerFunction("concat", new VarArgsSQLFunction(StandardBasicTypes.STRING, "", "||", ""));
        registerFunction("mod", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "?1 % ?2"));
        registerFunction("substr", new StandardSQLFunction("substr", StandardBasicTypes.STRING));
        registerFunction("substring", new StandardSQLFunction("substr", StandardBasicTypes.STRING));
    }

    @Override
    public boolean supportsLimit() {
        return true;
    }

    @Override
    public boolean supportsSequences() {
        return false;
    }

    @Override
    public String getAddColumnString() {
        return "add column";
    }

    @Override
    public String getForUpdateString() {
        return "";
    }

    @Override
    public LimitHandler getLimitHandler() {
        return limitHandler;
    }
}
