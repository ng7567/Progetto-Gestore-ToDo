package dao.impl;

import dao.ToDoDAO;
import util.DBConnection;
import model.Priority;
import model.ToDo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione concreta dell'interfaccia {@link ToDoDAO}.
 * Gestisce tutte le operazioni CRUD e le logiche di business (come lo spostamento
 * dell'ordine o la gestione dei collaboratori) interagendo in modo sicuro con il database relazionale.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class ToDoDAOImpl implements ToDoDAO {

    private static final Logger LOGGER = Logger.getLogger(ToDoDAOImpl.class.getName());

    // --- COSTANTI PER I NOMI DELLE COLONNE ---
    private static final String COL_TODO_ID = "todo_id";
    private static final String COL_BOARD_ID = "board_id";
    private static final String COL_TITLE = "title";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_EXPIRY_DATE = "expiry_date";
    private static final String COL_PRIORITY = "priority";
    private static final String COL_IS_COMPLETED = "is_completed";
    private static final String COL_URL_LINK = "url_link";
    private static final String COL_IMAGE_PATH = "image_path";
    private static final String COL_BACKGROUND_COLOR = "background_color";
    private static final String COL_POSITION_ORDER = "position_order";
    private static final String COL_USERNAME = "username";
    private static final String COL_OWNER_USERNAME = "owner_username";
    private static final String COL_ROLE = "role";

    /**
     * Inizializza l'implementazione del Data Access Object per i task.
     */
    public ToDoDAOImpl() {
        // Costruttore di default necessario per la generazione del Javadoc
    }

    /**
     * {@inheritDoc}
     *
     * @param boardId L'identificativo della bacheca.
     * @return Il valore massimo dell'ordine attuale incrementato di 1. Restituisce 1 se la bacheca è vuota o in caso di errore SQL.
     */
    @Override
    public int getNextPosition(int boardId) {
        String sql = "SELECT MAX(position_order) FROM v_user_visible_todos WHERE board_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, boardId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) + 1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore nel calcolo della prossima posizione per board_id: " + boardId);
        }
        return 1;
    }

    /**
     * {@inheritDoc}
     * La transazione garantisce l'atomicità tra l'inserimento del record principale e
     * l'associazione degli eventuali collaboratori.
     *
     * @param todo L'oggetto ToDo contenente i dati da persistere.
     * @return {@code true} se la transazione viene completata con successo;
     * {@code false} se si verifica un errore SQL, causando un rollback automatico.
     */
    @Override
    public boolean createToDo(ToDo todo) {
        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Inizio transazione

            int newTodoId = insertTodoRecord(conn, todo);

            if (newTodoId > 0) {
                todo.setId(newTodoId);
                if (todo.getCollaborators() != null && !todo.getCollaborators().isEmpty()) {
                    assignCollaborators(conn, newTodoId, todo.getCollaborators());
                }
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante la transazione createToDo", e);
            rollbackSilently(conn);
            return false;
        } finally {
            restoreAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * Modifica i dati base e risincronizza la lista dei collaboratori eliminando
     * le vecchie associazioni e inserendo le nuove in un'unica transazione.
     *
     * @param todo L'oggetto ToDo con i dati aggiornati.
     * @return {@code true} se l'aggiornamento ha successo e la transazione viene confermata;
     * {@code false} in caso di errore generico con conseguente rollback.
     * @throws SQLException Se il database blocca l'operazione a causa della violazione di un check constraint (es. data di scadenza invalida).
     */
    @Override
    public boolean updateToDo(ToDo todo) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Inizio transazione

            updateTodoRecord(conn, todo);
            clearCollaborators(conn, todo.getId());

            if (todo.getCollaborators() != null && !todo.getCollaborators().isEmpty()) {
                assignCollaborators(conn, todo.getId(), todo.getCollaborators());
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            rollbackSilently(conn);

            if ("23514".equals(e.getSQLState())) {
                throw e;
            }

            LOGGER.log(Level.SEVERE, e, () -> "Errore aggiornamento ToDo ID: " + todo.getId());
            return false;
        } finally {
            restoreAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param todoId L'identificativo univoco del task.
     * @return {@code true} se l'eliminazione ha successo;
     * {@code false} se il record non esiste o si verifica un errore SQL.
     */
    @Override
    public boolean deleteToDo(int todoId) {
        String sql = "DELETE FROM todos WHERE todo_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, todoId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore cancellazione ToDo ID: " + todoId);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param todoId      L'identificativo del task.
     * @param isCompleted Il nuovo stato booleano di completamento.
     * @return {@code true} se l'aggiornamento ha successo;
     * {@code false} se il record non viene trovato o in caso di errore SQL.
     */
    @Override
    public boolean toggleCompletion(int todoId, boolean isCompleted) {
        String sql = "UPDATE todos SET is_completed = ? WHERE todo_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, isCompleted);
            pstmt.setInt(2, todoId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore toggle completamento per ToDo ID: " + todoId);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * Apre una nuova connessione al database per il recupero dei dati.
     *
     * @param todoId L'identificativo del task.
     * @return Una lista di username associati come collaboratori. Restituisce una lista vuota (<b>mai null</b>) in caso di errore SQL.
     */
    @Override
    public List<String> getCollaborators(int todoId) {
        List<String> collaborators = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            collaborators = getCollaboratorsForTodo(todoId, conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore connessione getCollaborators per ToDo ID: " + todoId);
        }
        return collaborators;
    }

    /**
     * {@inheritDoc}
     * L'operazione, gestita tramite un'unica transazione atomica, aggiorna la bacheca fisica
     * e la posizione per il proprietario, ricalcolando contemporaneamente l'ordine per i collaboratori.
     *
     * @param todoId        L'identificativo univoco del task da spostare.
     * @param targetBoardId L'identificativo della bacheca di destinazione.
     * @param currentUserId L'ID del proprietario che richiede ed esegue lo spostamento.
     * @return {@code true} se l'intera transazione viene completata con successo;
     * {@code false} in caso di errori SQL o fallimento dell'aggiornamento.
     */
    @Override
    public boolean updateBoardId(int todoId, int targetBoardId, int currentUserId) {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            return executeUpdateBoardTransaction(conn, todoId, targetBoardId, currentUserId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore nello spostamento della bacheca per ToDo ID: " + todoId);
            return false;
        }
    }

    /**
     * Esegue fisicamente la transazione SQL per lo spostamento della bacheca.
     * Gestisce l'aggiornamento del record del proprietario e propaga l'aggiornamento a cascata
     * sulle posizioni dei collaboratori, garantendo l'integrità relazionale dei dati tramite
     * commit esplicito o rollback in caso di fallimento.
     *
     * @param conn          La connessione al database attiva da utilizzare per l'operazione.
     * @param todoId        L'identificativo univoco del task da aggiornare.
     * @param targetBoardId L'identificativo della nuova bacheca di destinazione.
     * @param currentUserId L'ID dell'utente proprietario, necessario per calcolare correttamente il nuovo ordine visivo.
     * @return {@code true} se i record vengono aggiornati e la transazione viene confermata;
     * {@code false} se il task originale non viene trovato e la transazione viene annullata.
     * @throws SQLException Se si verifica un errore durante l'esecuzione delle query (esegue automaticamente il rollback prima di propagare l'eccezione).
     */
    private boolean executeUpdateBoardTransaction(Connection conn, int todoId, int targetBoardId, int currentUserId) throws SQLException {
        String sqlOwner = "UPDATE todos SET board_id = ?, " +
                "position_order = (SELECT COALESCE(MAX(position_order), 0) + 1 FROM v_user_visible_todos WHERE board_id = ? AND viewer_id = ?) " +
                "WHERE todo_id = ?";

        String sqlShared = "UPDATE shared_todos st SET " +
                "position_order = ( " +
                "  SELECT COALESCE(MAX(position_order), 0) + 1 " +
                "  FROM v_user_visible_todos v " +
                "  WHERE v.board_title = (SELECT title FROM boards WHERE board_id = ?) " +
                "    AND v.viewer_id = st.user_id " +
                ") " +
                "WHERE st.todo_id = ?";

        conn.setAutoCommit(false); // Disabilita l'invio automatico per gestire la transazione

        try (PreparedStatement psOwner = conn.prepareStatement(sqlOwner);
             PreparedStatement psShared = conn.prepareStatement(sqlShared)) {

            // Configurazione ed esecuzione per il proprietario
            psOwner.setInt(1, targetBoardId);
            psOwner.setInt(2, targetBoardId);
            psOwner.setInt(3, currentUserId);
            psOwner.setInt(4, todoId);

            if (psOwner.executeUpdate() > 0) {
                // Aggiornamento posizioni per i collaboratori
                psShared.setInt(1, targetBoardId);
                psShared.setInt(2, todoId);
                psShared.executeUpdate();

                conn.commit(); // Consolida le modifiche nel database
                return true;
            } else {
                conn.rollback(); // Annulla le modifiche se il task non viene trovato
                return false;
            }
        } catch (SQLException e) {
            conn.rollback(); // Annulla la transazione in caso di errore tecnico
            throw e;
        } finally {
            conn.setAutoCommit(true); // Ripristina lo stato predefinito della connessione
        }
    }

    /**
     * {@inheritDoc}
     * Interroga la vista unificata del database per estrarre sia i task proprietari sia quelli condivisi,
     * ordinandoli in base allo stato di completamento e all'indice di posizionamento visivo.
     *
     * @param boardTitle    Il titolo della bacheca target.
     * @param currentUserId L'ID dell'utente corrente per la verifica della visibilità.
     * @return Una lista di oggetti ToDo estratti dalla vista. Restituisce una lista vuota (<b>mai null</b>) in caso di errore SQL o assenza di task.
     */
    @Override
    public List<ToDo> getTodosByBoardTitle(String boardTitle, int currentUserId) {
        List<ToDo> todos = new ArrayList<>();

        String sql = "SELECT todo_id, title, description, expiry_date, priority, " +
                "is_completed, url_link, image_path, background_color, position_order, board_id, owner_username, role " +
                "FROM v_user_visible_todos " +
                "WHERE board_title = ? " +
                "AND viewer_id = ? " +
                "ORDER BY is_completed ASC, position_order ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, boardTitle);
            pstmt.setInt(2, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int todoId = rs.getInt(COL_TODO_ID);
                    Priority priorityEnum = parsePriority(rs.getString(COL_PRIORITY));

                    ToDo t = new ToDo();
                    t.setId(todoId);
                    t.setTitle(rs.getString(COL_TITLE));
                    t.setDescription(rs.getString(COL_DESCRIPTION));
                    t.setExpiryDate(rs.getTimestamp(COL_EXPIRY_DATE));
                    t.setUrlLink(rs.getString(COL_URL_LINK));
                    t.setImagePath(rs.getString(COL_IMAGE_PATH));
                    t.setBackgroundColor(rs.getString(COL_BACKGROUND_COLOR));
                    t.setCompleted(rs.getBoolean(COL_IS_COMPLETED));
                    t.setPositionOrder(rs.getInt(COL_POSITION_ORDER));
                    t.setPriority(priorityEnum);
                    t.setBoardId(rs.getInt(COL_BOARD_ID));
                    t.setOwnerUsername(rs.getString(COL_OWNER_USERNAME));
                    t.setRole(rs.getString(COL_ROLE));

                    t.setCollaborators(getCollaboratorsForTodo(todoId, conn));
                    todos.add(t);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore recupero todos da VIEW per titolo: " + boardTitle);
        }
        return todos;
    }

    /**
     * {@inheritDoc}
     *
     * @param todoId L'identificativo univoco del task.
     * @return Il percorso dell'immagine come stringa; {@code null} se assente o in caso di errore SQL.
     */
    @Override
    public String getImagePath(int todoId) {
        String sql = "SELECT image_path FROM todos WHERE todo_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, todoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString(COL_IMAGE_PATH);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore recupero image path per ToDo ID: " + todoId);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param todoId L'identificativo del task.
     * @param userId L'identificativo dell'utente da disassociare.
     * @return {@code true} se la rimozione ha successo;
     * {@code false} se l'associazione non viene trovata o in caso di errore SQL.
     */
    @Override
    public boolean leaveTodo(int todoId, int userId) {
        String sql = "DELETE FROM shared_todos WHERE todo_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, todoId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore uscita condivisione per task ID: " + todoId);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * Unisce i dati provenienti dalla tabella principale e dalle tabelle correlate
     * per restituire l'entità completa, includendo i collaboratori associati.
     *
     * @param todoId L'identificativo del task da cercare.
     * @return L'oggetto ToDo popolato; {@code null} se il record è inesistente o in caso di errore SQL.
     */
    @Override
    public ToDo getTodoById(int todoId) {
        String sql = "SELECT t.todo_id, t.title, t.description, t.expiry_date, t.priority, " +
                "t.is_completed, t.url_link, t.image_path, t.background_color, t.position_order, t.board_id, " +
                "u.username AS owner_username " +
                "FROM todos t " +
                "JOIN boards b ON t.board_id = b.board_id " +
                "JOIN users u ON b.user_id = u.user_id " +
                "WHERE t.todo_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, todoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ToDo todo = new ToDo();
                    todo.setId(rs.getInt(COL_TODO_ID));
                    todo.setTitle(rs.getString(COL_TITLE));
                    todo.setDescription(rs.getString(COL_DESCRIPTION));
                    todo.setExpiryDate(rs.getTimestamp(COL_EXPIRY_DATE));
                    todo.setUrlLink(rs.getString(COL_URL_LINK));
                    todo.setImagePath(rs.getString(COL_IMAGE_PATH));
                    todo.setBackgroundColor(rs.getString(COL_BACKGROUND_COLOR));
                    todo.setCompleted(rs.getBoolean(COL_IS_COMPLETED));
                    todo.setPositionOrder(rs.getInt(COL_POSITION_ORDER));
                    todo.setBoardId(rs.getInt(COL_BOARD_ID));
                    todo.setOwnerUsername(rs.getString(COL_OWNER_USERNAME));
                    todo.setPriority(parsePriority(rs.getString(COL_PRIORITY)));
                    todo.setCollaborators(getCollaborators(todoId));
                    return todo;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore recupero ToDo tramite ID: " + todoId);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * Distingue la logica di aggiornamento tra la tabella principale (per i proprietari)
     * e la tabella di legame (per i collaboratori) al fine di mantenere ordinamenti indipendenti.
     *
     * @param todoId      L'identificativo del task.
     * @param newPosition Il nuovo numero d'ordine stabilito nell'interfaccia.
     * @param role        Il ruolo dell'utente che esegue lo spostamento (OWNER o SHARED).
     * @param userId      L'ID dell'utente che sta effettuando lo spostamento.
     * @return {@code true} se l'aggiornamento ha effetto;
     * {@code false} se il record non viene trovato o in caso di errore SQL.
     */
    @Override
    public boolean updatePosition(int todoId, int newPosition, String role, int userId) {
        String sql;

        // 1. Determina la query corretta in base al ruolo
        if ("OWNER".equalsIgnoreCase(role)) {
            sql = "UPDATE todos SET position_order = ? WHERE todo_id = ?";
        } else {
            // Aggiorna la posizione personale nella tabella di legame
            sql = "UPDATE shared_todos SET position_order = ? WHERE todo_id = ? AND user_id = ?";
        }

        // 2. Apre la connessione e prepara lo statement con la stringa SQL corretta
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newPosition);
            pstmt.setInt(2, todoId);

            // Se è un collaboratore, imposta anche il terzo parametro per l'identità dell'utente
            if (!"OWNER".equalsIgnoreCase(role)) {
                pstmt.setInt(3, userId);
            }

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore spostamento posizione per ToDo ID: " + todoId);
            return false;
        }
    }

    // ===================================================================================
    //  METODI PRIVATI HELPER (Per ridurre la complessità)
    // ===================================================================================

    /**
     * Interpreta la stringa testuale della priorità recuperata dal database e la converte nel corrispondente tipo enumerato.
     *
     * @param priorityString La stringa estratta dalla colonna del database.
     * @return L'oggetto Priority corrispondente; restituisce {@code Priority.BASSA} come fallback di sicurezza se la stringa è nulla o non valida.
     */
    private Priority parsePriority(String priorityString) {
        if (priorityString == null) return Priority.BASSA;
        try {
            return Priority.valueOf(priorityString);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, e, () -> "Priorità non valida nel DB, fallback a BASSA: " + priorityString);
            return Priority.BASSA;
        }
    }

    /**
     * Recupera l'elenco dei collaboratori associati a uno specifico task riutilizzando una connessione SQL preesistente.
     * Questo approccio evita l'apertura multipla di connessioni all'interno di cicli o transazioni.
     *
     * @param todoId L'identificativo univoco del task interrogato.
     * @param conn La connessione al database attiva da utilizzare.
     * @return Una lista di stringhe contenente gli username dei collaboratori associati. Restituisce una lista vuota in caso di errore o assenza di record.
     */
    private List<String> getCollaboratorsForTodo(int todoId, Connection conn) {
        List<String> collaborators = new ArrayList<>();
        String sql = "SELECT u.username FROM shared_todos st JOIN users u ON st.user_id = u.user_id WHERE st.todo_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, todoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) collaborators.add(rs.getString(COL_USERNAME));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e, () -> "Errore caricamento collaboratori per task ID: " + todoId);
        }
        return collaborators;
    }

    /**
     * Inserisce un nuovo record fisico nella tabella principale dei task (todos) recuperando l'identificativo chiave autogenerato.
     * Metodo di supporto utilizzato internamente durante l'operazione transazionale di creazione.
     *
     * @param conn La connessione al database attiva che gestisce la transazione.
     * @param todo L'oggetto ToDo contenente i dati grezzi da persistere.
     * @return L'identificativo intero (ID) generato dal database per il nuovo record; restituisce {@code -1} se l'operazione fallisce o l'ID non è recuperabile.
     * @throws SQLException Se si verifica un errore durante la preparazione dello statement o l'esecuzione dell'inserimento fisico.
     */
    private int insertTodoRecord(Connection conn, ToDo todo) throws SQLException {
        String sqlInsert = "INSERT INTO todos (board_id, title, description, expiry_date, priority, is_completed, url_link, image_path, background_color, position_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            pstmtInsert.setInt(1, todo.getBoardId());
            pstmtInsert.setString(2, todo.getTitle());
            pstmtInsert.setString(3, todo.getDescription());
            pstmtInsert.setTimestamp(4, todo.getExpiryDate());
            pstmtInsert.setObject(5, todo.getPriority().name(), Types.OTHER);
            pstmtInsert.setBoolean(6, false);
            pstmtInsert.setString(7, todo.getUrlLink() != null ? todo.getUrlLink() : "");
            pstmtInsert.setString(8, todo.getImagePath() != null ? todo.getImagePath() : "");
            pstmtInsert.setString(9, todo.getBackgroundColor() != null ? todo.getBackgroundColor() : "#FFFFFF");
            pstmtInsert.setInt(10, todo.getPositionOrder());

            if (pstmtInsert.executeUpdate() > 0) {
                try (ResultSet rs = pstmtInsert.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Aggiorna fisicamente i campi informativi di un record ToDo già esistente nel database.
     * Metodo di supporto utilizzato internamente durante l'operazione transazionale di modifica.
     *
     * @param conn La connessione al database attiva che gestisce la transazione.
     * @param todo L'oggetto ToDo popolato con i dati aggiornati da sovrascrivere.
     * @throws SQLException Se si verifica un errore durante la preparazione dello statement o l'esecuzione dell'aggiornamento.
     */
    private void updateTodoRecord(Connection conn, ToDo todo) throws SQLException {
        String sqlUpdate = "UPDATE todos SET title=?, description=?, expiry_date=?, priority=?, url_link=?, " +
                "background_color=?, is_completed=?, image_path=? WHERE todo_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
            pstmt.setString(1, todo.getTitle());
            pstmt.setString(2, todo.getDescription());
            pstmt.setTimestamp(3, todo.getExpiryDate());
            pstmt.setObject(4, todo.getPriority().name(), Types.OTHER);
            pstmt.setString(5, todo.getUrlLink());
            pstmt.setString(6, todo.getBackgroundColor());
            pstmt.setBoolean(7, todo.isCompleted());
            pstmt.setString(8, todo.getImagePath());
            pstmt.setInt(9, todo.getId());
            pstmt.executeUpdate();
        }
    }

    /**
     * Associa fisicamente una lista di utenti come collaboratori di un task specifico, inserendo le relative chiavi nella tabella di legame.
     * Sfrutta l'elaborazione batch per ottimizzare le prestazioni in caso di assegnazioni multiple.
     *
     * @param conn          La connessione al database attiva che gestisce la transazione.
     * @param todoId        L'identificativo del task principale a cui collegare gli utenti.
     * @param collaborators La lista degli username testuali da risolvere in ID e associare.
     * @throws SQLException Se si verifica un errore durante l'esecuzione del batch di inserimento SQL.
     */
    private void assignCollaborators(Connection conn, int todoId, List<String> collaborators) throws SQLException {
        String sqlAssign = "INSERT INTO shared_todos (todo_id, user_id) SELECT ?, user_id FROM users WHERE username = ?";
        try (PreparedStatement pstmtAssign = conn.prepareStatement(sqlAssign)) {
            pstmtAssign.setInt(1, todoId);
            for (String user : collaborators) {
                pstmtAssign.setString(2, user);
                pstmtAssign.addBatch();
            }
            pstmtAssign.executeBatch();
        }
    }

    /**
     * Rimuove indiscriminatamente tutte le associazioni di condivisione di un task dalla tabella di legame.
     * Tipicamente impiegato per resettare le condivisioni prima di reinserire una lista aggiornata.
     *
     * @param conn   La connessione al database attiva che gestisce la transazione.
     * @param todoId L'identificativo del task di cui eliminare ogni traccia di collaborazione.
     * @throws SQLException Se si verifica un errore durante l'esecuzione della direttiva di eliminazione.
     */
    private void clearCollaborators(Connection conn, int todoId) throws SQLException {
        String sqlDeleteCollabs = "DELETE FROM shared_todos WHERE todo_id=?";
        try (PreparedStatement pstmtDel = conn.prepareStatement(sqlDeleteCollabs)) {
            pstmtDel.setInt(1, todoId);
            pstmtDel.executeUpdate();
        }
    }

    /**
     * Intercetta e gestisce l'annullamento di una transazione, sopprimendo loggando eventuali eccezioni secondarie
     * per evitare l'occultamento dell'errore originale (Exception Masking).
     *
     * @param conn La connessione al database su cui invocare il rollback di emergenza.
     */
    private void rollbackSilently(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Impossibile eseguire il rollback della transazione", ex);
            }
        }
    }

    /**
     * Ripristina la connessione al database al suo comportamento predefinito, riabilitando la conferma automatica delle istruzioni.
     * Da invocare rigorosamente all'interno dei blocchi finally al termine di operazioni transazionali.
     *
     * @param conn La connessione al database da riconfigurare.
     */
    private void restoreAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Impossibile ripristinare auto-commit", e);
            }
        }
    }
}