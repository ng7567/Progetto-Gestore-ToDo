package gui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import controller.Controller;
import gui.style.GuiUtils;
import model.Priority;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import com.toedter.calendar.JDateChooser;
import dto.TodoCreationDTO;
import util.FileUtils;

import java.util.Calendar;
import java.util.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta la finestra di dialogo modale adibita alla creazione di un nuovo Task (ToDo).
 * Fornisce un'interfaccia utente strutturata a comparsa progressiva, permettendo l'inserimento
 * dei metadati di base (titolo, descrizione) e offrendo un pannello espandibile per la
 * configurazione delle opzioni avanzate (scadenza temporale, priorità, collaboratori,
 * allegati multimediali e colore di sfondo personalizzato).
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class AddTodoDialog extends JDialog {

    /** Il riferimento al gestore della logica applicativa per le operazioni di persistenza. */
    private final transient Controller controller;

    /** L'identificativo univoco della bacheca genitrice alla quale associare il nuovo task. */
    private final int boardId;

    /** Flag di stato che indica se l'operazione di salvataggio nel database si è conclusa con esito positivo. */
    private boolean success = false;

    /** Stringa costante utilizzata come titolo predefinito per i popup di avviso (Warning). */
    private static final String WARNING = "Attenzione";

    // --- Componenti UI Principali ---

    /** Il campo di testo a riga singola per l'inserimento del titolo obbligatorio del task. */
    private JTextField txtTitle;

    /** L'area di testo multi-riga per l'inserimento della descrizione opzionale del task. */
    private JTextArea txtDescription;

    /** Il menu a tendina per la selezione del livello di priorità (enum {@link Priority}). */
    private JComboBox<Priority> comboPriority;

    // --- Componenti Data e Ora ---

    /** Il componente grafico a calendario per la selezione visuale della data di scadenza. */
    private JDateChooser dateChooser;

    /** Il selettore numerico a rotazione per l'impostazione dell'orario (ore e minuti) della scadenza. */
    private JSpinner timeSpinner;

    // --- Componenti Pannello Avanzato ---

    /** Il contenitore grafico espandibile/collassabile che ospita le opzioni di configurazione secondarie. */
    private JPanel panelAdvanced;

    /** Il pulsante interattivo delegato all'alternanza della visibilità del pannello avanzato. */
    private JButton btnToggleAdvanced;

    /** Il campo di testo per l'inserimento di un URL di collegamento ipertestuale opzionale. */
    private JTextField txtLink;

    /** Il campo di testo (in sola lettura) che mostra il percorso nel file system dell'immagine allegata. */
    private JTextField txtImagePath;

    /** L'oggetto Color che memorizza la tinta di sfondo personalizzata scelta dall'utente (null se non specificata). */
    private Color selectedBackgroundColor = null;

    // --- Gestione Collaboratori ---

    /** Il modello dati sottostante che gestisce l'elenco in tempo reale degli username associati come collaboratori. */
    private DefaultListModel<String> collaboratorsModel;

    /** Il componente visuale a lista che espone graficamente gli elementi contenuti nel {@code collaboratorsModel}. */
    private JList<String> listCollaborators;

    /**
     * Inizializza la finestra di dialogo modale, bloccando l'interazione con la finestra
     * chiamante fino alla conclusione del processo di creazione o all'annullamento.
     *
     * @param owner      La finestra grafica proprietaria (genitore) da cui scaturisce il dialogo.
     * @param controller Il riferimento al Controller per l'inoltro delle richieste di business.
     * @param boardId    L'identificativo relazionale della bacheca di destinazione.
     */
    public AddTodoDialog(Window owner, Controller controller, int boardId) {
        super(owner, "Nuova task", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.boardId = boardId;

        initUI();

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * Struttura gerarchicamente l'interfaccia utente primaria della finestra di dialogo.
     * Applica un layout flessibile a griglia ({@link GridBagLayout}) per posizionare
     * i campi di input essenziali e aggancia i listener ai pulsanti di azione (Salva/Annulla).
     */
    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(GuiUtils.getBackgroundColor());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Titolo
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(GuiUtils.createBoldLabel("Titolo"), gbc);

        gbc.gridy = 1;
        txtTitle = GuiUtils.createStandardTextField(20);
        panel.add(txtTitle, gbc);

        // Descrizione
        gbc.gridy = 2;
        gbc.weighty = 0;
        panel.add(GuiUtils.createBoldLabel("Descrizione (opzionale):"), gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        txtDescription = new JTextArea(5, 20);
        txtDescription.setFont(GuiUtils.FONT_NORMAL);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        GuiUtils.styleTextArea(txtDescription);

        JScrollPane scrollDesc = GuiUtils.createModernScrollPane(txtDescription);
        scrollDesc.setPreferredSize(new Dimension(300, 100));
        scrollDesc.setBorder(GuiUtils.createStandardBorder());
        panel.add(scrollDesc, gbc);

        // Priorità
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 4;
        panel.add(GuiUtils.createBoldLabel("Priorità:"), gbc);

        gbc.gridy = 5;
        comboPriority = new JComboBox<>(Priority.values());
        GuiUtils.stylePriorityComboBox(comboPriority);
        panel.add(comboPriority, gbc);

        // Scadenza (Panel Data + Ora estratto)
        gbc.gridy = 6;
        panel.add(GuiUtils.createBoldLabel("Scadenza (Opzionale): "), gbc);

        gbc.gridy = 7;
        panel.add(createDatePanel(), gbc);

        // Toggle Avanzate
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        btnToggleAdvanced = new JButton("<html><u>Mostra altre opzioni &#9660;</u></html>");
        GuiUtils.styleLinkButton(btnToggleAdvanced);
        btnToggleAdvanced.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleAdvanced.addActionListener(e -> toggleAdvancedOptions());
        panel.add(btnToggleAdvanced, gbc);

        // Pannello Avanzato (Nascosto di default)
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panelAdvanced = new JPanel(new GridBagLayout());
        panelAdvanced.setBackground(GuiUtils.getBackgroundColor());
        TitledBorder border = BorderFactory.createTitledBorder("Opzioni extra");
        border.setTitleColor(GuiUtils.getTextColor());
        border.setTitleFont(GuiUtils.FONT_BOLD);
        panelAdvanced.setBorder(border);
        panelAdvanced.setVisible(false);

        initAdvancedPanel(panelAdvanced);
        panel.add(panelAdvanced, gbc);

        // Bottoni Azione
        gbc.gridy = 10;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(GuiUtils.getBackgroundColor());

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Salva");
        GuiUtils.stylePrimaryButton(btnSave);
        btnSave.addActionListener(e -> saveTodo());

        GuiUtils.makeSameSize(btnCancel, btnSave);
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        panel.add(btnPanel, gbc);

        add(panel);
        getRootPane().setDefaultButton(btnSave);
    }

    /**
     * Istanzia e configura il sottomodulo temporale delegato alla cattura della data e dell'orario.
     * Estrae e memorizza i riferimenti interni al {@code JDateChooser} e al {@code JSpinner}
     * per consentirne l'interrogazione successiva durante la fase di salvataggio.
     *
     * @return L'oggetto {@code JPanel} aggregato pronto per l'inserimento nel layout principale.
     */
    private JPanel createDatePanel() {
        GuiUtils.DateTimePicker picker = GuiUtils.createDateTimePicker(null);
        this.dateChooser = picker.dateChooser();
        this.timeSpinner = picker.timeSpinner();
        return picker.panel();
    }

    /**
     * Struttura i componenti funzionali incapsulati all'interno del pannello avanzato.
     * Configura i moduli per la selezione interattiva del colore (tramite {@link JColorChooser}),
     * il caricamento degli allegati multimediali (tramite {@link JFileChooser}) e la gestione
     * dinamica della lista dei collaboratori.
     *
     * @param p Il pannello contenitore di destinazione in cui iniettare i controlli avanzati.
     */
    private void initAdvancedPanel(JPanel p) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx = 0; g.gridy = 0;

        // LINK
        p.add(GuiUtils.createBoldLabel("Link (URL):"), g);
        g.gridx = 1;
        txtLink = GuiUtils.createStandardTextField(15);
        txtLink.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "https://esempio.com");
        p.add(txtLink, g);

        // COLORE
        g.gridy++;
        g.gridx = 0;
        p.add(GuiUtils.createBoldLabel("Colore Sfondo:"), g);

        g.gridx = 1;
        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        colorRow.setBackground(GuiUtils.getBackgroundColor());

        JButton btnChooseColor = new JButton("Scegli un Colore");
        GuiUtils.stylePrimaryButton(btnChooseColor);
        JPanel colorPreviewPanel = new JPanel();
        colorPreviewPanel.setPreferredSize(new Dimension(25, 25));
        colorPreviewPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        colorPreviewPanel.setBackground(Color.WHITE);
        colorPreviewPanel.setOpaque(true);

        btnChooseColor.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Scegli colore sfondo", selectedBackgroundColor);
            if(newColor != null) {
                selectedBackgroundColor = newColor;
                colorPreviewPanel.setBackground(newColor);
            }
        });

        colorRow.add(colorPreviewPanel);
        colorRow.add(Box.createHorizontalStrut(10));
        colorRow.add(btnChooseColor);
        p.add(colorRow, g);

        // IMMAGINE
        g.gridy++;
        g.gridx = 0;
        p.add(GuiUtils.createBoldLabel("Immagine:"), g);

        g.gridx = 1;
        JPanel imgRow = new JPanel(new BorderLayout(5, 0));
        imgRow.setBackground(GuiUtils.getBackgroundColor());

        txtImagePath = GuiUtils.createStandardTextField(15);
        txtImagePath.setEditable(false);

        JButton btnBrowse = new JButton("...");
        GuiUtils.stylePrimaryButton(btnBrowse);
        btnBrowse.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "Immagini (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));

            int result = fileChooser.showOpenDialog(this);
            if(result == JFileChooser.APPROVE_OPTION) {
                txtImagePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        imgRow.add(txtImagePath, BorderLayout.CENTER);
        imgRow.add(btnBrowse, BorderLayout.EAST);
        p.add(imgRow, g);

        // COLLABORATORI
        g.gridy++;
        g.gridx = 0;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(10, 5, 5, 5);
        p.add(GuiUtils.createBoldLabel("Collaboratori:"), g);

        g.gridx = 1;
        g.anchor = GridBagConstraints.CENTER;
        g.insets = new Insets(5, 5, 5, 5);

        JPanel collabPanel = new JPanel(new BorderLayout(0, 5));
        collabPanel.setOpaque(true);
        collabPanel.setBackground(GuiUtils.getBackgroundColor());

        collaboratorsModel = new DefaultListModel<>();
        listCollaborators = new JList<>(collaboratorsModel);
        listCollaborators.setBackground(GuiUtils.getInputBackground());
        listCollaborators.setForeground(GuiUtils.getTextColor());
        listCollaborators.setFont(GuiUtils.FONT_NORMAL);

        JScrollPane scrollCollab = GuiUtils.createModernScrollPane(listCollaborators);
        scrollCollab.setPreferredSize(new Dimension(100, 60));

        collabPanel.add(scrollCollab, BorderLayout.CENTER);

        JButton btnAddUser = new JButton();
        GuiUtils.styleSecondaryButton(btnAddUser);
        Icon icon = GuiUtils.loadSVG("icons/user/add-user.svg", 20, 20);
        if (icon != null) btnAddUser.setIcon(icon); else btnAddUser.setText("+");
        btnAddUser.setPreferredSize(new Dimension(40, 60));
        btnAddUser.setToolTipText("Aggiungi collaboratore");
        btnAddUser.addActionListener(e -> openUserSearchDialog());

        collabPanel.add(btnAddUser, BorderLayout.EAST);
        p.add(collabPanel, g);
    }

    /**
     * Innesca la comparsa del dialogo modale di ricerca e selezione degli utenti.
     * Intercetta la risposta e, previe verifiche di univocità, immette la selezione
     * nel modello dati dei collaboratori, aggiornando in tempo reale la visualizzazione.
     */
    private void openUserSearchDialog() {
        UserSearchDialog searchDialog = new UserSearchDialog(this, controller);
        searchDialog.setVisible(true);
        String newUser = searchDialog.getSelectedUser();

        if(newUser != null) {
            if(!collaboratorsModel.contains(newUser)) {
                collaboratorsModel.addElement(newUser);
                listCollaborators.ensureIndexIsVisible(collaboratorsModel.getSize() - 1);
            } else {
                JOptionPane.showMessageDialog(this, "L'utente " + newUser + " è già presente.", WARNING, JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Alterna la visibilità del blocco delle opzioni avanzate (Toggle behavior).
     * Modifica contestualmente il testo dell'etichetta direzionale sul pulsante e invoca
     * la ricalibrazione del layout ({@code pack()}) per adattare le dimensioni della finestra.
     */
    private void toggleAdvancedOptions() {
        boolean isVisible = panelAdvanced.isVisible();
        panelAdvanced.setVisible(!isVisible);

        if (!isVisible) {
            btnToggleAdvanced.setText("<html><u>Nascondi opzioni &#9650;</u></html>");
        } else {
            btnToggleAdvanced.setText("<html><u>Mostra altre opzioni &#9660;</u></html>");
        }
        pack();
    }

    /**
     * Coordina il flusso transazionale per il consolidamento dei dati immessi in un nuovo record.
     * Esegue le procedure di validazione preventiva sull'input, compone il payload tramite
     * il pattern DTO ({@link TodoCreationDTO}), intercetta le eccezioni di business logic
     * (es. vincoli di auto-condivisione) e ordina al controller la persistenza dei dati.
     */
    private void saveTodo() {
        String title = txtTitle.getText().trim();
        String desc = txtDescription.getText().trim();
        Priority priority = (Priority) comboPriority.getSelectedItem();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Inserisci almeno il titolo!", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (title.length() > 200) {
            JOptionPane.showMessageDialog(this, "Il titolo è troppo lungo (massimo 200 caratteri).", "Errore validazione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Calcola la scadenza combinata usando il metodo helper
        Timestamp expiryDate = getCombinedTimestamp(dateChooser.getDate(), (Date) timeSpinner.getValue());

        if (expiryDate != null && expiryDate.before(new Timestamp(System.currentTimeMillis()))) {
            JOptionPane.showMessageDialog(getBaseFrame(),
                    "Non puoi impostare una scadenza nel passato!",
                    "Data non valida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String link = txtLink.getText().trim();
        String rawImagePath = txtImagePath.getText().trim();
        String finalImagePath = null;

        if (!rawImagePath.isEmpty()) {
            finalImagePath = FileUtils.saveImage(rawImagePath);
            if (finalImagePath == null) {
                JOptionPane.showMessageDialog(this,
                        "Impossibile copiare l'immagine. Il task verrà salvato senza foto.",
                        WARNING, JOptionPane.WARNING_MESSAGE);
            }
        }

        List<String> collaborators = new ArrayList<>();
        for (int i = 0; i < collaboratorsModel.getSize(); i++) {
            collaborators.add(collaboratorsModel.getElementAt(i));
        }

        TodoCreationDTO newTodoData = new TodoCreationDTO(
                boardId,
                title,
                desc,
                expiryDate,
                priority,
                link,
                finalImagePath,
                selectedBackgroundColor,
                collaborators,
                0
        );

        try {
            // La chiamata ora può sollevare eccezioni di business logic
            boolean ok = controller.addTodo(newTodoData);

            if (ok) {
                success = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Errore nel salvataggio del task.", "Errore DB", JOptionPane.ERROR_MESSAGE);
            }
        } catch (exception.SelfSharingException ex) {
            // Gestione specifica dell'eccezione: mostra il messaggio "Non puoi condividere con te stesso"
            JOptionPane.showMessageDialog(this, ex.getMessage(), WARNING, JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            // Paracadute per errori tecnici imprevisti
            JOptionPane.showMessageDialog(this, "Si è verificato un errore: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sincronizza il giorno selezionato con le componenti orarie scelte nello spinner per
     * forgiare un indicatore temporale compatibile con lo standard SQL. In assenza di specifica
     * oraria, ripiega sull'ultimo istante utile della giornata selezionata (23:59:00).
     *
     * @param date L'entità {@code Date} rappresentante le frazioni spaziali anno/mese/giorno.
     * @param time L'entità {@code Date} deputata al trasporto delle frazioni temporali ore/minuti.
     * @return L'oggetto {@link Timestamp} processato per la query, o {@code null} se il giorno non è specificato.
     */
    private Timestamp getCombinedTimestamp(Date date, Date time) {
        if (date == null) return null;

        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);

        if (time != null) {
            Calendar calTime = Calendar.getInstance();
            calTime.setTime(time);
            calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
            calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
        } else {
            // Default sicurezza: fine della giornata
            calDate.set(Calendar.HOUR_OF_DAY, 23);
            calDate.set(Calendar.MINUTE, 59);
        }

        calDate.set(Calendar.SECOND, 0);
        calDate.set(Calendar.MILLISECOND, 0); // Azzera i millisecondi per evitare incongruenze DB
        return new Timestamp(calDate.getTimeInMillis());
    }

    /**
     * Interroga lo stato conclusivo del ciclo di vita del dialogo.
     * Utile al frame chiamante per determinare se è necessario innescare un aggiornamento visivo (refresh).
     *
     * @return {@code true} se la catena di persistenza nel database si è conclusa senza anomalie; {@code false} altrimenti.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Recupera l'astrazione Window genitore al vertice dell'albero gerarchico grafico.
     * Per garantire la corretta interruzione logica modale dei sottomenù d'avviso.
     *
     * @return L'entità {@link Window} superiore, o {@code null} se il frame risulta svincolato.
     */
    private Window getBaseFrame() {
        return SwingUtilities.getWindowAncestor(this);
    }
}