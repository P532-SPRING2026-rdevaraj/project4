package edu.iu.p532.rpl.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Render (and Heroku) hand out a database URL like
 * {@code postgresql://user:password@host:5432/dbname}. Spring/HikariCP needs
 * {@code jdbc:postgresql://host:5432/dbname} with credentials split into
 * separate properties. This processor detects the bare form (whether it
 * arrives as {@code DATABASE_URL} or {@code SPRING_DATASOURCE_URL}) and
 * normalises it before the DataSource bean is built.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String raw = firstNonBlank(
                env.getProperty("SPRING_DATASOURCE_URL"),
                env.getProperty("spring.datasource.url"),
                env.getProperty("DATABASE_URL"));
        if (raw == null) return;
        if (raw.startsWith("jdbc:")) return;
        if (!(raw.startsWith("postgres://") || raw.startsWith("postgresql://"))) return;

        try {
            URI uri = new URI(raw);
            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                username = colon < 0 ? userInfo : userInfo.substring(0, colon);
                password = colon < 0 ? null : userInfo.substring(colon + 1);
            }
            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + uri.getPath();
            if (uri.getQuery() != null) jdbcUrl += "?" + uri.getQuery();

            Map<String, Object> overrides = new HashMap<>();
            overrides.put("spring.datasource.url", jdbcUrl);
            if (username != null) overrides.put("spring.datasource.username", username);
            if (password != null) overrides.put("spring.datasource.password", password);
            env.getPropertySources().addFirst(
                    new MapPropertySource("renderDatabaseUrl", overrides));
        } catch (Exception ignored) {
            // Leave the original URL alone; Spring's own validation will surface a clear error.
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
