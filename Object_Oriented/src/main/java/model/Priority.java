package model;

import java.awt.Color;

/**
 * Definisce i livelli di priorità assegnabili a un task.
 * Ogni costante dell'enumerazione associa un'etichetta testuale descrittiva
 * e un oggetto {@link Color} specifico per standardizzare la resa cromatica
 * dei componenti nell'interfaccia grafica.
 */
public enum Priority {
    /**
     * Rappresenta la priorità massima, associata al colore rosso.
     */
    ALTA("Alta", new Color(231, 76, 60)),

    /**
     * Rappresenta la priorità intermedia, associata al colore giallo/arancio.
     */
    MEDIA("Media", new Color(241, 196, 15)),

    /**
     * Rappresenta la priorità minima, associata al colore verde.
     */
    BASSA("Bassa", new Color(39, 174, 96));

    private final String label;
    private final Color color;

    /**
     * Costruisce una costante di priorità con i relativi attributi visuali.
     * @param label Il nome leggibile della priorità da visualizzare a video.
     * @param color Il colore associato per la stilizzazione dei componenti UI.
     */
    Priority(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    /**
     * Restituisce l'etichetta testuale della priorità.
     *
     * @return La stringa contenente il nome della priorità.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Restituisce il colore associato alla priorità.
     *
     * @return L'oggetto {@link Color} per il rendering grafico.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Fornisce l'etichetta della priorità come rappresentazione testuale dell'oggetto.
     * Questo metodo è invocato automaticamente dai componenti Swing come le ComboBox.
     *
     * @return La label descrittiva della priorità.
     */
    @Override
    public String toString() {
        return label;
    }
}