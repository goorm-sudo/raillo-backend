package com.sudo.railo.support.extension;

import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class DatabaseCleanupExtension implements AfterEachCallback {

	@Override
	public void afterEach(ExtensionContext context) {
		JdbcTemplate jdbcTemplate = getJdbcTemplate(context);
		final List<String> truncateQueries = getTruncateQueries(jdbcTemplate);
		truncateTables(jdbcTemplate, truncateQueries);
	}

	private JdbcTemplate getJdbcTemplate(ExtensionContext context) {
		return SpringExtension.getApplicationContext(context).getBean(JdbcTemplate.class);
	}

	private List<String> getTruncateQueries(JdbcTemplate jdbcTemplate) {
		List<String> tableNames = jdbcTemplate.query(
			"SELECT TABLE_SCHEMA, TABLE_NAME " + "FROM INFORMATION_SCHEMA.TABLES ",
			(rs, rowNum) -> rs.getString("TABLE_SCHEMA") + "." + rs.getString("TABLE_NAME"));

		return tableNames.stream()
			.filter(tableNameWithSchema -> tableNameWithSchema.startsWith("PUBLIC."))
			.map(tableNameWithSchema -> "DELETE FROM " + tableNameWithSchema)
			.collect(java.util.stream.Collectors.toList());
	}

	private void truncateTables(JdbcTemplate jdbcTemplate, List<String> truncateQueries) {
		try {
			execute(jdbcTemplate, "SET FOREIGN_KEY_CHECKS = FALSE");
			truncateQueries.forEach(query -> execute(jdbcTemplate, query));
		} finally {
			execute(jdbcTemplate, "SET FOREIGN_KEY_CHECKS = TRUE");
		}
	}

	private void execute(JdbcTemplate jdbcTemplate, String query) {
		jdbcTemplate.execute(query);
	}
}
