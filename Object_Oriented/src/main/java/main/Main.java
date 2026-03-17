package main;

import controller.Controller;
import gui.frames.LogInFrame;
import util.DBConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Punto di ingresso principale dell'applicazione ToDo.
 * Si occupa di configurare le proprietà di sistema per il rendering grafico,
 * verificare la disponibilità del database e avviare l'interfaccia utente.
 */
public class Main {

    /**
     * Metodo di avvio dell'applicazione.
     *
     * @param args Argomenti passati da riga di comando (attualmente non utilizzati).
     */
    public static void main(String[] args) {
        // Configurazione Pipeline Grafica
        // Disabilita l'accelerazione Direct3D per prevenire glitch su alcune configurazioni Windows.
        System.setProperty("sun.java2d.d3d", "false");

        // Disabilita il caricamento delle librerie native di FlatLaf.
        System.setProperty("flatlaf.useNativeLibrary", "false");

        // Avvio dell'interfaccia grafica
        // Esegue la creazione della GUI nell'Event Dispatch Thread (EDT) per thread-safety.
        SwingUtilities.invokeLater(() -> {

            // Verifica Preliminare Connessione Database
            try (Connection testConn = DBConnection.getInstance().getConnection()) {
                if (testConn == null) {
                    throw new SQLException("L'istanza di connessione al database è nulla.");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Errore critico: Impossibile connettersi al Database.\n" +
                                "Dettaglio: " + e.getMessage() + "\n\n" +
                                "Assicurati che PostgreSQL sia avviato e i parametri in DBConnection siano corretti.",
                        "Errore di Connessione",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Termina l'applicazione con codice di errore
            }

            // Inizializzazione Core
            // Istanzia il Controller principale e apre la finestra di accesso.
            Controller controller = new Controller();
            LogInFrame loginFrame = new LogInFrame(controller);
            loginFrame.setVisible(true);
        });
    }
}