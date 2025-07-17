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
		return SpringExtension.getApplicationContext(context)
			.getBean(JdbcTemplate.class);
	}

	private List<String> getTruncateQueries(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.queryForList(
			"SELECT CONCAT('DELETE FROM ', TABLE_NAME) AS q "
				+ "FROM INFORMATION_SCHEMA.TABLES "
				+ "WHERE TABLE_SCHEMA = 'PUBLIC' "
				+ "AND TABLE_TYPE = 'TABLE'",
			String.class);
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
