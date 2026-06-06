package com.qiniuyun.novelscript;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseSchemaInitializationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void test_p3_c3_schema_tables() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertTrue(tableExists(metaData, "project"));
            assertTrue(tableExists(metaData, "source_chapter"));
            assertTrue(tableExists(metaData, "chapter_context"));
            assertTrue(tableExists(metaData, "story_bible"));
            assertTrue(tableExists(metaData, "adaptation_job"));
            assertTrue(tableExists(metaData, "script_version"));
            assertTrue(tableExists(metaData, "yaml_snapshot"));
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[] {"TABLE"})) {
            while (resultSet.next()) {
                String currentTableName = resultSet.getString("TABLE_NAME");
                if (currentTableName != null
                    && currentTableName.toLowerCase(Locale.ROOT).equals(tableName.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }
}
