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
 * Rappresenta la finestra di dialogo modale per la ricerca e la selezione
 * degli utenti registrati nel sistema.
 * Viene impiegata principalmente per l'individuazione di nuovi collaboratori da associare
 * ai task. Implementa una logica di filtraggio in tempo reale durante la digitazione,
 * ottimizzata mediante un meccanismo di "debouncing" che riduce il carico computazionale
 * sul database prevenendo l'esecuzione di query ridondanti.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class UserSearchDialog extends JDialog {

    /** Il riferimento al gestore della logica di business per l'interrogazione dell'anagrafica utenti. */
    private final transient Controller controller;

    /** Il campo di testo per l'immissione della stringa di ricerca. */
    private JTextField txtSearch;

    /** Il modello dati sottostante che gestisce dinamicamente l'elenco dei risultati visualizzati. */
    private final DefaultListModel<String> listModel;

    /** Il componente visuale a lista per la rappresentazione grafica degli utenti trovati. */
    private JList<String> listResults;

    /** Memorizza l'username dell'utente selezionato prima della chiusura del dialogo. */
    private String selectedUser = null;

    /** Flag di stato che indica se l'utente ha confermato esplicitamente la selezione. */
    private boolean confirmed = false;

    /**
     * Il timer delegato alla gestione del debouncing.
     * Introduce un ritardo controllato tra la digitazione e l'effettiva esecuzione della query SQL.
     */
    private Timer debounceTimer;

    /**
     * Inizializza il dialogo di ricerca configurando la modalità di blocco e il legame con il controller.
     *
     * @param owner      La finestra ({@link Window}) proprietaria necessaria per il corretto posizionamento.
     * @param controller Il riferimento al Controller per l'esecuzione delle ricerche nell'anagrafica.
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
     * Configura il timer di debouncing impostando una soglia di latenza di 300 millisecondi.
     * Questo meccanismo assicura che l'interrogazione al database avvenga esclusivamente
     * in seguito a una pausa nella digitazione dell'utente, garantendo la fluidità della UI.
     */
    private void initDebounceTimer() {
        debounceTimer = new Timer(300, e -> performSearch());
        debounceTimer.setRepeats(false);
    }

    /**
     * Struttura gerarchicamente l'interfaccia utente del dialogo.
     * Predispone la barra di ricerca superiore, l'area centrale per i risultati
     * e la bottoniera inferiore per le azioni di conferma o annullamento.
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
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Digita l'username...");

        // Listener per il monitoraggio dei mutamenti nel documento di testo
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            /** {@inheritDoc} */
            public void insertUpdate(DocumentEvent e) { debounceTimer.restart(); }
            /** {@inheritDoc} */
            public void removeUpdate(DocumentEvent e) { debounceTimer.restart(); }
            /** {@inheritDoc} */
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

        // Gestisce la conferma rapida tramite interazione a doppio click
        listResults.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
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
     * Esegue l'interrogazione dell'anagrafica utenti inoltrando la stringa di ricerca al controller.
     * Sincronizza il modello della lista con l'output prodotto dalla query SQL, gestendo
     * lo svuotamento preventivo dei risultati obsoleti.
     */
    private void performSearch() {
        String query = txtSearch.getText().trim();
        listModel.clear();

        if (!query.isEmpty()) {
            List<String> results = controller.searchUsers(query);
            results.forEach(listModel::addElement);
        }
    }

    /**
     * Valida e consolida la scelta dell'utente.
     * Se un elemento risulta selezionato, ne estrae il valore e termina il ciclo di vita
     * del dialogo; in caso contrario, emette un segnale acustico di avviso.
     */
    private void confirmSelection() {
        if (!listResults.isSelectionEmpty()) {
            selectedUser = listResults.getSelectedValue();
            confirmed = true;
            dispose();
        } else {
            // Feedback acustico per indicare l'assenza di selezione
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Fornisce al chiamante l'esito della ricerca utente.
     *
     * @return L'username dell'utente selezionato se l'operazione è stata confermata;
     * {@code null} se il dialogo è stato annullato, chiuso o se non è stata
     * effettuata alcuna selezione.
     */
    public String getSelectedUser() {
        return confirmed ? selectedUser : null;
    }
}