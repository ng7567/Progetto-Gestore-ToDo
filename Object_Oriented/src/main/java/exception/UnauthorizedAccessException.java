package exception;

/**
 * Eccezione lanciata quando un utente tenta di eseguire un'operazione
 * per la quale non possiede i permessi necessari (es. spostare una bacheca altrui).
 */
public class UnauthorizedAccessException extends BusinessLogicException {

    /**
     * Costruisce una nuova UnauthorizedAccessException con un messaggio personalizzato.
     *
     * @param action L'azione che si è tentato di eseguire (es. "spostare questo task").
     */
    public UnauthorizedAccessException(String action) {
        super("Accesso negato: non hai i permessi necessari per " + action + ".");
    }
}