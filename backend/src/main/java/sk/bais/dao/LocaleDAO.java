package sk.bais.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.bais.model.Locale;
import sk.bais.util.DatabaseConnection;

/**
 * DAO trieda pre tabulku locale.
 * Locale zaznamy su spravovane manualnie (POWER_USER) — nie su mazane ani menene bezne.
 */
public class LocaleDAO {

    private static final Logger log = LoggerFactory.getLogger(LocaleDAO.class);

    private static final String SQL_LIST =
            "SELECT code, name, is_default FROM locale ORDER BY is_default DESC, code";

    private static final String SQL_GET_BY_CODE =
            "SELECT code, name, is_default FROM locale WHERE code = ?";

    private static final String SQL_GET_DEFAULT =
            "SELECT code, name, is_default FROM locale WHERE is_default = TRUE LIMIT 1";

    private static final String SQL_INSERT =
            "INSERT INTO locale (code, name, is_default) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE locale SET name = ?, is_default = ? WHERE code = ?";

    private static final String SQL_DELETE =
            "DELETE FROM locale WHERE code = ?";

    /**
     * Zoznam vsetkych podporovanych lokalit.
     */
    public List<Locale> list() throws SQLException {
        List<Locale> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        log.info("Nacitanych {} locale zaznamov", list.size());
        return list;
    }

    /**
     * Najde lokalitu podla kodu (napr. 'sk', 'en').
     */
    public Optional<Locale> getByCode(String code) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_BY_CODE)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    log.debug("Najdena locale code={}", code);
                    return Optional.of(mapRow(rs));
                }
            }
        }
        log.debug("Locale code={} nenajdena", code);
        return Optional.empty();
    }

    /**
     * Vrati vychodziacu (default) lokalitu.
     */
    public Optional<Locale> getDefault() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_DEFAULT);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    /**
     * Prida novu lokalitu.
     */
    public Locale create(Locale locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setString(1, locale.getCode());
            stmt.setString(2, locale.getName());
            stmt.setBoolean(3, locale.isDefault());
            stmt.executeUpdate();
        }
        log.info("Vytvorena locale code={}", locale.getCode());
        return locale;
    }

    /**
     * Upravi existujucu lokalitu.
     */
    public boolean update(Locale locale) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, locale.getName());
            stmt.setBoolean(2, locale.isDefault());
            stmt.setString(3, locale.getCode());
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) log.info("Locale code={} uspesne upravena", locale.getCode());
            return updated;
        }
    }

    /**
     * Vymaze lokalitu podla kodu.
     * Pozor: DB RESTRICT brani vymazaniu ak existuju preklady viazane na tuto lokalitu.
     */
    public boolean delete(String code) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setString(1, code);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) log.info("Vymazana locale code={}", code);
            return deleted;
        }
    }

    /**
     * Mapovanie ResultSet na objekt Locale.
     */
    private Locale mapRow(ResultSet rs) throws SQLException {
        return new Locale(
                rs.getString("code"),
                rs.getString("name"),
                rs.getBoolean("is_default")
        );
    }
}
