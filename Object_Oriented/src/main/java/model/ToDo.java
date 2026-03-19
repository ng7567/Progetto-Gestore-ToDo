package model;

import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rappresenta un singolo ToDo (o Task) all'interno di una bacheca.
 * Contiene tutte le informazioni relative al contenuto, alla scadenza,
 * alla priorità e agli utenti che ci collaborano.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class ToDo {

    /** Colore di sfondo predefinito in formato esadecimale (bianco). */
    private static final String DEFAULT_BG_COLOR = "#FFFFFF";

    /** ID numerico univoco del task nel database. */
    private int id;

    /** Titolo o nome breve del task. */
    private String title;

    /** Testo lungo che descrive i dettagli del task. */
    private String description;

    /** Data e ora in cui il task scade. */
    private Timestamp expiryDate;

    /** Indirizzo web (URL) salvato come link allegato al task. */
    private String urlLink;

    /** Percorso nel file system dell'immagine allegata al task. */
    private String imagePath;

    /** Colore di sfondo del task salvato come testo esadecimale (es. "#FF0000"). */
    private String backgroundColor;

    /** Indica se il task è stato completato (spuntato). */
    private boolean isCompleted;

    /** Numero che indica la posizione del task per l'ordinamento nella bacheca. */
    private int positionOrder;

    /** Livello di importanza del task. */
    private Priority priority;

    /** ID della bacheca a cui appartiene questo task. */
    private int boardId;

    /** Username dell'utente che ha creato il task. */
    private String ownerUsername;

    /** Lista degli username degli utenti invitati a collaborare. */
    private List<String> collaborators = new ArrayList<>();

    /** Indica il ruolo dell'utente che sta guardando il task per la gestione dei permessi UI (es. "OWNER" o "SHARED"). */
    private String role;

    /**
     * Crea un task vuoto.
     * Viene usato principalmente per creare un oggetto base da riempire
     * in seguito usando i metodi setter.
     */
    public ToDo() {}

    /**
     * Crea un task con le informazioni base necessarie.
     * Imposta in automatico lo stato a "non completato" e lo sfondo a bianco.
     *
     * @param boardId     L'ID della bacheca in cui inserire il task.
     * @param title       Il titolo del task.
     * @param description Il testo con i dettagli dell'attività.
     * @param expiryDate  La data e l'ora di scadenza.
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
     * Imposta il colore di sfondo partendo da un oggetto {@link Color} di Java.
     * Converte in automatico il colore nel formato di testo esadecimale (es. "#FFFFFF").
     *
     * @param color Il colore scelto nell'interfaccia UI (se nullo, usa il bianco di default).
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

    /**
     * Restituisce l'username del creatore del task.
     *
     * @return L'username del proprietario.
     */
    public String getOwnerUsername() { return ownerUsername; }

    /**
     * Imposta l'username del creatore del task.
     *
     * @param ownerUsername L'username da salvare come proprietario.
     */
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    /**
     * Restituisce la lista di chi collabora al task.
     *
     * @return La lista degli username dei collaboratori.
     */
    public List<String> getCollaborators() { return collaborators; }

    /**
     * Imposta la lista dei collaboratori.
     *
     * @param collaborators La lista di username da associare al task.
     */
    public void setCollaborators(List<String> collaborators) { this.collaborators = collaborators; }

    /**
     * Restituisce l'ID del task.
     *
     * @return L'ID numerico univoco nel database.
     */
    public int getId() { return id; }

    /**
     * Imposta l'ID del task.
     *
     * @param id L'ID numerico da assegnare.
     */
    public void setId(int id) { this.id = id; }

    /**
     * Restituisce il titolo del task.
     *
     * @return Il titolo in formato testo.
     */
    public String getTitle() { return title; }

    /**
     * Imposta il titolo del task.
     *
     * @param title Il testo da usare come titolo.
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Restituisce la descrizione dettagliata del task.
     *
     * @return Il testo della descrizione.
     */
    public String getDescription() { return description; }

    /**
     * Imposta la descrizione del task.
     *
     * @param description Il testo con i dettagli da salvare.
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Restituisce la scadenza del task.
     *
     * @return L'oggetto {@link Timestamp} con data e ora.
     */
    public Timestamp getExpiryDate() { return expiryDate; }

    /**
     * Imposta la scadenza del task.
     *
     * @param expiryDate La data e l'ora da salvare.
     */
    public void setExpiryDate(Timestamp expiryDate) { this.expiryDate = expiryDate; }

    /**
     * Restituisce il link web allegato.
     *
     * @return L'indirizzo URL salvato.
     */
    public String getUrlLink() { return urlLink; }

    /**
     * Imposta un link web da allegare al task.
     *
     * @param urlLink L'indirizzo URL da salvare.
     */
    public void setUrlLink(String urlLink) { this.urlLink = urlLink; }

    /**
     * Restituisce il percorso dell'immagine allegata.
     *
     * @return Il percorso del file immagine.
     */
    public String getImagePath() { return imagePath; }

    /**
     * Imposta il percorso di un'immagine da allegare.
     *
     * @param imagePath Il percorso del file sul computer.
     */
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    /**
     * Restituisce il colore di sfondo del task.
     *
     * @return Il codice esadecimale del colore (es. "#FFFFFF").
     */
    public String getBackgroundColor() { return backgroundColor; }

    /**
     * Imposta il colore di sfondo usando un testo esadecimale.
     *
     * @param backgroundColor Il codice colore (es. "#000000") da salvare.
     */
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    /**
     * Controlla se il task è completato.
     *
     * @return {@code true} se è completato, {@code false} se è ancora da fare.
     */
    public boolean isCompleted() { return isCompleted; }

    /**
     * Cambia lo stato di completamento del task.
     *
     * @param completed {@code true} per segnarlo come finito, {@code false} per riaprirlo.
     */
    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    /**
     * Restituisce la posizione del task per l'ordinamento a video.
     *
     * @return Il numero che indica la posizione nella lista.
     */
    public int getPositionOrder() { return positionOrder; }

    /**
     * Imposta la posizione del task per l'ordinamento.
     *
     * @param positionOrder Il numero della posizione da assegnare.
     */
    public void setPositionOrder(int positionOrder) { this.positionOrder = positionOrder; }

    /**
     * Restituisce l'ID della bacheca che contiene il task.
     *
     * @return L'ID numerico della bacheca.
     */
    public int getBoardId() { return boardId; }

    /**
     * Imposta la bacheca a cui assegnare il task.
     *
     * @param boardId L'ID numerico della bacheca contenitore.
     */
    public void setBoardId(int boardId) { this.boardId = boardId; }

    /**
     * Restituisce il livello di importanza del task.
     *
     * @return L'oggetto {@link Priority} assegnato.
     */
    public Priority getPriority() { return priority; }

    /**
     * Imposta il livello di importanza del task.
     *
     * @param priority Il livello (ALTA, MEDIA, BASSA) da assegnare.
     */
    public void setPriority(Priority priority) { this.priority = priority; }

    /**
     * Restituisce il ruolo dell'utente che sta visualizzando il task.
     *
     * @return Il testo indicante il ruolo (es. "OWNER" o "SHARED").
     */
    public String getRole() { return role; }

    /**
     * Imposta il ruolo dell'utente per definire cosa può fare nell'interfaccia UI.
     *
     * @param role Il ruolo da assegnare (es. "OWNER").
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Restituisce il titolo del task quando l'oggetto viene convertito in testo.
     * Utile per la stampa nei log o in componenti base di Swing.
     *
     * @return Il titolo del task.
     */
    @Override
    public String toString() {
        return title;
    }
}