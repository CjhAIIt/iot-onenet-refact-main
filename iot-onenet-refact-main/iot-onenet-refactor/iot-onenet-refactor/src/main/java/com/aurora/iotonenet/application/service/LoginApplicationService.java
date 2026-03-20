package com.aurora.iotonenet.application.service;

import com.aurora.iotonenet.api.dto.LoginRequest;
import com.aurora.iotonenet.api.dto.LoginResponse;
import com.aurora.iotonenet.api.dto.RegisterRequest;
import com.aurora.iotonenet.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(LoginApplicationService.class);
    private static final String SESSION_USER_KEY = "logged_in_user";

    @Value("${app.login.username}")
    private String configUsername;

    @Value("${app.login.password}")
    private String configPassword;

    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> registeredUsers = new ConcurrentHashMap<>();
    private final boolean databaseMode;

    public LoginApplicationService(AppProperties appProperties, PasswordEncoder passwordEncoder) {
        this.appProperties = appProperties;
        this.passwordEncoder = passwordEncoder;
        this.databaseMode = "mysql".equalsIgnoreCase(appProperties.getUserStore().getMode());
    }

    @PostConstruct
    public void initDefaultUser() {
        if (databaseMode) {
            initializeDatabase();
            ensureDefaultUserInDatabase();
            logger.info("Login service initialized with MySQL user store");
            return;
        }

        registeredUsers.put(configUsername, passwordEncoder.encode(configPassword));
        logger.info("Login service initialized with in-memory user store, default user={}", configUsername);
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
        String username = request.getUsername();
        String password = request.getPassword();
        logger.info("Login attempt: username={}", username);

        String storedPasswordHash = loadPasswordHash(username);
        if (storedPasswordHash != null && passwordMatches(password, storedPasswordHash)) {
            session.setAttribute(SESSION_USER_KEY, username);
            logger.info("Login succeeded: username={}", username);
            return new LoginResponse(true, "登录成功");
        }

        logger.warn("Login failed: username={}", username);
        return new LoginResponse(false, "用户名或密码错误");
    }

    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        logger.info("Register attempt: username={}", username);

        if (!password.equals(confirmPassword)) {
            return new LoginResponse(false, "两次输入的密码不一致");
        }

        if (userExists(username)) {
            return new LoginResponse(false, "用户名已存在，请更换用户名");
        }

        saveUser(username, passwordEncoder.encode(password));
        logger.info("Register succeeded: username={}", username);
        return new LoginResponse(true, "注册成功，请使用新账号登录");
    }

    public Map<String, Object> checkLogin(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Object user = session.getAttribute(SESSION_USER_KEY);
        if (user != null) {
            response.put("loggedIn", true);
            response.put("username", user);
        } else {
            response.put("loggedIn", false);
        }
        return response;
    }

    public LoginResponse logout(HttpSession session) {
        Object user = session.getAttribute(SESSION_USER_KEY);
        if (user != null) {
            logger.info("Logout: username={}", user);
            session.invalidate();
        }
        return new LoginResponse(true, "已登出");
    }

    private String loadPasswordHash(String username) {
        if (!databaseMode) {
            return registeredUsers.get(username);
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select password_hash from app_users where username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load user from database", ex);
        }
    }

    private boolean userExists(String username) {
        if (!databaseMode) {
            return registeredUsers.containsKey(username);
        }
        return StringUtils.hasText(loadPasswordHash(username));
    }

    private void saveUser(String username, String passwordHash) {
        if (!databaseMode) {
            registeredUsers.put(username, passwordHash);
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "insert into app_users (username, password_hash) values (?, ?)")) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save user into database", ex);
        }
    }

    private boolean passwordMatches(String rawPassword, String storedPasswordHash) {
        if (!StringUtils.hasText(storedPasswordHash)) {
            return false;
        }

        if (storedPasswordHash.startsWith("$2a$")
                || storedPasswordHash.startsWith("$2b$")
                || storedPasswordHash.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPasswordHash);
        }
        return storedPasswordHash.equals(rawPassword);
    }

    private void initializeDatabase() {
        AppProperties.UserStore userStore = appProperties.getUserStore();
        if (!StringUtils.hasText(userStore.getJdbcUrl())) {
            throw new IllegalStateException("MySQL user store mode requires app.user-store.jdbc-url");
        }

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table if not exists app_users (
                        username varchar(64) primary key,
                        password_hash varchar(255) not null,
                        created_at timestamp default current_timestamp,
                        updated_at timestamp default current_timestamp on update current_timestamp
                    )
                    """);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize user store schema", ex);
        }
    }

    private void ensureDefaultUserInDatabase() {
        if (userExists(configUsername)) {
            return;
        }
        saveUser(configUsername, passwordEncoder.encode(configPassword));
        logger.info("Seeded default user into database: {}", configUsername);
    }

    private Connection openConnection() throws SQLException {
        AppProperties.UserStore userStore = appProperties.getUserStore();
        return DriverManager.getConnection(
                userStore.getJdbcUrl(),
                userStore.getUsername(),
                userStore.getPassword()
        );
    }
}
