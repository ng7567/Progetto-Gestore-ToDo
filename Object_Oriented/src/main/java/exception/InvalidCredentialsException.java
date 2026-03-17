package exception;

/**
 * Eccezione sollevata quando le credenziali di accesso fornite dall'utente
 * non risultano valide o non corrispondono ad alcun account registrato.
 * Permette di specificare messaggi di errore dettagliati per distinguere
 * tra errori di username e di password.
 */
public class InvalidCredentialsException extends BusinessLogicException {

    /**
     * Costruisce una nuova eccezione con un messaggio di errore predefinito.
     */
    public InvalidCredentialsException() {
        super("Username o password non validi. Controlla le tue credenziali e riprova.");
    }

    /**
     * Costruisce una nuova eccezione con un messaggio di errore personalizzato.
     * Consente al chiamante di specificare la natura esatta dell'errore di autenticazione.
     *
     * @param message Il messaggio descrittivo dell'errore.
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}