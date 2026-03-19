package dao;

import model.ToDo;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaccia che definisce le operazioni di accesso ai dati per i task (ToDo).
 * Gestisce il ciclo di vita dei task, inclusi creazione, aggiornamento,
 * eliminazione e gestione delle condivisioni tra utenti.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see dao.impl.ToDoDAOImpl
 */
public interface ToDoDAO {

    /**
     * Inserisce un nuovo task nel database.
     *
     * @param todo L'oggetto {@link ToDo} contenente i dati da salvare.
     * @return {@code true} se l'inserimento è avvenuto con successo nel database;
     * {@code false} se l'operazione è fallita o i dati non sono validi.
     */
    boolean createToDo(ToDo todo);

    /**
     * Aggiorna lo stato di completamento di un task (fatto/non fatto).
     *
     * @param todoId      L'identificativo univoco del task.
     * @param isCompleted Il nuovo stato di completamento ({@code true} per completato).
     * @return {@code true} se lo stato è stato aggiornato correttamente;
     * {@code false} se nessun task corrisponde all'ID fornito.
     */
    boolean toggleCompletion(int todoId, boolean isCompleted);

    /**
     * Elimina fisicamente un task dal database.
     * Questa operazione rimuove anche le associazioni con i collaboratori
     * grazie ai vincoli di CASCADE del database.
     *
     * @param todoId L'identificativo del task da eliminare.
     * @return {@code true} se il task è stato eliminato con successo;
     * {@code false} se il task non esiste nel database.
     */
    boolean deleteToDo(int todoId);

    /**
     * Calcola la prossima posizione disponibile per l'ordinamento dei task in una bacheca.
     * Utile per inserire nuovi task in fondo alla lista.
     *
     * @param boardId L'identificativo della bacheca.
     * @return Un intero progressivo rappresentante la nuova posizione in coda (massima posizione attuale + 1).
     */
    int getNextPosition(int boardId);

    /**
     * Aggiorna tutti i dati di un task esistente (titolo, descrizione, scadenza, priorità, ecc.).
     *
     * @param todo L'oggetto {@link ToDo} con i dati aggiornati.
     * @return {@code true} se l'aggiornamento modifica effettivamente il record;
     * {@code false} se l'ID del task non viene trovato.
     * @throws SQLException Se si verifica un errore nel database o se un vincolo di
     * integrità (es. data di scadenza non valida) viene violato.
     */
    boolean updateToDo(ToDo todo) throws SQLException;

    /**
     * Sposta un task da una bacheca all'altra aggiornando il riferimento al relativo ID della bacheca.
     *
     * @param todoId        L'identificativo del task da spostare.
     * @param targetBoardId L'identificativo della bacheca di destinazione.
     * @param currentUserId L'ID dell'utente proprietario che esegue lo spostamento.
     * @return {@code true} se lo spostamento ha successo;
     * {@code false} se l'operazione fallisce o l'utente non ha i permessi necessari.
     */
    boolean updateBoardId(int todoId, int targetBoardId, int currentUserId);

    /**
     * Recupera la lista dei task associati a una bacheca tramite il suo titolo.
     * Include sia i task proprietari che quelli condivisi visibili all'utente corrente.
     *
     * @param boardTitle    Il titolo testuale della bacheca.
     * @param currentUserId L'ID dell'utente che richiede i dati.
     * @return Una lista di oggetti {@link ToDo}. Restituisce una lista vuota (<b>mai null</b>) se non ci sono task.
     */
    List<ToDo> getTodosByBoardTitle(String boardTitle, int currentUserId);

    /**
     * Recupera il percorso del file immagine associato a un task, se presente.
     * Utile per eliminare il file fisico dal disco quando si cancella il task dal database.
     *
     * @param todoId L'identificativo del task.
     * @return Il percorso locale del file come stringa, oppure {@code null} se non esiste o non è impostato.
     */
    String getImagePath(int todoId);

    /**
     * Rimuove l'associazione di condivisione tra un utente collaboratore e un task.
     * Non elimina il task fisico dal database, ma rimuove solo la partecipazione dell'utente.
     *
     * @param todoId L'identificativo del task.
     * @param userId L'ID dell'utente che abbandona la condivisione.
     * @return {@code true} se la rimozione ha successo;
     * {@code false} se l'associazione non esiste o si verifica un errore.
     */
    boolean leaveTodo(int todoId, int userId);

    /**
     * Recupera un singolo oggetto ToDo tramite il suo identificativo univoco.
     *
     * @param todoId L'identificativo del task.
     * @return L'oggetto {@link ToDo} trovato, oppure {@code null} se il task non esiste nel database.
     */
    ToDo getTodoById(int todoId);

    /**
     * Recupera la lista degli username dei collaboratori associati a un task specifico.
     *
     * @param todoId L'identificativo del task.
     * @return Una lista di stringhe contenente i nomi utenti dei collaboratori. Restituisce una lista vuota (<b>mai null</b>) se non ci sono collaboratori.
     */
    List<String> getCollaborators(int todoId);

    /**
     * Aggiorna la posizione numerica di un task all'interno della sua bacheca.
     * Utilizzato per gestire il riordinamento (es. tramite Drag &amp; Drop visivo).
     *
     * @param todoId      L'identificativo del task da spostare.
     * @param newPosition Il nuovo valore dell'indice di ordinamento.
     * @param role        Il ruolo dell'utente che esegue l'operazione (es. OWNER o SHARED).
     * @param userId      L'ID dell'utente che esegue l'operazione.
     * @return {@code true} se l'aggiornamento della posizione ha successo;
     * {@code false} altrimenti.
     */
    boolean updatePosition(int todoId, int newPosition, String role, int userId);
}