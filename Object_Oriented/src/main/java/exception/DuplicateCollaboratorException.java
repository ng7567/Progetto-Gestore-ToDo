package exception;

/**
 * Eccezione lanciata quando si tenta di aggiungere un collaboratore
 * a un task in cui è già presente.
 */
public class DuplicateCollaboratorException extends BusinessLogicException {

    /**
     * Costruisce una nuova DuplicateCollaboratorException.
     *
     * @param username L'username del collaboratore già presente.
     */
    public DuplicateCollaboratorException(String username) {
        super("L'utente \"" + username + "\" collabora già a questo task.");
    }
}