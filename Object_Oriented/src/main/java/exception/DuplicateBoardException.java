package exception;

/**
 * Eccezione lanciata quando si tenta di creare o rinominare una bacheca
 * utilizzando un titolo già esistente per lo stesso utente.
 */
public class DuplicateBoardException extends BusinessLogicException {

    /**
     * Costruisce una nuova DuplicateBoardException indicando il nome duplicato.
     *
     * @param boardTitle Il titolo della bacheca che ha causato il conflitto.
     */
    public DuplicateBoardException(String boardTitle) {
        super("Hai già una bacheca chiamata \"" + boardTitle + "\". Scegli un nome diverso.");
    }
}