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
 * Implementazione concreta dell'interfaccia {@link UserDAO}.
 * Gestisce l'accesso diretto al database PostgreSQL per le operazioni relative agli utenti,
 * occupandosi dell'autenticazione sicura e della persistenza dei profili.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class UserDAOImpl implements UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());

    /**
     * Inizializza l'implementazione del Data Access Object per gli utenti.
     */
    public UserDAOImpl() {
        // Costruttore di default
    }

    /**
     * {@inheritDoc}
     * Esegue il controllo della password confrontando l'input con l'hash memorizzato
     * tramite le utilità di sicurezza del sistema.
     *
     * @param username Il nome utente fornito.
     * @param password La password in chiaro fornita.
     * @return L'oggetto {@link User} popolato se le credenziali sono corrette;
     * {@code null} in caso di mancata corrispondenza o utente inesistente.
     * @throws SQLException Se si verifica un errore durante l'esecuzione della query.
     */
    @Override
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, password FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
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
        return null;
    }

    /**
     * {@inheritDoc}
     * Genera l'hash della password prima di procedere con l'inserimento fisico nel database.
     *
     * @param user L'oggetto {@link User} da registrare.
     * @return {@code true} se la registrazione ha avuto successo;
     * {@code false} se l'inserimento non ha prodotto modifiche nel database.
     * @throws SQLException Se si verifica un errore SQL, inclusa la violazione di univocità dello username.
     */
    @Override
    public boolean registerUser(User user) throws SQLException {
        String sql = "INSERT INTO users(username, password) VALUES (?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());

            String hashedPassword = util.SecurityUtils.hashPassword(user.getPassword());
            pstmt.setString(2, hashedPassword);

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            throw new SQLException("Errore SQL durante la registrazione utente: " + user.getUsername(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param username Il nome utente da controllare.
     * @return {@code true} se lo username è già registrato;
     * {@code false} se non è stata trovata alcuna corrispondenza.
     * @throws SQLException Se si verifica un errore durante l'interrogazione del database.
     */
    @Override
    public boolean userExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * {@inheritDoc}
     * Limita i risultati restituiti per ottimizzare le prestazioni della ricerca.
     *
     * @param query           La stringa di ricerca parziale.
     * @param excludeUsername Il nome utente da escludere dai risultati.
     * @return Una lista di username corrispondenti ai criteri. Restituisce una lista vuota (<b>mai null</b>) in caso di errore o assenza di match.
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
            LOGGER.log(Level.SEVERE, e, () -> "Errore durante la ricerca utenti con query: " + query);
        }
        return users;
    }
}