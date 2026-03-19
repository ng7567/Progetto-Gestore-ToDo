package exception;

/**
 * Rappresenta l'eccezione sollevata qualora un'operazione di business tenti di coinvolgere,
 * interrogare o associare un utente che non risulta registrato all'interno del database.
 * Evita che l'applicazione tenti di risolvere relazioni inesistenti (ad esempio durante
 * l'aggiunta di un nuovo collaboratore) e fornendo un feedback immediato e guidato all'interfaccia grafica.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see exception.BusinessLogicException
 */
public class UserNotFoundException extends BusinessLogicException {

    /**
     * Inizializza l'eccezione assemblando dinamicamente un messaggio di errore esplicito
     * e orientato all'utente finale. Il testo include direttamente l'input errato per facilitare
     * l'identificazione e la correzione immediata del refuso visivo.
     *
     * @param username Il nome testuale dell'utente ricercato che non ha prodotto alcuna corrispondenza nel sistema.
     */
    public UserNotFoundException(String username) {
        super("L'utente \"" + username + "\" non esiste. Verifica di aver scritto correttamente il nome.");
    }
}