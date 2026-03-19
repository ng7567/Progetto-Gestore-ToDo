package exception;

/**
 * Rappresenta l'eccezione sollevata quando si tenta di aggiungere un utente
 * alla lista dei collaboratori di un task (ToDo) al quale risulta già esplicitamente associato.
 * Garantisce l'integrità dei dati a livello applicativo, prevenendo duplicazioni
 * nelle relazioni di condivisione all'interno del sistema.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see exception.BusinessLogicException
 */
public class DuplicateCollaboratorException extends BusinessLogicException {

    /**
     * Inizializza l'eccezione generando automaticamente un messaggio di errore descrittivo
     * che notifica all'utente la preesistenza della collaborazione.
     *
     * @param username Il nome utente del collaboratore che ha causato la violazione del vincolo di univocità.
     */
    public DuplicateCollaboratorException(String username) {
        super("L'utente \"" + username + "\" collabora già a questo task.");
    }
}