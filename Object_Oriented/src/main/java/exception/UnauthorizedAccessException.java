package exception;

/**
 * Rappresenta l'eccezione sollevata qualora un utente tenti di eseguire un'operazione
 * per la quale non possiede i privilegi di accesso o i permessi necessari.
 * Garantisce l'applicazione delle politiche di autorizzazione del sistema
 * (come la distinzione tra i ruoli OWNER e SHARED), proteggendo le risorse
 * (ad esempio bacheche e task di altri utenti) da modifiche arbitrarie, accessi abusivi
 * o tentativi di violazione dei confini dello spazio di lavoro.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see exception.BusinessLogicException
 */
public class UnauthorizedAccessException extends BusinessLogicException {

    /**
     * Inizializza l'eccezione assemblando dinamicamente un messaggio di errore contestuale,
     * basato sull'operazione specifica che il sistema ha bloccato.
     * Questo approccio consente di restituire un feedback mirato e trasparente all'utente
     * tramite l'interfaccia grafica, chiarendo esattamente quale azione non è consentita.
     *
     * @param action La stringa testuale che descrive l'azione bloccata (es. "spostare questo task" o "rinominare la bacheca").
     */
    public UnauthorizedAccessException(String action) {
        super("Accesso negato: non hai i permessi necessari per " + action + ".");
    }
}