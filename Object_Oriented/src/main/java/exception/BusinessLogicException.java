package exception;

/**
 * Eccezione personalizzata per gestire errori relativi alle regole di business
 * dell'applicazione (es. violazioni di vincoli logici, trigger del database,
 * operazioni non consentite su dati condivisi).
 */
public class BusinessLogicException extends Exception {

    /**
     * Costruttore per un messaggio di errore semplice.
     *
     * @param message Il messaggio descrittivo dell'errore.
     */
    @SuppressWarnings("unused")
    public BusinessLogicException(String message) {
        super(message);
    }

    /**
     * Costruttore per un messaggio di errore concatenato a un'eccezione originale.
     *
     * @param message Il messaggio descrittivo.
     * @param cause   L'eccezione originale (es. SQLException) che ha causato il problema.
     */
    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}