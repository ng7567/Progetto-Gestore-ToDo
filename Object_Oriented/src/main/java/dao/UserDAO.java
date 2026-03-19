package dao;

import model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaccia che definisce le operazioni di accesso ai dati per gli utenti.
 * Gestisce l'autenticazione, la registrazione e la ricerca degli utenti all'interno del sistema.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see dao.impl.UserDAOImpl
 */
public interface UserDAO {

    /**
     * Tenta di autenticare un utente verificando la corrispondenza delle credenziali fornite.
     *
     * @param username Il nome utente da verificare.
     * @param password La password fornita (in chiaro).
     * @return L'oggetto {@link User} popolato se le credenziali sono corrette;
     * {@code null} se l'utente non esiste o la password è errata.
     * @throws SQLException Se si verifica un errore di connessione o di esecuzione della query sul database.
     */
    User authenticate(String username, String password) throws SQLException;

    /**
     * Registra un nuovo utente nel database, occupandosi di salvare la password in formato hash
     * per ragioni di sicurezza.
     *
     * @param user L'oggetto {@link User} contenente i dati da registrare.
     * @return {@code true} se l'inserimento è avvenuto con successo;
     * {@code false} se l'operazione è fallita (es. validazione non superata).
     * @throws SQLException Se si verifica un errore SQL, incluso il tentativo di inserire uno username già esistente (violazione vincolo UNIQUE).
     */
    boolean registerUser(User user) throws SQLException;

    /**
     * Verifica se un determinato nome utente esiste già nel database.
     * Utile in fase di registrazione o login per controlli preliminari di validazione.
     *
     * @param username Il nome utente da verificare.
     * @return {@code true} se il nome utente è già presente nel sistema;
     * {@code false} se l'username è disponibile.
     * @throws SQLException Se si verifica un errore durante l'interrogazione del database.
     */
    boolean userExists(String username) throws SQLException;

    /**
     * Cerca utenti nel database il cui username corrisponde parzialmente alla query fornita (ricerca tramite operatore LIKE).
     * Esclude automaticamente dai risultati un nome utente specifico (tipicamente l'utente che sta effettuando la ricerca).
     *
     * @param query           La stringa di ricerca parziale.
     * @param excludeUsername Il nome utente da escludere esplicitamente dai risultati.
     * @return Una lista di stringhe contenente gli username trovati. Restituisce una lista vuota (<b>mai null</b>) se non ci sono corrispondenze.
     */
    List<String> searchUsers(String query, String excludeUsername);
}