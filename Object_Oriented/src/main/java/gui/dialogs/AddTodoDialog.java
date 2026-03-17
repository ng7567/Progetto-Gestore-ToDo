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
 * Finestra di dialogo modale per la creazione di un nuovo Task (ToDo).
 * Permette di inserire titolo, descrizione e opzioni avanzate come scadenza,
 * priorità, collaboratori e allegati.
 */
public class AddTodoDialog extends JDialog {

    private final transient Controller controller;
    private final int boardId;
    private boolean success = false;

    private static final String WARNING = "Attenzione";

    // Componenti UI principali
    private JTextField txtTitle;
    private JTextArea txtDescription;
    private JComboBox<Priority> comboPriority;

    // Componenti Data e Ora
    private JDateChooser dateChooser;
    private JSpinner timeSpinner;

    // Componenti Pannello Avanzato
    private JPanel panelAdvanced;
    private JButton btnToggleAdvanced;
    private JTextField txtLink;
    private JTextField txtImagePath;
    private Color selectedBackgroundColor = null;

    // Gestione Collaboratori
    private DefaultListModel<String> collaboratorsModel;
    private JList<String> listCollaborators;

    /**
     * Costruisce il dialogo per la creazione di un nuovo task.
     *
     * @param owner      La finestra proprietaria.
     * @param controller Il controller per la logica di business.
     * @param boardId    L'ID della bacheca in cui inserire il task.
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
     * Inizializza l'interfaccia utente principale della finestra di dialogo.
     * Configura il layout {@code GridBagLayout} per organizzare i campi di input
     * e imposta gli stili grafici standard tramite le utilità di sistema.
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
     * Crea e configura il pannello contenente il DateChooser e lo Spinner per l'ora.
     * Estrae i riferimenti ai componenti interni per permetterne l'accesso durante il salvataggio.
     *
     * @return Un oggetto {@code JPanel} contenente i selettori di data e ora sincronizzati.
     */
    private JPanel createDatePanel() {
        GuiUtils.DateTimePicker picker = GuiUtils.createDateTimePicker(null);
        this.dateChooser = picker.dateChooser();
        this.timeSpinner = picker.timeSpinner();
        return picker.panel();
    }

    /**
     * Inizializza i componenti del pannello avanzato del dialogo.
     * Configura i campi per l'inserimento di URL, la selezione del colore di sfondo,
     * la gestione degli allegati multimediali e l'aggiunta di collaboratori.
     *
     * @param p Il pannello contenitore in cui inserire i componenti avanzati.
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
     * Apre la finestra di dialogo per la ricerca e la selezione di utenti nel sistema.
     * Aggiunge l'utente selezionato alla lista dei collaboratori se non già presente.
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
     * Alterna la visibilità del pannello contenente le opzioni avanzate.
     * Aggiorna il testo dell'etichetta del pulsante e ridimensiona la finestra di dialogo.
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
     * Esegue la procedura di salvataggio del nuovo task.
     * Raccoglie i dati inseriti, convalida la scadenza e il titolo, gestisce il trasferimento
     * fisico dei file multimediali e comunica con il controller per la persistenza.
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
     * Combina la data selezionata con l'orario dello spinner per creare un Timestamp SQL.
     * Se l'orario non è specificato, imposta come default l'ultimo minuto del giorno (23:59).
     *
     * @param date La data (giorno/mese/anno) selezionata dall'utente.
     * @param time L'oggetto Date contenente le informazioni su ore e minuti dallo spinner.
     * @return Il {@code Timestamp} risultante dalla combinazione, o {@code null} se la data è nulla.
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
            // Default sicurezza
            calDate.set(Calendar.HOUR_OF_DAY, 23);
            calDate.set(Calendar.MINUTE, 59);
        }

        calDate.set(Calendar.SECOND, 0);
        return new Timestamp(calDate.getTimeInMillis());
    }

    /**
     * Indica se l'operazione di creazione del task si è conclusa con successo.
     *
     * @return {@code true} se il task è stato salvato correttamente nel database, {@code false} altrimenti.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Recupera la finestra principale (Window) che funge da antenato per il dialogo.
     *
     * @return L'oggetto {@code Window} gerarchicamente superiore a questo dialogo.
     */
    private Window getBaseFrame() {
        return SwingUtilities.getWindowAncestor(this);
    }
}