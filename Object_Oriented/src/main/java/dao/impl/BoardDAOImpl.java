package dao.impl;

import dao.BoardDAO;
import util.DBConnection;
import model.Board;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione concreta dell'interfaccia BoardDAO.
 * Gestisce l'interazione diretta con la base di dati relazionale per tutte le operazioni
 * di persistenza relative alle entità di tipo Bacheca (Board).
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class BoardDAOImpl implements BoardDAO {

    private static final Logger LOGGER = Logger.getLogger(BoardDAOImpl.class.getName());

    /**
     * Inizializza l'implementazione del Data Access Object per le bacheche.
     */
    public BoardDAOImpl() {
        // Costruttore di default
    }

    /**
     * {@inheritDoc}
     * Inserisce un nuovo record rappresentante una bacheca nel database,
     * associandolo in modo univoco all'utente specificato.
     *
     * @param board L'oggetto Board contenente i dati da persistere.
     * @return {@code true} se l'inserimento nel database ha avuto successo;
     * {@code false} se si verifica un errore durante l'esecuzione della query.
     */
    @Override
    public boolean createBoard(Board board) {
        String sql = "INSERT INTO BOARDS (title, description, user_id) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, board.getTitle());
            pstmt.setString(2, board.getDescription() != null ? board.getDescription() : "");
            pstmt.setInt(3, board.getUserId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore durante la creazione della Board: " + board.getTitle());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * Recupera la lista completa delle bacheche appartenenti a un determinato utente.
     * I risultati vengono estratti indicando esplicitamente le colonne necessarie
     * e sono ordinati in modo crescente in base all'identificativo della bacheca.
     *
     * @param userId L'identificativo univoco dell'utente proprietario.
     * @return Una lista di oggetti Board associati all'utente. Restituisce una lista vuota (<b>mai null</b>) in caso di errore.
     */
    @Override
    public List<Board> getBoardsByUser(int userId) {
        List<Board> boards = new ArrayList<>();

        String sql = "SELECT board_id, title, description FROM BOARDS WHERE user_id = ? ORDER BY board_id ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("board_id");
                    String title = rs.getString("title");
                    String description = rs.getString("description");

                    boards.add(new Board(id, title, description, userId));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore durante il recupero delle Board per l'utente : " + userId);
        }

        return boards;
    }

    /**
     * {@inheritDoc}
     * Aggiorna il titolo e/o la descrizione di una bacheca esistente nel database.
     * L'eccezione SQL viene intenzionalmente propagata al livello superiore (Controller)
     * per consentire la corretta gestione di eventuali blocchi imposti dai trigger
     * del database (es. tentativo di rinominare una bacheca condivisa).
     *
     * @param boardId  L'identificativo univoco della bacheca da aggiornare.
     * @param newTitle Il nuovo titolo testuale da assegnare.
     * @param newDescription La nuova descrizione testuale da assegnare.
     * @return {@code true} se l'aggiornamento del record ha successo;
     * {@code false} se il record non esiste.
     * @throws SQLException Se si verifica un errore SQL a livello di connessione o se un trigger blocca l'operazione.
     */
    @Override
    public boolean updateBoard(int boardId, String newTitle, String newDescription) throws SQLException {
        String sql = "UPDATE boards SET title = ?, description = ? WHERE board_id = ?";

        // L'eccezione non viene catturata localmente per delegarne la gestione logica al Controller
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newTitle);
            pstmt.setString(2, newDescription);
            pstmt.setInt(3, boardId);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * {@inheritDoc}
     * Rimuove definitivamente una bacheca dal database tramite il suo identificativo.
     * L'operazione presuppone che il database sia configurato con vincoli di tipo ON DELETE CASCADE,
     * garantendo così la rimozione automatica e coerente di tutti i task associati.
     *
     * @param boardId L'identificativo univoco della bacheca da eliminare.
     * @return {@code true} se l'eliminazione del record ha avuto successo;
     * {@code false} se l'ID specificato non esiste o si verifica un errore.
     */
    @Override
    public boolean deleteBoard(int boardId) {
        String sql = "DELETE FROM boards WHERE board_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, boardId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore durante l'eliminazione della Board ID: " + boardId);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * Verifica la presenza di restrizioni che impediscono la modifica di una bacheca.
     * Nello specifico, accerta se esistono task condivisi con l'utente corrente
     * situati all'interno di bacheche aventi il medesimo titolo di quella interrogata.
     *
     * @param boardTitle    Il titolo testuale della bacheca da sottoporre a verifica.
     * @param currentUserId L'identificativo dell'utente corrente (che opera come collaboratore).
     * @return {@code true} se sono presenti task condivisi che inibiscono la modifica;
     * {@code false} altrimenti.
     */
    @Override
    public boolean isBoardLocked(String boardTitle, int currentUserId) {
        String sql = "SELECT COUNT(*) FROM shared_todos st " +
                "JOIN todos t ON st.todo_id = t.todo_id " +
                "JOIN boards b ON t.board_id = b.board_id " +
                "WHERE b.title = ? " +
                "AND st.user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, boardTitle);
            pstmt.setInt(2, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore durante il controllo del blocco per la Board: " + boardTitle);
        }
        return false;
    }
}