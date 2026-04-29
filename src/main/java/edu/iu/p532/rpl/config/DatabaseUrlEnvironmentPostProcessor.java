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
        // DATABASE_URL is checked FIRST and exclusively. application.properties
        // already has a localhost default for spring.datasource.url, so checking
        // that property here would always short-circuit and ignore Render's
        // DATABASE_URL env var.
        String raw = firstNonBlank(
                env.getProperty("DATABASE_URL"),
                env.getProperty("database.url"));
        if (raw == null) return;
        if (raw.startsWith("jdbc:")) {
            // Already a JDBC URL — just promote it to spring.datasource.url so
            // it overrides the application.properties default.
            Map<String, Object> direct = new HashMap<>();
            direct.put("spring.datasource.url", raw);
            env.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", direct));
            return;
        }
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
            System.out.println("[DatabaseUrlEnvironmentPostProcessor] translated DATABASE_URL → " + jdbcUrl
                    + " (user=" + username + ")");
        } catch (Exception e) {
            System.err.println("[DatabaseUrlEnvironmentPostProcessor] failed to parse DATABASE_URL: " + e.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
