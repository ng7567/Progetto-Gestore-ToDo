package model;

import java.util.Objects;

/**
 * Rappresenta l'entità bacheca (Board) all'interno del sistema.
 * Contiene i dati relativi al raggruppamento dei task, mantiene il collegamento
 * con l'utente creatore e fornisce i metodi necessari per leggere o modificare
 * le sue informazioni principali.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class Board {

    /** Il codice numerico univoco generato dal database per identificare la bacheca. */
    private int id;

    /** Il nome assegnato alla bacheca. */
    private String title;

    /** Il testo aggiuntivo che descrive lo scopo o il contenuto della bacheca. */
    private String description;

    /** Il codice numerico dell'utente che ha creato la bacheca. */
    private final int userId;

    /**
     * Crea una nuova bacheca pronta per essere salvata nel database.
     * L'ID non viene richiesto perché sarà assegnato automaticamente dal sistema
     * al momento del salvataggio.
     *
     * @param title       Il nome della bacheca.
     * @param description Le informazioni aggiuntive sul contenuto.
     * @param userId      Il codice dell'utente creatore.
     */
    public Board(String title, String description, int userId) {
        this.title = title;
        this.description = description;
        this.userId = userId;
    }

    /**
     * Crea un oggetto Board completo usando i dati letti dal database.
     *
     * @param id          Il codice numerico univoco della bacheca.
     * @param title       Il nome della bacheca.
     * @param description Le informazioni aggiuntive sul contenuto.
     * @param userId      Il codice dell'utente creatore.
     */
    public Board(int id, String title, String description, int userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.userId = userId;
    }

    /**
     * Legge il codice identificativo della bacheca.
     *
     * @return L'ID numerico associato al record.
     */
    public int getId() {
        return id;
    }

    /**
     * Modifica il codice identificativo della bacheca.
     *
     * @param id Il nuovo ID numerico da assegnare.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Legge il nome della bacheca.
     *
     * @return La stringa che contiene il titolo.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sostituisce il nome della bacheca con uno nuovo.
     *
     * @param title Il nuovo nome da assegnare.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Legge il codice dell'utente che ha creato la bacheca.
     *
     * @return L'ID numerico dell'utente creatore.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Legge le informazioni aggiuntive della bacheca.
     *
     * @return La stringa con la descrizione, o {@code null} se non è stata inserita.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sostituisce le informazioni aggiuntive della bacheca con un nuovo testo.
     *
     * @param description Il nuovo testo da assegnare.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Restituisce una rappresentazione testuale dell'oggetto.
     * Mostra direttamente il nome della bacheca, facilitando la sua visualizzazione
     * automatica all'interno di liste o menu a tendina nell'interfaccia grafica.
     *
     * @return Il nome della bacheca.
     */
    @Override
    public String toString() {
        return title;
    }

    /**
     * Confronta la bacheca con un altro oggetto per capire se sono uguali.
     * Due bacheche sono considerate identiche se hanno lo stesso ID numerico.
     *
     * @param o L'oggetto da confrontare.
     * @return {@code true} se gli oggetti sono uguali, {@code false} altrimenti.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return id == board.id;
    }

    /**
     * Genera un codice numerico (hash) per la bacheca.
     * Il calcolo si basa unicamente sul suo ID univoco per garantire coerenza
     * con il metodo {@link #equals(Object)}.
     *
     * @return Il valore hash calcolato.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}