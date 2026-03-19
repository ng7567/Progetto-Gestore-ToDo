package exception;

/**
 * Rappresenta un'eccezione personalizzata sollevata in caso di violazione delle regole di business
 * dell'applicazione (es. violazioni di vincoli logici, trigger del database o
 * operazioni non consentite su dati condivisi).
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class BusinessLogicException extends Exception {

    /**
     * Inizializza l'eccezione specificando esclusivamente un messaggio di errore testuale.
     *
     * @param message Il messaggio descrittivo dell'errore.
     */
    @SuppressWarnings("unused")
    public BusinessLogicException(String message) {
        super(message);
    }

    /**
     * Inizializza l'eccezione specificando un messaggio di errore e incapsulando
     * l'eccezione originaria (pattern dell'exception chaining).
     *
     * @param message Il messaggio testuale descrittivo dell'errore.
     * @param cause   L'eccezione originale (es. {@link java.sql.SQLException}) che ha scatenato il problema.
     */
    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}