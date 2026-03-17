package gui.dialogs;

import controller.Controller;
import gui.style.GuiUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogo modale per la registrazione di un nuovo utente.
 * Gestisce l'input di username e password, la validazione dei dati
 * e la comunicazione con il controller per la creazione dell'account.
 */
public class RegisterDialog extends JDialog {

    private final transient Controller controller;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;

    private static final String ERROR = "Errore";

    /**
     * Costruisce il dialogo di registrazione.
     *
     * @param parent     Il frame genitore (per centrare il dialogo).
     * @param controller Il controller per gestire la logica di registrazione.
     */
    public RegisterDialog(Frame parent, Controller controller) {
        super(parent, "Nuovo utente", true); // Modale
        this.controller = controller;

        initUI();

        setSize(400, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    /**
     * Inizializza l'interfaccia utente del dialogo di registrazione.
     * Predispone i campi di testo per le credenziali, include i suggerimenti per la
     * sicurezza della password e configura i pulsanti di azione con i relativi listener.
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

        // Reset constraints
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 15, 5, 5); // Spazio standard

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

        // Info Password (Text Block Java 15+)
        gbc.gridy++;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 15, 10, 5); // Meno spazio sopra

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
        gbc.insets = new Insets(5, 15, 5, 5); // Ripristina margine
        panel.add(GuiUtils.createLabel("Conferma:"), gbc);

        gbc.gridx = 1;
        txtConfirmPassword = GuiUtils.createPasswordField(15);
        panel.add(GuiUtils.createPasswordPanelWithEye(txtConfirmPassword), gbc);

        // --- BOTTONI ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2; // Occupa tutta la larghezza per il pannello bottoni
        gbc.insets = new Insets(25, 15, 15, 15);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dispose());

        JButton btnRegister = new JButton("Registrati");
        GuiUtils.stylePrimaryButton(btnRegister);
        btnRegister.addActionListener(e -> performRegistration());

        GuiUtils.makeSameSize(btnCancel, btnRegister); // Uniforma dimensioni

        btnPanel.add(btnCancel);
        btnPanel.add(btnRegister);
        panel.add(btnPanel, gbc);

        add(panel);

        // Tasto Invio attiva la registrazione
        this.getRootPane().setDefaultButton(btnRegister);
    }

    /**
     * Esegue la procedura logica per la registrazione di un nuovo profilo utente.
     * Valida la conformità dei dati inseriti rispetto ai requisiti di sistema (lunghezza,
     * caratteri speciali, corrispondenza password) e inoltra la richiesta al controller
     * gestendo eventuali errori di duplicazione o problemi tecnici del database.
     */
    private void performRegistration() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String confirmPass = new String(txtConfirmPassword.getPassword());

        // Validazioni locali (rimangono invariate per non pesare sul DB)
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

        // Chiamata al Controller con gestione Eccezioni
        try {
            controller.register(user, pass);

            // Se non vengono lanciate eccezioni, la registrazione è riuscita
            JOptionPane.showMessageDialog(this,
                    "Registrazione completata con successo!\nOra puoi effettuare il login.",
                    "Successo",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (exception.UserAlreadyExistsException ex) {
            // Gestione nome duplicato
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Errore registrazione", JOptionPane.WARNING_MESSAGE);
        } catch (java.sql.SQLException ex) {
            // Gestione errore database
            JOptionPane.showMessageDialog(this, "Errore tecnico durante la registrazione.", "Errore DB", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            // Errore generico
            JOptionPane.showMessageDialog(this, "Si è verificato un errore imprevisto.", ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Verifica se la stringa della password soddisfa i criteri minimi di complessità richiesti.
     * Il controllo accerta la presenza di numeri, lettere maiuscole, minuscole,
     * caratteri speciali e l'assenza di spazi bianchi, in un range di 8-24 caratteri.
     *
     * @param password La stringa testuale della password da sottoporre a verifica.
     * @return {@code true} se la password rispetta tutti i criteri di sicurezza, {@code false} altrimenti.
     */
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-])(?=\\S+$).{8,24}$";
        return password.matches(regex);
    }
}