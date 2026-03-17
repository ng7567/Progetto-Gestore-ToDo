package gui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import controller.Controller;
import gui.style.GuiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Finestra di dialogo modale per la ricerca e la selezione di utenti nel sistema.
 * Viene utilizzata principalmente per aggiungere collaboratori a un task.
 * Implementa la ricerca in tempo reale durante la digitazione, ottimizzata con un
 * meccanismo di "debounce" per evitare di sovraccaricare il database.
 */
public class UserSearchDialog extends JDialog {

    private final transient Controller controller;
    private JTextField txtSearch;
    private final DefaultListModel<String> listModel;
    private JList<String> listResults;
    private String selectedUser = null;
    private boolean confirmed = false;

    // Timer per il debouncing delle richieste al database durante la digitazione
    private Timer debounceTimer;

    /**
     * Costruisce il dialogo di ricerca utenti.
     *
     * @param owner      La finestra ({@link Window}) proprietaria del dialogo.
     * @param controller Il {@link Controller} per eseguire la ricerca degli utenti nel database.
     */
    public UserSearchDialog(Window owner, Controller controller) {
        super(owner, "Aggiungi utente", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.listModel = new DefaultListModel<>();

        initDebounceTimer();
        initUI();

        setSize(350, 400);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * Inizializza il timer di debounce. Attende 300 millisecondi dall'ultima
     * pressione di un tasto prima di eseguire effettivamente la query al database,
     * mantenendo l'interfaccia fluida.
     */
    private void initDebounceTimer() {
        debounceTimer = new Timer(300, e -> performSearch());
        debounceTimer.setRepeats(false);
    }

    /**
     * Inizializza l'interfaccia utente del dialogo di ricerca.
     * Configura la barra di ricerca con il meccanismo di ascolto in tempo reale,
     * predispone la lista per la visualizzazione dei risultati e organizza i
     * pulsanti di azione nel pannello inferiore.
     */
    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(GuiUtils.getBackgroundColor());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- Barra di ricerca (Nord) ---
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBackground(GuiUtils.getBackgroundColor());

        JLabel lblSearch = GuiUtils.createBoldLabel("Cerca nome:");
        searchPanel.add(lblSearch, BorderLayout.NORTH);

        txtSearch = GuiUtils.createStandardTextField(20);
        // Aggiunge un testo suggerimento (Placeholder)
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Digita l'username...");

        // Listener per ricerca real-time con debounce
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounceTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { debounceTimer.restart(); }
            public void changedUpdate(DocumentEvent e) { debounceTimer.restart(); }
        });
        searchPanel.add(txtSearch, BorderLayout.CENTER);

        panel.add(searchPanel, BorderLayout.NORTH);

        // --- Lista risultati (Centro) ---
        listResults = new JList<>(listModel);
        listResults.setBackground(GuiUtils.getInputBackground());
        listResults.setForeground(GuiUtils.getTextColor());
        listResults.setFont(GuiUtils.FONT_NORMAL);
        listResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Doppio click per confermare subito la selezione
        listResults.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    confirmSelection();
                }
            }
        });

        JScrollPane scrollPane = GuiUtils.createModernScrollPane(listResults);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Bottoni Azione (Sud) ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(GuiUtils.getBackgroundColor());

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dispose());

        JButton btnAdd = new JButton("Aggiungi");
        GuiUtils.stylePrimaryButton(btnAdd);
        btnAdd.addActionListener(e -> confirmSelection());

        btnPanel.add(btnCancel);
        btnPanel.add(btnAdd);

        panel.add(btnPanel, BorderLayout.SOUTH);

        add(panel);
    }

    /**
     * Esegue la ricerca degli utenti tramite il controller basandosi sul testo inserito.
     * Pulisce il modello attuale e lo aggiorna con i nuovi risultati ottenuti
     * dall'interrogazione al database.
     */
    private void performSearch() {
        String query = txtSearch.getText().trim();
        listModel.clear();

        if (!query.isEmpty()) {
            List<String> results = controller.searchUsers(query);
            // Utilizzo del method reference per aggiungere elementi alla lista (SonarQube compliant)
            results.forEach(listModel::addElement);
        }
    }

    /**
     * Conferma la selezione dell'utente attualmente evidenziato nella lista.
     * Registra l'username scelto e chiude il dialogo; se non è presente alcuna
     * selezione, emette un segnale acustico di avviso.
     */
    private void confirmSelection() {
        if (!listResults.isSelectionEmpty()) {
            selectedUser = listResults.getSelectedValue();
            confirmed = true;
            dispose();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Restituisce l'username dell'utente selezionato se l'operazione è stata confermata.
     *
     * @return L'username dell'utente scelto, oppure {@code null} se l'utente ha annullato
     * la ricerca o chiuso il dialogo senza confermare.
     */
    public String getSelectedUser() {
        return confirmed ? selectedUser : null;
    }
}