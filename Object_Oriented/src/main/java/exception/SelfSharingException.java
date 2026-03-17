package exception;

/**
 * Eccezione lanciata quando il creatore di un ToDo tenta di aggiungere
 * se stesso alla lista dei collaboratori condivisi.
 */
public class SelfSharingException extends BusinessLogicException {

    /**
     * Costruisce una nuova SelfSharingException con un messaggio predefinito.
     */
    public SelfSharingException() {
        super("Non puoi condividere un task con te stesso. Sei già il proprietario!");
    }
}