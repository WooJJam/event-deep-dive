package co.kr.woojjam.event_deep_dive.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

@Configuration
public class ShedLockProvider {

	@Bean
	public LockProvider lockProvider(final DataSource dataSource, @Qualifier("instanceId") final String instanceId) {
		return new JdbcTemplateLockProvider(
			JdbcTemplateLockProvider.Configuration.builder()
				.withJdbcTemplate(new JdbcTemplate(dataSource))
				.withLockedByValue("instance-" + instanceId)
				.usingDbTime()
				.build()
		);
	}
}
