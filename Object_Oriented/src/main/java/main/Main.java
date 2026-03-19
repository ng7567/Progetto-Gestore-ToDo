package main;

import controller.Controller;
import gui.frames.LogInFrame;
import util.DBConnection;

import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Punto di ingresso dell'applicazione ToDo.
 * Questa classe configura le proprietà di sistema per il rendering grafico,
 * controlla che il database sia raggiungibile e avvia la schermata iniziale
 * dell'interfaccia utente.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class Main {

    /**
     * Costruttore privato.
     * La classe Main serve solo come punto di ingresso del programma
     * e non deve essere creata come oggetto.
     */
    private Main() {
        // Nessuna istanziazione necessaria
    }

    /**
     * Avvia l'intero sistema.
     * Configura la pipeline grafica per evitare glitch visivi e delega l'apertura
     * delle finestre all'Event Dispatch Thread (EDT) per garantire la thread-safety di Swing.
     *
     * @param args Argomenti da riga di comando (non utilizzati).
     */
    public static void main(String[] args) {
        // Configurazione grafica di sistema
        // Disabilita Direct3D per evitare problemi di visualizzazione su alcuni PC Windows.
        System.setProperty("sun.java2d.d3d", "false");

        // Forza FlatLaf a non usare librerie native esterne.
        System.setProperty("flatlaf.useNativeLibrary", "false");

        // Avvio dell'interfaccia grafica nel thread dedicato agli eventi
        SwingUtilities.invokeLater(() -> {

            // Controllo della connessione al database
            // Tenta di aprire una connessione di prova; se fallisce, l'app non parte.
            try (Connection testConn = DBConnection.getInstance().getConnection()) {
                if (testConn == null) {
                    throw new SQLException("La connessione ottenuta è nulla.");
                }
            } catch (SQLException e) {
                // Mostra un messaggio di errore all'utente e chiude il programma
                JOptionPane.showMessageDialog(null,
                        "Errore critico: Impossibile connettersi al Database.\n" +
                                "Dettaglio: " + e.getMessage() + "\n\n" +
                                "Verifica che PostgreSQL sia attivo e che i dati in DBConnection siano giusti.",
                        "Errore di Connessione",
                        JOptionPane.ERROR_MESSAGE);

                // Esce dal programma con un codice di errore
                System.exit(1);
            }

            // Avvio della logica e della finestra di login
            // Crea il Controller e apre la finestra per l'accesso dell'utente.
            Controller controller = new Controller();
            LogInFrame loginFrame = new LogInFrame(controller);
            loginFrame.setVisible(true);
        });
    }
}