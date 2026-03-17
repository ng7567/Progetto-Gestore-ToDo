package gui.frames;

import controller.Controller;
import gui.style.GuiUtils;
import gui.dialogs.RegisterDialog;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Finestra di login dell'applicazione.
 * Gestisce l'autenticazione dell'utente e il reindirizzamento alla dashboard principale.
 * Memorizza l'ultimo username utilizzato nelle preferenze locali.
 */
public class LogInFrame extends JFrame {

    private final transient Controller controller;
    private final transient Preferences prefs;
    private static final String PREF_LAST_USER = "last_username";

    // Componenti grafici
    private JTextField txtUsername;
    private JPasswordField txtPassword;

    /**
     * Costruisce la finestra di login.
     *
     * @param controller Il controller per la gestione della logica di autenticazione.
     */
    public LogInFrame(Controller controller) {
        super("ToDo App - Login");
        this.controller = controller;
        this.prefs = Preferences.userNodeForPackage(LogInFrame.class);

        initUI();

        // Impostazioni base della finestra
        GuiUtils.setAppIcon(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 450); // Leggermente più alta per spaziatura migliore
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * Inizializza l'interfaccia utente della schermata di accesso.
     * Configura il layout {@code GridBagLayout} per centrare i componenti, predispone
     * i campi di input per le credenziali (incluso il recupero dell'ultimo username)
     * e definisce la gerarchia visiva dei pulsanti di azione.
     */
    private void initUI() {
        // Pannello principale con colore di sfondo
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(GuiUtils.getBackgroundColor());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20); // Margini laterali più ampi
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- TITOLO ---
        JLabel lblTitle = new JLabel("BENTORNATO", SwingConstants.CENTER);
        lblTitle.setFont(GuiUtils.FONT_TITLE);
        lblTitle.setForeground(GuiUtils.getTextColor());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Occupa tutta la larghezza
        gbc.insets = new Insets(20, 20, 30, 20); // Più spazio sotto il titolo
        mainPanel.add(lblTitle, gbc);

        // Reset constraints per i campi
        gbc.insets = new Insets(5, 20, 5, 20);
        gbc.gridwidth = 2; // I campi occupano tutta la larghezza per design più pulito

        // --- USERNAME ---
        gbc.gridy++;
        JLabel lblUser = GuiUtils.createLabel("Username");
        mainPanel.add(lblUser, gbc);

        gbc.gridy++;
        txtUsername = GuiUtils.createStandardTextField(15);
        // Recupera l'ultimo utente salvato
        String lastUser = prefs.get(PREF_LAST_USER, "");
        txtUsername.setText(lastUser);
        mainPanel.add(txtUsername, gbc);

        // --- PASSWORD ---
        gbc.gridy++;
        gbc.insets = new Insets(15, 20, 5, 20); // Spazio extra sopra la label password
        JLabel lblPass = GuiUtils.createLabel("Password");
        mainPanel.add(lblPass, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(5, 20, 5, 20);
        txtPassword = GuiUtils.createPasswordField(15);
        JPanel passwordPanel = GuiUtils.createPasswordPanelWithEye(txtPassword);
        mainPanel.add(passwordPanel, gbc);

        // --- BOTTONE LOGIN ---
        gbc.gridy++;
        gbc.insets = new Insets(30, 20, 10, 20); // Spazio prima del bottone
        JButton btnLogin = new JButton("Accedi");
        GuiUtils.stylePrimaryButton(btnLogin);
        // Evento Login
        btnLogin.addActionListener(e -> performLogin());
        mainPanel.add(btnLogin, gbc);

        // --- BOTTONE REGISTRAZIONE ---
        gbc.gridy++;
        gbc.insets = new Insets(5, 20, 20, 20);
        JButton btnRegister = new JButton("Non hai un account? Registrati");
        GuiUtils.styleLinkButton(btnRegister);
        // Evento Registrazione
        btnRegister.addActionListener(e -> openRegisterDialog());
        mainPanel.add(btnRegister, gbc);

        add(mainPanel);

        // Imposta il tasto Invio per attivare il login
        this.getRootPane().setDefaultButton(btnLogin);
    }

    /**
     * Apre il dialogo modale dedicato alla registrazione di un nuovo profilo utente.
     * Passa il riferimento della finestra attuale come proprietaria per garantire
     * il corretto posizionamento del dialog.
     */
    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this, controller);
        dialog.setVisible(true);
    }

    /**
     * Esegue la procedura logica per l'autenticazione dell'utente nel sistema.
     * Recupera le credenziali dai campi di input, le valida formalmente e le inoltra
     * al controller. In caso di successo, memorizza l'username nelle preferenze locali
     * e avvia la transizione verso la dashboard principale (HomeFrame).
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
            // Tenta il login: se le credenziali sono errate, viene lanciata l'eccezione
            controller.login(user, pass);

            // Se arriva qui, il login è riuscito
            prefs.put(PREF_LAST_USER, user);
            this.dispose();
            new HomeFrame(controller).setVisible(true);

        } catch (exception.InvalidCredentialsException ex) {
            // Mostra il messaggio specifico definito nell'eccezione
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Credenziali non valide",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (java.sql.SQLException ex) {
            // Gestione dell'errore tecnico del database
            JOptionPane.showMessageDialog(
                    this,
                    "Errore di connessione al database. Riprova più tardi.",
                    "Errore Tecnico",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            // Errori imprevisti
            JOptionPane.showMessageDialog(
                    this,
                    "Si è verificato un errore inaspettato durante l'accesso.",
                    "Errore",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}