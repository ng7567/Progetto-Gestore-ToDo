package dao;

import model.Board;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaccia che definisce le operazioni di accesso ai dati per le bacheche (Board).
 * Gestisce la creazione, lettura, aggiornamento ed eliminazione delle bacheche,
 * inclusi i controlli specifici per le logiche di condivisione.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 * @see dao.impl.BoardDAOImpl
 */
public interface BoardDAO {

    /**
     * Inserisce una nuova bacheca nel database.
     *
     * @param board L'oggetto {@link Board} contenente i dati da persistere.
     * @return {@code true} se l'inserimento è avvenuto con successo nel database;
     * {@code false} se l'operazione è fallita (es. errore tecnico o validazione dati non superata).
     */
    boolean createBoard(Board board);

    /**
     * Recupera la lista di tutte le bacheche associate a uno specifico utente.
     *
     * @param userId L'identificativo univoco dell'utente.
     * @return Una lista di oggetti {@link Board} appartenenti all'utente specificato.
     * Se l'utente non possiede bacheche, restituisce una lista vuota (<b>mai null</b>).
     */
    List<Board> getBoardsByUser(int userId);

    /**
     * Aggiorna il titolo e/o la descrizione di una bacheca esistente identificata dal suo ID.
     * L'operazione può sollevare un'eccezione SQL se vengono violati vincoli di integrità o trigger.
     *
     * @param boardId        L'identificativo della bacheca da modificare.
     * @param newTitle       Il nuovo titolo da assegnare alla bacheca.
     * @param newDescription La nuova descrizione da assegnare alla bacheca.
     * @return {@code true} se l'aggiornamento modifica almeno una riga, {@code false} altrimenti.
     * @throws SQLException Se si verifica un errore durante l'esecuzione della query o se un trigger blocca l'operazione.
     */
    boolean updateBoard(int boardId, String newTitle, String newDescription) throws SQLException;

    /**
     * Elimina definitivamente una bacheca dal database.
     * Grazie ai vincoli CASCADE del database, eliminerà automaticamente anche tutti i task in essa contenuti.
     *
     * @param boardId L'identificativo della bacheca da eliminare.
     * @return {@code true} se la bacheca è stata eliminata correttamente;
     * {@code false} se nessun record è stato trovato con l'ID specificato.
     */
    boolean deleteBoard(int boardId);

    /**
     * Verifica se una bacheca è bloccata per la rinomina.
     * Il blocco si verifica tipicamente quando la bacheca contiene task condivisi
     * con altri utenti, richiedendo che il nome rimanga sincronizzato tra tutti i collaboratori.
     *
     * @param boardTitle    Il titolo della bacheca da verificare.
     * @param currentUserId L'ID dell'utente corrente per verificare le sue condivisioni.
     * @return {@code true} se la bacheca è bloccata (non rinominabile), {@code false} altrimenti.
     */
    boolean isBoardLocked(String boardTitle, int currentUserId);
}