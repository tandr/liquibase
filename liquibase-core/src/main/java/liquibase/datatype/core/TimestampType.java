package liquibase.datatype.core;

import java.util.Locale;

import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.util.StringUtils;

@DataTypeInfo(name = "timestamp", aliases = {"java.sql.Types.TIMESTAMP", "java.sql.Timestamp", "timestamptz"}, minParameters = 0, maxParameters = 1, priority = LiquibaseDataType.PRIORITY_DEFAULT)
public class TimestampType extends DateTimeType {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        String originalDefinition = StringUtils.trimToEmpty(getRawDefinition());
        if (database instanceof MySQLDatabase) {
            if (getRawDefinition().contains(" ") || getRawDefinition().contains("(")) {
                return new DatabaseDataType(getRawDefinition());
            }
            return super.toDatabaseDataType(database);
        }
        if (database instanceof MSSQLDatabase) {
            if (!LiquibaseConfiguration.getInstance().getProperty(GlobalConfiguration.class, GlobalConfiguration.CONVERT_DATA_TYPES).getValue(Boolean.class) && originalDefinition.toLowerCase().startsWith("timestamp")) {
                return new DatabaseDataType(database.escapeDataTypeName("timestamp"));
            }

            return new DatabaseDataType(database.escapeDataTypeName("datetime"));
        }
        if (database instanceof SybaseDatabase) {
            return new DatabaseDataType(database.escapeDataTypeName("datetime"));
        }
        if (database instanceof DB2Database) {
            Object[] parameters = getParameters();
            if (parameters != null && parameters.length > 1) {
                parameters = new Object[] {parameters[1]};
            }
            return new DatabaseDataType(database.escapeDataTypeName("timestamp"), parameters);
        }


        if (getAdditionalInformation() != null
                && (database instanceof PostgresDatabase
                || database instanceof OracleDatabase)
                || database instanceof HsqlDatabase){
            DatabaseDataType type = new DatabaseDataType("TIMESTAMP");
            String additionalInformation = this.getAdditionalInformation();

            if (additionalInformation != null) {
                String additionInformation = additionalInformation.toUpperCase(Locale.US);
                if ((database instanceof PostgresDatabase) && additionInformation.contains("TIMEZONE")) {
                    additionalInformation = additionInformation.replace("TIMEZONE", "TIME ZONE");
                }
                // CORE-3229 Oracle 11g doesn't support WITHOUT clause in TIMESTAMP data type
                if ((database instanceof OracleDatabase) && additionInformation.startsWith("WITHOUT")) {
                    // https://docs.oracle.com/cd/B19306_01/server.102/b14225/ch4datetime.htm#sthref389
                    additionalInformation = null;
                }
            }

            type.addAdditionalInformation(additionalInformation);
            return type;
        }

        return super.toDatabaseDataType(database);
    }
}
