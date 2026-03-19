package model;

import java.awt.Color;

/**
 * Definisce i livelli di priorità che possono essere assegnati a un task.
 * Ogni elemento dell'enumerazione contiene un'etichetta di testo e un oggetto
 * {@link Color} per mantenere coerenti i colori nell'interfaccia utente (UI).
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public enum Priority {

    /** Rappresenta la priorità più alta, associata al colore rosso. */
    ALTA("Alta", new Color(231, 76, 60)),

    /** Rappresenta la priorità intermedia, associata al colore giallo. */
    MEDIA("Media", new Color(241, 196, 15)),

    /** Rappresenta la priorità più bassa, associata al colore verde. */
    BASSA("Bassa", new Color(39, 174, 96));

    /** Il nome della priorità mostrato all'utente. */
    private final String label;

    /** Il colore associato alla priorità per il rendering grafico. */
    private final Color color;

    /**
     * Crea un elemento della priorità con il suo nome e colore.
     * @param label Il nome della priorità da mostrare nell'interfaccia.
     * @param color Il colore usato per disegnare i componenti UI legati a questa priorità.
     */
    Priority(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    /**
     * Restituisce il nome della priorità.
     *
     * @return La stringa che contiene il nome.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Restituisce il colore associato alla priorità.
     *
     * @return L'oggetto {@link Color} usato per il rendering.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Restituisce il nome della priorità come testo.
     * Viene usato in automatico dai componenti Swing, come le JComboBox,
     * per mostrare il nome corretto nell'interfaccia.
     *
     * @return Il nome descrittivo della priorità.
     */
    @Override
    public String toString() {
        return label;
    }
}