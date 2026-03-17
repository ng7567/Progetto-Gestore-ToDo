package exception;

/**
 * Eccezione lanciata quando si tenta di interagire con o cercare
 * un utente che non è presente nel database.
 */
public class UserNotFoundException extends BusinessLogicException {

    /**
     * Costruisce una nuova UserNotFoundException.
     *
     * @param username L'username dell'utente non trovato.
     */
    public UserNotFoundException(String username) {
        super("L'utente \"" + username + "\" non esiste. Verifica di aver scritto correttamente il nome.");
    }
}