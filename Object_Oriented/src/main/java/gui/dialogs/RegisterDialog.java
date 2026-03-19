package gui.dialogs;

import controller.Controller;
import gui.style.GuiUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Rappresenta la finestra di dialogo modale adibita alla registrazione di un nuovo profilo utente.
 * Coordina l'acquisizione delle credenziali (username e password), implementa i protocolli di
 * validazione della robustezza dei dati e gestisce la comunicazione bidirezionale con il
 * controller per la persistenza dell'account, garantendo il rispetto dei vincoli di sicurezza.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class RegisterDialog extends JDialog {

    /** Il riferimento al gestore della logica di business per le operazioni di registrazione. */
    private final transient Controller controller;

    /** Il campo di input testuale per l'immissione dell'username desiderato. */
    private JTextField txtUsername;

    /** Il campo di input protetto per l'immissione della password. */
    private JPasswordField txtPassword;

    /** Il campo di input protetto per la conferma della password, atto a prevenire refusi di battitura. */
    private JPasswordField txtConfirmPassword;

    /** Costante testuale utilizzata come intestazione per i messaggi di errore critico. */
    private static final String ERROR = "Errore";

    /**
     * Inizializza il dialogo di registrazione impostando la modalità di blocco (modal)
     * e configurando il posizionamento relativo rispetto al frame chiamante.
     *
     * @param parent     Il frame genitore utilizzato per l'ancoraggio visivo e la centratura.
     * @param controller Il riferimento al Controller per l'inoltro della richiesta di creazione profilo.
     */
    public RegisterDialog(Frame parent, Controller controller) {
        super(parent, "Nuovo utente", true); // Definizione del comportamento modale
        this.controller = controller;

        initUI();

        setSize(400, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    /**
     * Struttura gerarchicamente l'interfaccia utente del dialogo sfruttando un layout a griglia.
     * Predispone i controlli di input, integra suggerimenti visivi sulla complessità della
     * password e configura i trigger di azione (Registrati/Annulla) con i rispettivi gestori di evento.
     */
    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(GuiUtils.getBackgroundColor());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- TITOLO ---
        JLabel lblTitle = new JLabel("REGISTRATI", SwingConstants.CENTER);
        lblTitle.setFont(GuiUtils.FONT_TITLE);
        lblTitle.setForeground(GuiUtils.getTextColor());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 20, 10);
        panel.add(lblTitle, gbc);

        // Ripristino vincoli standard
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 15, 5, 5);

        // --- USERNAME ---
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(GuiUtils.createLabel("Username:"), gbc);

        gbc.gridx = 1;
        txtUsername = GuiUtils.createStandardTextField(15);
        panel.add(txtUsername, gbc);

        // --- PASSWORD ---
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(GuiUtils.createLabel("Password:"), gbc);

        gbc.gridx = 1;
        txtPassword = GuiUtils.createPasswordField(15);
        panel.add(GuiUtils.createPasswordPanelWithEye(txtPassword), gbc);

        // Informativa sui requisiti di sicurezza (Text Block Java 15+)
        gbc.gridy++;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 15, 10, 5);

        JLabel lblInfoPass = new JLabel("""
                <html><font size='3' color='gray'>
                8-24 car, 1 Maiusc, 1 Minusc,<br>
                1 Num, 1 Speciale (!@#$)
                </font></html>
                """);
        panel.add(lblInfoPass, gbc);

        // --- CONFERMA PASSWORD ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 15, 5, 5);
        panel.add(GuiUtils.createLabel("Conferma:"), gbc);

        gbc.gridx = 1;
        txtConfirmPassword = GuiUtils.createPasswordField(15);
        panel.add(GuiUtils.createPasswordPanelWithEye(txtConfirmPassword), gbc);

        // --- BOTTONI ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 15, 15, 15);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dispose());

        JButton btnRegister = new JButton("Registrati");
        GuiUtils.stylePrimaryButton(btnRegister);
        btnRegister.addActionListener(e -> performRegistration());

        GuiUtils.makeSameSize(btnCancel, btnRegister);

        btnPanel.add(btnCancel);
        btnPanel.add(btnRegister);
        panel.add(btnPanel, gbc);

        add(panel);

        // Imposta il pulsante di registrazione come azione predefinita alla pressione del tasto Invio
        this.getRootPane().setDefaultButton(btnRegister);
    }

    /**
     * Coordina la procedura logica per la creazione di un nuovo profilo utente.
     * Implementa un meccanismo di validazione "fail-fast" su parametri quali lunghezza,
     * corrispondenza delle password e conformità ai criteri di sicurezza prima di
     * interrogare il database, riducendo il carico di rete e gestendo puntualmente
     * le eccezioni di business (es. username già occupato).
     */
    private void performRegistration() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String confirmPass = new String(txtConfirmPassword.getPassword());

        // Validazioni di integrità locali
        if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Compila tutti i campi!", "Campi mancanti", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!user.matches("^[a-zA-Z0-9_.-]{3,20}$")) {
            JOptionPane.showMessageDialog(this, "Formato username non valido.", ERROR, JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Le password non coincidono!", ERROR, JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isValidPassword(pass)) {
            JOptionPane.showMessageDialog(this, "Password troppo debole.", ERROR, JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Tentativo di persistenza tramite controller
            controller.register(user, pass);

            JOptionPane.showMessageDialog(this,
                    "Registrazione completata con successo!\nOra puoi effettuare il login.",
                    "Successo",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (exception.UserAlreadyExistsException ex) {
            // Intercettazione specifica per violazione di univocità dello username
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Errore registrazione", JOptionPane.WARNING_MESSAGE);
        } catch (java.sql.SQLException ex) {
            // Gestione di anomalie a livello di connessione o driver DB
            JOptionPane.showMessageDialog(this, "Errore tecnico durante la registrazione.", "Errore DB", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            // Cattura di eccezioni impreviste (paracadute di sicurezza)
            JOptionPane.showMessageDialog(this, "Si è verificato un errore imprevisto.", ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Valuta se la stringa della password rispetta i criteri di complessità algoritmica definiti.
     * Sfrutta un'espressione regolare (Regex) per accertare contemporaneamente la presenza di
     * cifre, lettere (maiuscole/minuscole) e caratteri speciali, garantendo un elevato
     * standard di protezione degli account.
     *
     * @param password La stringa testuale della password da sottoporre a verifica.
     * @return {@code true} se la stringa soddisfa tutti i requisiti di sicurezza imposti; {@code false} altrimenti.
     */
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-])(?=\\S+$).{8,24}$";
        return password.matches(regex);
    }
}