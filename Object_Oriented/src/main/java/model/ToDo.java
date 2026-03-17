package model;

import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rappresenta un singolo Task (attività) all'interno di una bacheca.
 * Contiene tutte le informazioni relative al contenuto, alla scadenza,
 * alla priorità e alla gestione dei collaboratori.
 */
public class ToDo {

    private static final String DEFAULT_BG_COLOR = "#FFFFFF";

    private int id;
    private String title;
    private String description;
    private Timestamp expiryDate;
    private String urlLink;
    private String imagePath;
    private String backgroundColor;
    private boolean isCompleted;
    private int positionOrder;
    private Priority priority;
    private int boardId;
    private String ownerUsername;
    private List<String> collaborators = new ArrayList<>();
    private String role;

    /**
     * Costruisce un'istanza vuota di default.
     * Metodo utilizzato per l'inizializzazione progressiva tramite i setter.
     */
    public ToDo() {}

    /**
     * Costruisce un task con i parametri base necessari alla creazione.
     * Inizializza lo stato come non completato e imposta lo sfondo bianco di default.
     *
     * @param boardId     L'identificativo della bacheca di appartenenza.
     * @param title       Il titolo testuale del task.
     * @param description La descrizione dettagliata dell'attività.
     * @param expiryDate  La data e l'ora di scadenza prevista.
     * @param priority    Il livello di priorità assegnato.
     */
    public ToDo(int boardId, String title, String description, Timestamp expiryDate, Priority priority) {
        this.boardId = boardId;
        this.title = title;
        this.description = Objects.toString(description, "");
        this.priority = priority;
        this.expiryDate = expiryDate;
        this.backgroundColor = DEFAULT_BG_COLOR;
        this.isCompleted = false;
        this.positionOrder = 0;
    }

    /**
     * Imposta il colore di sfondo partendo da un oggetto {@link Color}.
     * Esegue la conversione automatica del colore nel formato esadecimale (es. #FFFFFF).
     *
     * @param color L'oggetto colore selezionato dall'interfaccia grafica.
     */
    public void setBackgroundColorObj(Color color) {
        if (color == null) {
            this.backgroundColor = DEFAULT_BG_COLOR;
        } else {
            this.backgroundColor = String.format("#%02x%02x%02x",
                    color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    // --- GETTERS & SETTERS ---

    /** @return L'username del proprietario del task. */
    public String getOwnerUsername() { return ownerUsername; }
    /** @param ownerUsername L'username da impostare come proprietario. */
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    /** @return La lista degli username dei collaboratori. */
    public List<String> getCollaborators() { return collaborators; }
    /** @param collaborators La lista di collaboratori da associare. */
    public void setCollaborators(List<String> collaborators) { this.collaborators = collaborators; }

    /** @return L'ID univoco del task. */
    public int getId() { return id; }
    /** @param id L'identificativo univoco da assegnare. */
    public void setId(int id) { this.id = id; }

    /** @return Il titolo del task. */
    public String getTitle() { return title; }
    /** @param title Il titolo testuale da impostare. */
    public void setTitle(String title) { this.title = title; }

    /** @return La descrizione del task. */
    public String getDescription() { return description; }
    /** @param description La descrizione testuale da impostare. */
    public void setDescription(String description) { this.description = description; }

    /** @return Il timestamp di scadenza. */
    public Timestamp getExpiryDate() { return expiryDate; }
    /** @param expiryDate La data di scadenza da impostare. */
    public void setExpiryDate(Timestamp expiryDate) { this.expiryDate = expiryDate; }

    /** @return Il link URL associato al task. */
    public String getUrlLink() { return urlLink; }
    /** @param urlLink L'indirizzo web da collegare. */
    public void setUrlLink(String urlLink) { this.urlLink = urlLink; }

    /** @return Il percorso dell'immagine allegata. */
    public String getImagePath() { return imagePath; }
    /** @param imagePath Il percorso del file immagine sul disco. */
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    /** @return Il codice esadecimale del colore di sfondo. */
    public String getBackgroundColor() { return backgroundColor; }
    /** @param backgroundColor Il codice colore esadecimale da impostare. */
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    /** @return {@code true} se il task è completato. */
    public boolean isCompleted() { return isCompleted; }
    /** @param completed Lo stato di completamento da impostare. */
    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    /** @return La posizione numerica del task nella bacheca. */
    public int getPositionOrder() { return positionOrder; }
    /** @param positionOrder L'indice di ordinamento da impostare. */
    public void setPositionOrder(int positionOrder) { this.positionOrder = positionOrder; }

    /** @return L'ID della bacheca contenitrice. */
    public int getBoardId() { return boardId; }
    /** @param boardId L'ID della bacheca a cui assegnare il task. */
    public void setBoardId(int boardId) { this.boardId = boardId; }

    /** @return Il livello di priorità del task. */
    public Priority getPriority() { return priority; }
    /** @param priority La priorità da assegnare. */
    public void setPriority(Priority priority) { this.priority = priority; }

    /** @return Il ruolo dell'utente corrente rispetto al task (OWNER/SHARED). */
    public String getRole() { return role; }
    /** @param role Il ruolo da assegnare per la gestione dei permessi UI. */
    public void setRole(String role) { this.role = role; }

    /**
     * Restituisce il titolo del task come rappresentazione testuale.
     * @return Il titolo dell'attività.
     */
    @Override
    public String toString() {
        return title;
    }
}