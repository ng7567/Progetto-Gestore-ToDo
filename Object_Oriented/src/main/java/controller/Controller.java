package controller;

import dao.*;
import dao.impl.BoardDAOImpl;
import dao.impl.ToDoDAOImpl;
import dao.impl.UserDAOImpl;
import dto.TodoCreationDTO;
import model.*;
import exception.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestisce la logica di business dell'applicazione e media la comunicazione
 * tra l'interfaccia grafica e il livello di accesso ai dati.
 * Mantiene lo stato della sessione utente corrente e solleva eccezioni di dominio
 * in caso di violazione delle regole di business.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class Controller {

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    private final UserDAO userDAO;
    private final BoardDAO boardDAO;
    private final ToDoDAO toDoDAO;

    private User currentUser = null;

    /**
     * Inizializza il controller e istanzia i DAO necessari per interagire con il database.
     *
     * @see dao.UserDAO
     * @see dao.BoardDAO
     * @see dao.ToDoDAO
     */
    public Controller() {
        userDAO = new UserDAOImpl();
        boardDAO = new BoardDAOImpl();
        toDoDAO = new ToDoDAOImpl();
    }

    // --- METODI USER ---

    /**
     * Tenta l'autenticazione di un utente nel sistema verificando le credenziali.
     * Se fallisce lancia un'eccezione di credenziali invalide o SQL.
     *
     * @param username Il nome utente fornito durante il tentativo di accesso.
     * @param password La password fornita (in chiaro) durante il tentativo di accesso.
     * @throws InvalidCredentialsException Se l'utente non esiste o la password è errata.
     * @throws SQLException Se si verifica un errore critico di connessione al database.
     */
    public void login(String username, String password) throws InvalidCredentialsException, SQLException {
        if (!userDAO.userExists(username)) {
            throw new InvalidCredentialsException("Username non trovato.");
        }

        User user = userDAO.authenticate(username, password);

        if (user != null) {
            this.currentUser = user;
        } else {
            throw new InvalidCredentialsException("Password errata");
        }
    }

    /**
     * Registra un nuovo utente nel sistema.
     * Verifica preventivamente l'esistenza dello username per gestire correttamente
     * l'eccezione di duplicazione prima di procedere con la persistenza.
     *
     * @param username Il nome desiderato per il nuovo utente.
     * @param password La password in chiaro.
     * @throws UserAlreadyExistsException Se lo username è già registrato nel database.
     * @throws SQLException               Se si verifica un errore durante l'esecuzione della query.
     */
    public void register(String username, String password) throws UserAlreadyExistsException, SQLException {
        // Controllo preventivo: se l'utente esiste già, lanciamo subito l'eccezione specifica.
        if (userDAO.userExists(username)) {
            throw new UserAlreadyExistsException(username);
        }

        // Se non esiste, procediamo alla creazione.
        User newUser = new User(username, password);
        boolean success = userDAO.registerUser(newUser);

        if (!success) {
            // Questo caso ora gestirebbe solo errori di inserimento generici non causati da duplicati.
            throw new SQLException("Inserimento fallito per motivi ignoti.");
        }
    }

    /**
     * Effettua il logout dell'utente corrente, terminando la sessione locale
     * e invalidando le informazioni utente in memoria.
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Restituisce l'oggetto User associato all'utente attualmente loggato nel sistema.
     *
     * @return L'oggetto {@link User} corrente, oppure {@code null} se nessun utente è autenticato.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Cerca utenti nel database corrispondenti alla query specificata,
     * escludendo l'utente attualmente loggato dai risultati restituiti.
     *
     * @param query La stringa di ricerca parziale per filtrare il nome utente.
     * @return Una lista di nomi utente che corrispondono ai criteri di ricerca,
     * oppure una lista vuota se la query è nulla o vuota.
     */
    public List<String> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String currentUsername = "";
        if (currentUser != null) {
            currentUsername = currentUser.getUsername();
        }
        return userDAO.searchUsers(query, currentUsername);
    }

    // --- METODI BOARD ---

    /**
     * Crea una nuova bacheca nel database e la associa all'utente attualmente loggato.
     *
     * @param title Il titolo da assegnare alla nuova bacheca.
     * @param description La descrizione della bacheca.
     * @return {@code true} se la creazione ha avuto successo, {@code false} se manca l'utente loggato.
     * @throws DuplicateBoardException Se l'utente possiede già una bacheca con lo stesso nome.
     */
    public boolean addBoard(String title, String description) throws DuplicateBoardException {
        if (currentUser == null) {
            LOGGER.warning("Tentativo di creare una bacheca senza utente loggato.");
            return false;
        }

        String cleanTitle = title.trim();

        boolean nameExists = getUserBoards().stream()
                .anyMatch(b -> b.getTitle().equalsIgnoreCase(cleanTitle));

        if (nameExists) {
            throw new DuplicateBoardException(cleanTitle);
        }

        Board newBoard = new Board(cleanTitle, description, currentUser.getId());
        return boardDAO.createBoard(newBoard);
    }

    /**
     * Recupera la lista di tutte le bacheche appartenenti all'utente corrente.
     *
     * @return Una lista di oggetti {@link Board} associati all'utente,
     * oppure una lista vuota se nessun utente è loggato.
     */
    public List<Board> getUserBoards() {
        if (currentUser == null) return new ArrayList<>();
        return boardDAO.getBoardsByUser(currentUser.getId());
    }

    /**
     * Rinomina e cambia la descrizione di una bacheca esistente nel database.
     *
     * @param boardId L'identificativo univoco della bacheca da rinominare.
     * @param newTitle Il nuovo titolo da assegnare alla bacheca.
     * @param newDescription La nuova descrizione della bacheca.
     * @return {@code true} se l'aggiornamento ha avuto successo, {@code false} se il nuovo titolo è nullo o vuoto.
     * @throws DuplicateBoardException Se il nuovo titolo esiste già per un'altra bacheca dell'utente.
     * @throws SQLException Se si verifica un errore tecnico di connessione o query.
     */
    public boolean updateBoardDetails(int boardId, String newTitle, String newDescription) throws DuplicateBoardException, SQLException{
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return false;
        }

        String cleanTitle = newTitle.trim();

        // Evita duplicati (escludendo se stessa dal controllo)
        boolean nameExists = getUserBoards().stream()
                .anyMatch(b -> b.getId() != boardId && b.getTitle().equalsIgnoreCase(cleanTitle));

        if (nameExists) {
            throw new DuplicateBoardException(cleanTitle);
        }

        return boardDAO.updateBoard(boardId, cleanTitle, newDescription);
    }

    /**
     * Verifica se una bacheca &egrave; bloccata per la rinomina (ad esempio se contiene task condivisi).
     *
     * @param boardTitle Il titolo della bacheca da verificare.
     * @param userId L'ID dell'utente che richiede la verifica.
     * @return {@code true} se la bacheca &egrave; bloccata e non modificabile, {@code false} se il titolo della bacheca è modificabile.
     */
    public boolean isBoardLocked(String boardTitle, int userId) {
        return boardDAO.isBoardLocked(boardTitle, userId);
    }

    /**
     * Elimina definitivamente una bacheca e tutti i task (ToDo) in essa contenuti dal database.
     * Rimuove anche dal disco le eventuali immagini fisiche associate ai task eliminati.
     * Operazione irreversibile.
     *
     * @param board L'oggetto bacheca da eliminare.
     * @return {@code true} se l'eliminazione ha avuto successo, {@code false} se l'eliminazione non &egrave; andata a buon fine
     */
    public boolean deleteBoard(Board board) {
        List<ToDo> todosInBoard = getTodos(board.getTitle());

        boolean success = boardDAO.deleteBoard(board.getId());

        if (success && todosInBoard != null) {
            for (ToDo todo : todosInBoard) {
                String imagePath = todo.getImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    boolean fileDeleted = util.FileUtils.deleteFile(imagePath);
                    if (!fileDeleted) {
                        LOGGER.log(Level.WARNING, "Impossibile eliminare il file immagine fisico: {0}", imagePath);
                    }
                }
            }
        }

        return success;
    }

    // --- METODI TODO ---

    /**
     * Recupera la lista dei task (ToDo) associati a una specifica bacheca visibile all'utente.
     *
     * @param boardTitle Il titolo della bacheca.
     * @return Una lista di oggetti {@link ToDo}, oppure una lista vuota se nessun utente è loggato.
     */
    public List<ToDo> getTodos(String boardTitle) {
        if (currentUser == null) return new ArrayList<>();
        return toDoDAO.getTodosByBoardTitle(boardTitle, currentUser.getId());
    }

    /**
     * Calcola e restituisce la prossima posizione disponibile per inserire un nuovo task in coda.
     *
     * @param boardId L'identificativo univoco della bacheca.
     * @return Un intero progressivo rappresentante la nuova posizione disponibile.
     */
    public int getNextTodoPosition(int boardId) {
        return toDoDAO.getNextPosition(boardId);
    }

    /**
     * Crea un nuovo task (ToDo) nel database a partire da un oggetto di trasferimento dati.
     *
     * @param todoDetails L'oggetto {@link TodoCreationDTO} contenente tutti i dettagli del nuovo task.
     * @return {@code true} se la creazione ha avuto successo, {@code false} se la creazione non &egrave; andata a buon fine
     * @throws SelfSharingException Se l'utente tenta erroneamente di aggiungere se stesso alla lista dei collaboratori.
     */
    public boolean addTodo(TodoCreationDTO todoDetails) throws SelfSharingException {
        if (currentUser != null && todoDetails.collaborators() != null &&
                todoDetails.collaborators().contains(currentUser.getUsername())) {
            throw new SelfSharingException();
        }

        ToDo newTodo = new ToDo(
                todoDetails.boardId(),
                todoDetails.title(),
                todoDetails.description(),
                todoDetails.expiryDate(),
                todoDetails.priority()
        );

        newTodo.setUrlLink(todoDetails.link());
        newTodo.setImagePath(todoDetails.imagePath());
        newTodo.setBackgroundColorObj(todoDetails.backgroundColor());
        newTodo.setCollaborators(todoDetails.collaborators());
        newTodo.setPositionOrder(todoDetails.positionOrder());

        return toDoDAO.createToDo(newTodo);
    }

    /**
     * Aggiorna lo stato di completamento di un task specifico.
     *
     * @param todoId    L'identificativo univoco del task.
     * @param completed {@code true} per marcare come completato, {@code false} per rimetterlo da completare.
     * @return {@code true} se l'aggiornamento dello stato ha avuto successo nel database, {@code false} se l'aggiornamento dello stato non &egrave; andato a buon fine.
     */
    public boolean setTodoComplete(int todoId, boolean completed) {
        return toDoDAO.toggleCompletion(todoId, completed);
    }

    /**
     * Aggiorna tutti i campi di un ToDo esistente nel database sovrascrivendoli.
     *
     * @param todo L'oggetto {@link ToDo} contenente i dati aggiornati provenienti dalla UI.
     * @return {@code true} se l'aggiornamento ha avuto successo, {@code false} se l'aggiornamento del task non ha avuto successo.
     * @throws SelfSharingException Se si tenta di aggiungere il proprietario ai collaboratori.
     * @throws SQLException Se si verifica un errore durante l'aggiornamento (es. violazione check constraint date).
     */
    public boolean updateTodo(ToDo todo) throws SelfSharingException, SQLException {
        if (todo.getOwnerUsername() != null && todo.getCollaborators() != null &&
                todo.getCollaborators().contains(todo.getOwnerUsername())) {
            throw new SelfSharingException();
        }
        return toDoDAO.updateToDo(todo);
    }

    /**
     * Gestisce la rimozione di un task dall'interfaccia dell'utente.
     * Il comportamento varia in base al ruolo dell'utente:
     * <ul>
     *     <li><b>Proprietario:</b> il task e la relativa immagine fisica vengono eliminati dal database per tutti gli utenti.</li>
     *     <li><b>Collaboratore:</b> rimuove esclusivamente l'associazione dell'utente al task (abbandono della condivisione).</li>
     * </ul>
     *
     * @param todoId L'identificativo univoco del task da rimuovere o abbandonare.
     * @return {@code true} se l'operazione (eliminazione o abbandono) ha avuto successo, {@code false} se il task non esiste o c'è stato un problema nell'eliminazione.
     */
    public boolean deleteTodo(int todoId) {
        ToDo todo = toDoDAO.getTodoById(todoId);
        if (todo == null) return false;

        String currentUsername = getCurrentUser().getUsername();

        // Se l'utente è il creatore originario, cancella tutto
        if (currentUsername.equals(todo.getOwnerUsername())) {
            String imagePath = toDoDAO.getImagePath(todoId);
            if (imagePath != null && !imagePath.isEmpty()) {
                boolean fileDeleted = util.FileUtils.deleteFile(imagePath);
                if (!fileDeleted) {
                    LOGGER.log(Level.WARNING, "Impossibile eliminare il file immagine fisico: {0}", imagePath);
                }
            }
            return toDoDAO.deleteToDo(todoId);
        } else {
            // Se è un collaboratore, abbandona semplicemente il task
            int myUserId = getCurrentUser().getId();
            return toDoDAO.leaveTodo(todoId, myUserId);
        }
    }

    /**
     * Sposta logicamente un task da una bacheca all'altra modificandone l'ID di riferimento.
     *
     * @param todoId        L'identificativo del task da spostare.
     * @param targetBoardId L'identificativo della bacheca di destinazione.
     * @param currentUserId L'identificativo dell'utente loggato al momento.
     * @return {@code true} se lo spostamento ha avuto successo, {@code false} se lo spostamento non &egrave; andato a buon fine.
     */
    public boolean moveTodo(int todoId, int targetBoardId, int currentUserId) {
        return toDoDAO.updateBoardId(todoId, targetBoardId, currentUserId);
    }

    /**
     * Aggiorna la posizione (ordine di visualizzazione) di un task all'interno della bacheca.
     * Utilizzato dopo un'operazione di Drag &amp; Drop.
     *
     * @param todoId L'identificativo univoco del task.
     * @param newPosition Il nuovo valore numerico intero della posizione.
     * @param role        Il ruolo dell'utente (OWNER o SHARED).
     * @return {@code true} se l'aggiornamento ha avuto successo nel database, {@code false} se l'aggiornamento non &egrave; andato a buon fine.
     */
    public boolean updateTodoPosition(int todoId, int newPosition, String role) {
        if (currentUser == null) return false;
        return toDoDAO.updatePosition(todoId, newPosition, role, currentUser.getId());
    }
}