package gui.dialogs;

import controller.Controller;
import exception.BusinessLogicException;
import gui.style.GuiUtils;
import model.Board;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * Finestra di dialogo per la visualizzazione e modifica dei dettagli di una bacheca.
 * Consente di aggiornare il titolo e la descrizione in un'unica interfaccia.
 * Se la bacheca contiene task condivisi, blocca la modifica del titolo per evitare
 * problemi di sincronizzazione, ma permette comunque di aggiornare la descrizione.
 */
public class BoardDetailsDialog extends JDialog {

    private final transient Controller controller;
    private final transient Board board;
    private final transient Runnable onUpdate;
    private final boolean isTitleLocked;

    // Componenti UI
    private JTextField txtTitle;
    private JTextArea txtDesc;

    /**
     * Crea una nuova finestra di dettaglio per la bacheca specificata.
     *
     * @param owner      La finestra genitore ({@link Window}).
     * @param controller Il {@link Controller} per le operazioni di persistenza e validazione.
     * @param board      L'oggetto {@link Board} da visualizzare e modificare.
     * @param onUpdate   La callback da eseguire in caso di salvataggio riuscito.
     */
    public BoardDetailsDialog(Window owner, Controller controller, Board board, Runnable onUpdate) {
        super(owner, "Dettagli Bacheca", Dialog.ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.board = board;
        this.onUpdate = onUpdate;

        // Verifica i permessi solo per il titolo
        this.isTitleLocked = controller.isBoardLocked(board.getTitle(), controller.getCurrentUser().getId());

        initUI();
        loadData();

        setSize(450, 400);
        setLocationRelativeTo(owner);
    }

    /**
     * Inizializza l'interfaccia utente del dialogo.
     * Configura la disposizione dei pannelli principali, aggiunge gli avvisi dinamici
     * in caso di restrizioni sulle modifiche e organizza le sezioni di input.
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(GuiUtils.getBackgroundColor());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Avviso superiore in caso di titolo bloccato
        if (isTitleLocked) {
            mainPanel.add(createLockWarningLabel(), BorderLayout.NORTH);
        }

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);

        centerPanel.add(createTitleSection(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        centerPanel.add(createDescSection(), gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        add(mainPanel);
    }


    /**
     * Crea un'etichetta di avviso testuale con icona di sistema.
     * Viene visualizzata esclusivamente se la bacheca è bloccata per informare l'utente
     * che il titolo non è modificabile a causa di collaborazioni attive.
     *
     * @return Un oggetto {@code JLabel} configurato con stile di avviso.
     */
    private JLabel createLockWarningLabel() {
        JLabel lblWarning = new JLabel("<html><b>Attenzione:</b> La bacheca contiene task condivisi. Il titolo è bloccato, ma puoi modificare la descrizione.</html>");
        lblWarning.setForeground(new Color(231, 76, 60)); // Rosso
        lblWarning.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        lblWarning.setBorder(new EmptyBorder(0, 0, 10, 0));
        return lblWarning;
    }

    /**
     * Crea e configura la sezione dedicata alla modifica del titolo della bacheca.
     * Applica restrizioni di sola lettura al campo di testo se la bacheca risulta bloccata.
     *
     * @return Un {@code JPanel} contenente l'etichetta e il campo di input per il titolo.
     */
    private JPanel createTitleSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);

        JLabel lblTitle = GuiUtils.createBoldLabel("Nome della Bacheca:");
        txtTitle = GuiUtils.createStandardTextField(20);

        // Blocca SOLO il titolo se necessario
        txtTitle.setEditable(!isTitleLocked);
        if (isTitleLocked) {
            txtTitle.setToolTipText("Il titolo non può essere modificato perché contiene task condivisi.");
        }

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(txtTitle, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Crea e configura la sezione dedicata alla modifica della descrizione della bacheca.
     * Inserisce l'area di testo all'interno di un pannello di scorrimento moderno.
     *
     * @return Un {@code JPanel} contenente l'etichetta e l'area di testo per la descrizione.
     */
    private JPanel createDescSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);

        JLabel lblDesc = GuiUtils.createBoldLabel("Descrizione:");

        txtDesc = new JTextArea();
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);

        // La descrizione è SEMPRE modificabile
        txtDesc.setEditable(true);
        GuiUtils.styleTextArea(txtDesc);

        JScrollPane scroll = GuiUtils.createModernScrollPane(txtDesc);
        scroll.setBorder(GuiUtils.createStandardBorder());

        panel.add(lblDesc, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Crea il pannello dei pulsanti di azione posizionato nella parte inferiore del dialogo.
     * Configura i listener per l'annullamento dell'operazione e per il salvataggio dei dati.
     *
     * @return Un {@code JPanel} contenente i pulsanti "Annulla" e "Salva Modifiche".
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panel.setOpaque(false);

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dispose());
        panel.add(btnCancel);

        // Il bottone salva è sempre visibile
        JButton btnSave = new JButton("Salva Modifiche");
        GuiUtils.stylePrimaryButton(btnSave);
        GuiUtils.makeSameSize(btnCancel, btnSave);
        btnSave.addActionListener(e -> {
            try {
                saveChanges();
            } catch (SQLException | BusinessLogicException ex) {
                JOptionPane.showMessageDialog(
                        BoardDetailsDialog.this,
                        "Errore durante l'aggiornamento:\n" + ex.getMessage(),
                        "Errore Operazione",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
        panel.add(btnSave);
        getRootPane().setDefaultButton(btnSave);

        return panel;
    }

    /**
     * Carica i dati correnti della bacheca all'interno dei componenti di input.
     * Riempie i campi titolo e descrizione con i valori estratti dall'oggetto {@link Board}.
     */
    private void loadData() {
        txtTitle.setText(board.getTitle());
        if (board.getDescription() != null) {
            txtDesc.setText(board.getDescription());
        }
    }

    /**
     * Raccoglie i dati dai componenti UI e richiede il salvataggio delle modifiche al controller.
     * Effettua controlli di validazione sulla lunghezza del titolo e sulla presenza di duplicati
     * prima di procedere con la persistenza.
     *
     * @throws SQLException           Se si verifica un errore durante la comunicazione con il database.
     * @throws BusinessLogicException Se l'operazione viola le regole di business del sistema.
     */
    private void saveChanges() throws SQLException, BusinessLogicException {
        String newTitle = txtTitle.getText().trim();
        String newDesc = txtDesc.getText().trim();

        if (newTitle.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Il titolo è obbligatorio.", "Errore validazione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newTitle.length() > 100) {
            JOptionPane.showMessageDialog(this, "Il titolo è troppo lungo (massimo 100 caratteri).", "Errore validazione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Evita duplicati (escludendo se stessa dal controllo)
        boolean nameExists = controller.getUserBoards().stream()
                .anyMatch(b -> b.getId() != board.getId() && b.getTitle().equalsIgnoreCase(newTitle));

        if (nameExists) {
            JOptionPane.showMessageDialog(this, "Hai già una bacheca chiamata \"" + newTitle + "\".", "Nome duplicato", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Se il titolo non è cambiato (es. perché in read-only), il salvataggio andrà a buon fine aggiornando la descrizione
        if (controller.updateBoardDetails(board.getId(), newTitle, newDesc)) {
            board.setTitle(newTitle);
            board.setDescription(newDesc);

            if (onUpdate != null) {
                onUpdate.run();
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Errore durante il salvataggio dei dettagli.", "Errore DB", JOptionPane.ERROR_MESSAGE);
        }
    }
}