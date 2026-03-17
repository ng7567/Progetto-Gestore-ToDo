package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestisce la creazione delle connessioni al database PostgreSQL.
 * Implementa il pattern Singleton per fornire un punto di accesso centralizzato,
 * ottimizzato per operare in sinergia con i costrutti try-with-resources dei DAO.
 */
public class DBConnection {

    // Costanti di configurazione del Database
    private static final String URL = "jdbc:postgresql://localhost:5432/todo?characterEncoding=UTF-8";
    private static final String USERNAME = "postgres";

    // La password viene recuperata dalle variabili d'ambiente per evitare credenziali hardcoded.
    // Per facilitare lo sviluppo locale, è stato mantenuto un fallback al valore di default.
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "root";

    private static DBConnection instance;

    /**
     * Costruttore privato del Singleton.
     * Il caricamento esplicito del driver è stato rimosso in quanto obsoleto.
     */
    private DBConnection() {
        // Nessun caricamento di stato necessario
    }

    /**
     * Restituisce l'istanza unica della classe {@link DBConnection} in modo thread-safe.
     *
     * @return L'istanza singleton di {@link DBConnection}.
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Genera e fornisce una nuova connessione attiva al database.
     * Progettato per essere chiamato all'interno dei blocchi try-with-resources
     * affinché il chiamante si occupi della chiusura sicura della risorsa.
     *
     * @return Un nuovo oggetto {@link Connection} pronto all'uso.
     * @throws SQLException Se la connessione al database fallisce. L'eccezione viene
     * propagata per demandare la gestione al livello superiore.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}