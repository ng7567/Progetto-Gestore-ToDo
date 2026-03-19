package exception;

/**
 * Rappresenta l'eccezione sollevata durante la fase di registrazione qualora si tenti
 * di inserire nel sistema un nome utente (username) già assegnato a un altro account.
 * Garantisce il rispetto del vincolo di univocità imposto dal database, prevenendo
 * la creazione di profili ambigui o duplicati e permettendo al livello di presentazione (GUI)
 * di intercettare l'anomalia per richiedere all'utente un input alternativo.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see exception.BusinessLogicException
 */
public class UserAlreadyExistsException extends BusinessLogicException {

    /**
     * Inizializza l'eccezione assemblando dinamicamente un messaggio di errore
     * esplicito, includendo direttamente nel testo l'username che ha generato il conflitto.
     *
     * @param username Il nome testuale dell'utente risultato non disponibile durante il controllo.
     */
    public UserAlreadyExistsException(String username) {
        super("L'username '" + username + "' è già occupato. Scegli un nome diverso.");
    }
}