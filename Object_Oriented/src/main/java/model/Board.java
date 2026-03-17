package model;

import java.util.Objects;

/**
 * Rappresenta l'entità bacheca (Board) all'interno del sistema.
 * Gestisce i dati relativi al contenitore dei task, mantenendo il riferimento
 * univoco all'utente proprietario e fornendo i metodi per la manipolazione
 * degli attributi testuali.
 */
public class Board {
    private int id;
    private String title;
    private String description;
    private final int userId;

    /**
     * Costruisce una nuova bacheca destinata all'inserimento nel database.
     * L'identificativo viene omesso poiché la sua generazione è delegata
     * al sistema di persistenza (chiave seriale).
     *
     * @param title       Il titolo testuale della bacheca.
     * @param description La descrizione estesa del contenuto.
     * @param userId      L'identificativo univoco dell'utente proprietario.
     */
    public Board(String title, String description, int userId) {
        this.title = title;
        this.description = description;
        this.userId = userId;
    }

    /**
     * Costruisce un oggetto Board completo includendo i dati recuperati dal database.
     *
     * @param id          L'identificativo univoco della bacheca.
     * @param title       Il titolo testuale della bacheca.
     * @param description La descrizione estesa del contenuto.
     * @param userId      L'identificativo univoco dell'utente proprietario.
     */
    public Board(int id, String title, String description, int userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.userId = userId;
    }

    /**
     * Restituisce l'identificativo univoco della bacheca.
     *
     * @return L'ID numerico della bacheca.
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'identificativo univoco della bacheca.
     *
     * @param id L'ID numerico da assegnare al record.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce il titolo della bacheca.
     *
     * @return La stringa contenente il titolo.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Aggiorna il titolo della bacheca.
     *
     * @param title Il nuovo titolo testuale da assegnare.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Restituisce l'identificativo dell'utente proprietario della bacheca.
     *
     * @return L'ID dell'utente a cui è associata la bacheca.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Restituisce la descrizione estesa della bacheca.
     *
     * @return La stringa contenente la descrizione, o {@code null} se non presente.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Aggiorna la descrizione della bacheca.
     *
     * @param description La nuova descrizione testuale da assegnare.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Fornisce una rappresentazione testuale dell'oggetto.
     * Restituisce il titolo per facilitare il rendering automatico nei componenti
     * grafici di tipo lista o menu a tendina.
     *
     * @return Il titolo della bacheca.
     */
    @Override
    public String toString() {
        return title;
    }

    /**
     * Confronta la bacheca corrente con un altro oggetto per verificarne l'uguaglianza.
     * La verifica si basa esclusivamente sulla corrispondenza dell'ID univoco.
     *
     * @param o L'oggetto da confrontare con l'istanza corrente.
     * @return {@code true} se gli ID coincidono, {@code false} altrimenti.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return id == board.id;
    }

    /**
     * Calcola il codice hash univoco per l'istanza della bacheca.
     * Il valore è generato a partire dall'identificativo numerico.
     *
     * @return Il valore intero rappresentante l'hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}