// File: src/main/java/swimming/db/Db.java.
package swimming.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public final class Db {
    private static volatile DataSource DS;

    private Db() {}

    /** Obtiene conexión, asegura esquema (sin inserts), y deja autoCommit=false.
     * @return  */
    public static Connection getConnection() {
        try {
            Connection c = getDataSource().getConnection();
            c.setAutoCommit(false);
            ensureSchema(c);
            c.commit();
            return c;
        } catch (SQLException e) {
            throw new RuntimeException(
                "No se pudo abrir conexión MySQL. Revisa db.properties (url/user/password) y que MySQL esté arriba.",
                e
            );
        }
    }

    // --- DataSource (HikariCP) ---

    private static DataSource getDataSource() {
        if (DS == null) {
            synchronized (Db.class) {
                if (DS == null) DS = initDataSource();
            }
        }
        return DS;
    }

    /** Inicializa Hikari con propiedades de resources y/o variables de entorno. */
    private static DataSource initDataSource() {
        Properties p = new Properties();
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {
            // continuar con defaults/env
        }

        String url  = envOr("DB_URL",  p.getProperty("url",
                "jdbc:mysql://localhost:3306/library?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
        String user = envOr("DB_USER", p.getProperty("user", "SAMESCOBAR23"));
        String pass = envOr("DB_PASSWORD", p.getProperty("password", "CB994719s"));

        int pool;
        try {
            pool = Integer.parseInt(envOr("DB_POOL", p.getProperty("maximumPoolSize", "5")));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("DB_POOL/maximumPoolSize no es un número válido.", nfe);
        }

        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setMaximumPoolSize(pool);
            cfg.setPoolName("LibraryPool");
            cfg.addDataSourceProperty("cachePrepStmts", "true");
            cfg.addDataSourceProperty("prepStmtCacheSize", "250");
            cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            return new HikariDataSource(cfg);
        } catch (RuntimeException e) { // Hikari lanza unchecked
            throw new RuntimeException("Error configurando el DataSource (revisa URL/credenciales de MySQL).", e);
        }
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : def;
    }

    // --- Esquema (sin text blocks) ---

    /** Crea tabla e índices si faltan. Sin datos demo. */
    private static void ensureSchema(Connection conn) throws SQLException {
        // Tabla
        try (Statement st = conn.createStatement()) {
            String ddl = ""
                + "CREATE TABLE IF NOT EXISTS books ("
                + "  id INT AUTO_INCREMENT PRIMARY KEY,"
                + "  title VARCHAR(255) NOT NULL,"
                + "  author VARCHAR(255) NOT NULL,"
                + "  year INT NOT NULL,"
                + "  favorite TINYINT(1) NOT NULL DEFAULT 0"
                + ")";
            st.execute(ddl);
        }

        // Índices (compatibles, sin IF NOT EXISTS)
        createIndexIfMissing(conn, "books", "idx_books_favorite", "favorite");
        createIndexIfMissing(conn, "books", "idx_books_title", "title");
        createIndexIfMissing(conn, "books", "idx_books_author", "author");
    }

    /** Agrega índice solo si falta (consulta INFORMATION_SCHEMA). */
    private static void createIndexIfMissing(Connection conn, String table, String index, String col) throws SQLException {
        String check = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                     + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, index);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (Statement st = conn.createStatement()) {
                        st.execute("ALTER TABLE " + table + " ADD INDEX " + index + " (" + col + ")");
                    }
                }
            }
        }
    }
}
