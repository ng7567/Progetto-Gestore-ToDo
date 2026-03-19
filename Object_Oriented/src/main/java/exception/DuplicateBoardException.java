package exception;

/**
 * Rappresenta l'eccezione sollevata quando si tenta di creare o rinominare una bacheca
 * utilizzando un titolo testuale già esistente per il medesimo utente.
 * Garantisce l'applicazione della regola di business che impone l'univocità
 * dei nomi delle bacheche all'interno dello spazio di lavoro personale.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see exception.BusinessLogicException
 */
public class DuplicateBoardException extends BusinessLogicException {

    /**
     * Inizializza l'eccezione generando automaticamente un messaggio di errore descrittivo
     * che espone all'utente il titolo della bacheca in conflitto.
     *
     * @param boardTitle Il titolo testuale della bacheca che ha causato la violazione del vincolo di univocità.
     */
    public DuplicateBoardException(String boardTitle) {
        super("Hai già una bacheca chiamata \"" + boardTitle + "\". Scegli un nome diverso.");
    }
}