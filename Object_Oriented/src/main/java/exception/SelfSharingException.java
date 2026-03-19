package exception;

/**
 * Rappresenta l'eccezione sollevata quando l'utente proprietario (creatore) di un task
 * tenta di inserire il proprio identificativo all'interno della lista dei collaboratori condivisi.
 * Garantisce la coerenza della logica di dominio, impedendo ridondanze relazionali
 * nel database e prevenendo conflitti applicativi nella risoluzione dei permessi
 * (ruolo OWNER vs SHARED) all'interno dell'interfaccia grafica.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see exception.BusinessLogicException
 */
public class SelfSharingException extends BusinessLogicException {

    /**
     * Inizializza l'eccezione assegnando automaticamente un messaggio di errore
     * predefinito e user-friendly. Questo messaggio è concepito per poter essere
     * mostrato direttamente all'utente finale tramite i dialoghi di notifica della UI,
     * spiegando in modo chiaro l'incongruenza dell'operazione richiesta.
     */
    public SelfSharingException() {
        super("Non puoi condividere un task con te stesso. Sei già il proprietario!");
    }
}