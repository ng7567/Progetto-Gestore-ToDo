package gui.frames;

import controller.Controller;
import gui.style.GuiUtils;
import gui.dialogs.RegisterDialog;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Rappresenta la finestra di accesso (Login) dell'applicazione.
 * Coordina le procedure di autenticazione dell'utente, gestisce la convalida delle
 * credenziali tramite il controller e abilita il reindirizzamento verso la dashboard principale.
 * Implementa inoltre un meccanismo di persistenza locale tramite le API {@link Preferences}
 * per memorizzare l'ultimo username utilizzato, ottimizzando l'esperienza di accesso successiva.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class LogInFrame extends JFrame {

    /** Il riferimento al gestore della logica di business per le operazioni di autenticazione. */
    private final transient Controller controller;

    /** Il nodo delle preferenze utente per la memorizzazione dei dati di configurazione locale. */
    private final transient Preferences prefs;

    /** La chiave identificativa utilizzata per salvare e recuperare l'ultimo username nel registro di sistema. */
    private static final String PREF_LAST_USER = "last_username";

    /** Il campo di input testuale per l'immissione dell'username. */
    private JTextField txtUsername;

    /** Il campo di input protetto per l'immissione della password. */
    private JPasswordField txtPassword;

    /**
     * Inizializza la finestra di login configurando il legame con il controller e
     * ripristinando le preferenze utente precedentemente salvate.
     *
     * @param controller Il riferimento al Controller per l'inoltro delle richieste di accesso.
     */
    public LogInFrame(Controller controller) {
        super("ToDo App - Login");
        this.controller = controller;
        this.prefs = Preferences.userNodeForPackage(LogInFrame.class);

        initUI();

        // Configurazione delle proprietà strutturali della finestra
        GuiUtils.setAppIcon(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 450);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * Struttura gerarchicamente l'interfaccia utente della schermata di accesso.
     * Impiega un layout a griglia ({@link GridBagLayout}) per assicurare il centraggio
     * dei componenti e predispone i moduli di input, integrando il valore di default
     * recuperato dalle impostazioni locali per l'username.
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(GuiUtils.getBackgroundColor());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Sezione Titolo ---
        JLabel lblTitle = new JLabel("BENTORNATO", SwingConstants.CENTER);
        lblTitle.setFont(GuiUtils.FONT_TITLE);
        lblTitle.setForeground(GuiUtils.getTextColor());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 30, 20);
        mainPanel.add(lblTitle, gbc);

        // Reset dei vincoli per i campi di input
        gbc.insets = new Insets(5, 20, 5, 20);
        gbc.gridwidth = 2;

        // --- Sezione Username ---
        gbc.gridy++;
        JLabel lblUser = GuiUtils.createLabel("Username");
        mainPanel.add(lblUser, gbc);

        gbc.gridy++;
        txtUsername = GuiUtils.createStandardTextField(15);
        // Ripristino dell'ultimo username salvato nel registro locale
        String lastUser = prefs.get(PREF_LAST_USER, "");
        txtUsername.setText(lastUser);
        mainPanel.add(txtUsername, gbc);

        // --- Sezione Password ---
        gbc.gridy++;
        gbc.insets = new Insets(15, 20, 5, 20);
        JLabel lblPass = GuiUtils.createLabel("Password");
        mainPanel.add(lblPass, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(5, 20, 5, 20);
        txtPassword = GuiUtils.createPasswordField(15);
        JPanel passwordPanel = GuiUtils.createPasswordPanelWithEye(txtPassword);
        mainPanel.add(passwordPanel, gbc);

        // --- Sezione Azioni ---
        gbc.gridy++;
        gbc.insets = new Insets(30, 20, 10, 20);
        JButton btnLogin = new JButton("Accedi");
        GuiUtils.stylePrimaryButton(btnLogin);
        btnLogin.addActionListener(e -> performLogin());
        mainPanel.add(btnLogin, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(5, 20, 20, 20);
        JButton btnRegister = new JButton("Non hai un account? Registrati");
        GuiUtils.styleLinkButton(btnRegister);
        btnRegister.addActionListener(e -> openRegisterDialog());
        mainPanel.add(btnRegister, gbc);

        add(mainPanel);

        // Vincola il tasto Invio all'esecuzione del tentativo di login
        this.getRootPane().setDefaultButton(btnLogin);
    }

    /**
     * Innesca l'apertura del dialog dedicato alla registrazione.
     * Utilizza l'istanza corrente come finestra proprietaria per mantenere
     * il vincolo di gerarchia visiva.
     */
    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this, controller);
        dialog.setVisible(true);
    }

    /**
     * Gestisce la procedura logica per l'autenticazione del profilo.
     * Esegue l'estrazione e la normalizzazione dei dati di input, demanda al controller
     * l'interrogazione del database e, in caso di esito positivo, provvede al salvataggio
     * persistente dell'username e alla transizione verso la schermata principale ({@link HomeFrame}).
     * Implementa una gestione delle eccezioni per isolare errori di credenziali
     * da anomalie tecniche di rete o database.
     */
    private void performLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Inserisci username e password!",
                    "Campi mancanti",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Inoltro della richiesta di autenticazione al livello di business
            controller.login(user, pass);

            // Consolidamento dell'username nelle preferenze locali al successo dell'operazione
            prefs.put(PREF_LAST_USER, user);
            this.dispose();
            new HomeFrame(controller).setVisible(true);

        } catch (exception.InvalidCredentialsException ex) {
            // Intercettazione di credenziali errate o utente inesistente
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Credenziali non valide",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (java.sql.SQLException ex) {
            // Gestione di interruzioni di connettività con il server PostgreSQL
            JOptionPane.showMessageDialog(
                    this,
                    "Errore di connessione al database. Riprova più tardi.",
                    "Errore Tecnico",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            // Paracadute per eccezioni runtime non catalogate
            JOptionPane.showMessageDialog(
                    this,
                    "Si è verificato un errore inaspettato durante l'accesso.",
                    "Errore",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}