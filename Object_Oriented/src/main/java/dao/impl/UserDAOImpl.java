package dao.impl;

import dao.UserDAO;
import util.DBConnection;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione concreta dell'interfaccia UserDAO.
 * Gestisce l'accesso diretto al database per le operazioni relative agli utenti,
 * garantendo la chiusura sicura delle risorse e delegando la sicurezza delle password.
 */
public class UserDAOImpl implements UserDAO {

    // Logger per tracciamento errori
    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());

    /**
     * Tenta di autenticare un utente verificando le credenziali nel database.
     * Utilizza funzioni di hash sicure (tramite SecurityUtils) per il controllo della password.
     *
     * @param username Il nome utente fornito.
     * @param password La password in chiaro fornita.
     * @return L'oggetto {@link User} se l'autenticazione ha successo, {@code null} altrimenti.
     * @throws SQLException Se si verifica un errore durante l'esecuzione della query.
     */
    @Override
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, password FROM users WHERE username = ?";

        // RISOLTO: Ora la Connection è dentro il try-with-resources
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // L'utente esiste, recupera l'hash e controlla la password
                    String storedHash = rs.getString("password");

                    if (util.SecurityUtils.checkPassword(password, storedHash)) {
                        return new User(
                                rs.getInt("user_id"),
                                rs.getString("username"),
                                storedHash
                        );
                    }
                }
            }
        }
        return null; // Credenziali errate o utente non trovato
    }

    /**
     * Registra un nuovo utente nel sistema salvando la password in formato hash.
     *
     * @param user L'oggetto {@link User} da registrare (con password in chiaro temporanea).
     * @return {@code true} se l'inserimento avviene con successo, {@code false} altrimenti.
     * @throws SQLException Se si verifica un errore SQL (incluso il duplicato dello username).
     */
    @Override
    public boolean registerUser(User user) throws SQLException {
        String sql = "INSERT INTO users(username, password) VALUES (?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());

            // Genera l'hash della password prima di salvarla nel DB
            String hashedPassword = util.SecurityUtils.hashPassword(user.getPassword());
            pstmt.setString(2, hashedPassword);

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            throw new SQLException("Errore SQL durante la registrazione utente: " + user.getUsername(), e);
        }
    }

    /**
     * Verifica l'esistenza di un nome utente nel database tramite query ottimizzata (SELECT 1).
     *
     * @param username Il nome utente da controllare.
     * @return {@code true} se l'utente esiste, {@code false} altrimenti.
     * @throws SQLException Se si verifica un errore SQL.
     */
    @Override
    public boolean userExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        // RISOLTO: Ora la Connection è dentro il try-with-resources
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Se c'è almeno un risultato, l'utente esiste
            }
        }
    }

    /**
     * Esegue una ricerca parziale (LIKE) sugli utenti, escludendo l'utente corrente.
     * Limita i risultati a 10 occorrenze per efficienza prestazionale.
     *
     * @param query           La stringa di ricerca parziale.
     * @param excludeUsername Il nome utente da escludere dai risultati.
     * @return Una lista di username trovati, o una lista vuota in caso di nessun risultato o errore.
     */
    @Override
    public List<String> searchUsers(String query, String excludeUsername) {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users WHERE username LIKE ? AND username != ? LIMIT 10";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + query + "%");
            pstmt.setString(2, excludeUsername);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            // Log con Lambda
            LOGGER.log(Level.SEVERE, e, () -> "Errore durante la ricerca utenti con query: " + query);
        }
        return users;
    }
}