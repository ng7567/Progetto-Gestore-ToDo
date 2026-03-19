package exception;

/**
 * Rappresenta l'eccezione sollevata durante il processo di autenticazione qualora
 * le credenziali fornite dall'utente non risultino valide o non corrispondano ad alcun account.
 * Garantisce una gestione centralizzata dei fallimenti di login, permettendo al livello
 * di business (Controller) di interrompere il flusso di accesso e notificare l'interfaccia grafica.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see exception.BusinessLogicException
 */
public class InvalidCredentialsException extends BusinessLogicException {

    /**
     * Inizializza l'eccezione assegnando un messaggio di errore generico e predefinito.
     * Costituisce la scelta raccomandata per ragioni di sicurezza: restituendo un messaggio
     * ambiguo ("Username o password non validi"), previene attacchi di tipo "username enumeration",
     * impedendo a un malintenzionato di scoprire se uno specifico username esiste nel sistema.
     */
    public InvalidCredentialsException() {
        super("Username o password non validi. Controlla le tue credenziali e riprova.");
    }

    /**
     * Inizializza l'eccezione assegnando un messaggio di errore esplicito e personalizzato.
     * Consente al livello chiamante di specificare la natura esatta dell'errore di autenticazione
     *
     * @param message Il messaggio testuale descrittivo che dettaglia la causa specifica del fallimento.
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}