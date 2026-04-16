package co.kr.woojjam.event_deep_dive.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

@Configuration
public class ShedLockProvider {

	@Bean
	public LockProvider lockProvider(final DataSource dataSource) {

		String instanceName = System.getenv().getOrDefault("APP_INSTANCE_ID", "unknown");

		return new JdbcTemplateLockProvider(
			JdbcTemplateLockProvider.Configuration.builder()
				.withJdbcTemplate(new JdbcTemplate(dataSource))
				.withLockedByValue("instance-" + instanceName)
				.usingDbTime()
				.build()
		);
	}
}
