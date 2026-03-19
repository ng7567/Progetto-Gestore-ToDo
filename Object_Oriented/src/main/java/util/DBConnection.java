package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestisce la creazione delle connessioni al database PostgreSQL.
 * Usa il pattern Singleton per offrire un punto di accesso unico, pensato per
 * lavorare bene in combinazione con i blocchi try-with-resources all'interno dei DAO.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class DBConnection {

    /** Indirizzo di connessione al database PostgreSQL locale. */
    private static final String URL = "jdbc:postgresql://localhost:5432/todo?characterEncoding=UTF-8";

    /** Nome utente predefinito per l'accesso al database. */
    private static final String USERNAME = "postgres";

    /**
     * Password per l'accesso.
     * Viene letta dalle variabili d'ambiente per sicurezza, ma usa "root" come
     * valore di default per facilitare i test in locale.
     */
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "root";

    /** L'unica istanza attiva della classe creata dal pattern Singleton. */
    private static DBConnection instance;

    /**
     * Costruttore privato per impedire la creazione di oggetti dall'esterno.
     * Il caricamento manuale del driver è stato rimosso perché non è più
     * necessario nelle versioni moderne di Java (JDBC 4.0+).
     */
    private DBConnection() {
        // Nessun caricamento di stato necessario
    }

    /**
     * Restituisce l'unica istanza disponibile della classe in modo thread-safe.
     * Se l'istanza non esiste ancora, la crea.
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
     * Crea e restituisce una nuova connessione attiva al database.
     * È pensato per essere usato all'interno di un blocco try-with-resources,
     * in modo che chi chiama il metodo si occupi in automatico di chiudere la connessione.
     *
     * @return Un nuovo oggetto {@link Connection} pronto all'uso.
     * @throws SQLException Se la connessione al database fallisce. L'errore viene
     * passato a chi chiama il metodo per gestirlo nel modo corretto.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}