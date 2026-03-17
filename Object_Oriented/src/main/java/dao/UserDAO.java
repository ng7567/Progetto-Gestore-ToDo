package dao;

import model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaccia che definisce le operazioni di accesso ai dati per gli Utenti.
 * Gestisce l'autenticazione, la registrazione e la ricerca degli utenti nel sistema.
 */
public interface UserDAO {

    /**
     * Tenta di autenticare un utente verificando le credenziali fornite.
     *
     * @param username Il nome utente.
     * @param password La password fornita (in chiaro).
     * @return L'oggetto {@link User} popolato se le credenziali sono corrette, {@code null} altrimenti.
     * @throws SQLException Se si verifica un errore di connessione o di esecuzione della query.
     */
    User authenticate(String username, String password) throws SQLException;

    /**
     * Registra un nuovo utente nel sistema salvando la password in formato hash.
     *
     * @param user L'oggetto {@link User} da registrare.
     * @return {@code true} se l'inserimento avviene con successo, {@code false} altrimenti.
     * @throws SQLException Se si verifica un errore SQL (incluso il tentativo di inserire uno username duplicato).
     */
    boolean registerUser(User user) throws SQLException;

    /**
     * Verifica se un determinato nome utente esiste già nel database.
     * Utile in fase di registrazione o login per controlli preliminari di validazione.
     *
     * @param username Il nome utente da verificare.
     * @return {@code true} se il nome utente è già presente, {@code false} altrimenti.
     * @throws SQLException Se si verifica un errore durante l'interrogazione del database.
     */
    boolean userExists(String username) throws SQLException;

    /**
     * Cerca utenti nel database il cui username corrisponde parzialmente alla query fornita (match tramite LIKE).
     * Esclude dai risultati un nome utente specifico (tipicamente l'utente che sta effettuando la ricerca).
     *
     * @param query           La stringa di ricerca parziale.
     * @param excludeUsername Il nome utente da escludere dai risultati (es. l'utente corrente).
     * @return Una lista di stringhe contenente gli username trovati. Restituisce una lista vuota se non ci sono corrispondenze.
     */
    List<String> searchUsers(String query, String excludeUsername);
}