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
 * Rappresenta la finestra di dialogo modale delegata alla visualizzazione e all'ispezione
 * dei metadati estesi di una singola bacheca (Board).
 * Espone un'interfaccia unificata per la modifica contestuale del titolo e della descrizione.
 * Implementa una logica di protezione preventiva: qualora la bacheca ospiti task soggetti a
 * condivisione, inibisce la mutazione del titolo (rendendolo in sola lettura) per prevenire
 * disallineamenti o corruzioni relazionali nel database, pur mantenendo inalterata
 * la possibilità di aggiornare liberamente il campo descrittivo.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class BoardDetailsDialog extends JDialog {

    /** Il riferimento al gestore della logica applicativa per le operazioni di persistenza e validazione. */
    private final transient Controller controller;

    /** L'astrazione di dominio (Entity) rappresentante la bacheca attualmente in ispezione. */
    private final transient Board board;

    /** L'azione di callback da invocare al termine di una transazione di salvataggio conclusasi con successo. */
    private final transient Runnable onUpdate;

    /** Flag di stato di sicurezza che determina se il campo del titolo debba essere reso in sola lettura. */
    private final boolean isTitleLocked;

    // --- Componenti UI ---

    /** Il campo di testo a riga singola per l'esposizione e la modifica del titolo della bacheca. */
    private JTextField txtTitle;

    /** L'area di testo multi-riga per l'esposizione e la modifica della descrizione estesa della bacheca. */
    private JTextArea txtDesc;

    /**
     * Inizializza l'infrastruttura grafica della finestra di dettaglio, valutando istantaneamente
     * i permessi di modifica in base alla presenza di vincoli di condivisione sulla bacheca target.
     *
     * @param owner      Il contenitore grafico genitore ({@link Window}) da cui scaturisce il dialogo.
     * @param controller Il riferimento al {@link Controller} per orchestrare le query di verifica e persistenza.
     * @param board      L'istanza dell'entità {@link Board} da iniettare nei campi del modulo.
     * @param onUpdate   La funzione di callback per notificare al chiamante l'avvenuta mutazione dello stato.
     */
    public BoardDetailsDialog(Window owner, Controller controller, Board board, Runnable onUpdate) {
        super(owner, "Dettagli Bacheca", Dialog.ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.board = board;
        this.onUpdate = onUpdate;

        // Verifica proattiva dei permessi di modifica sul titolo delegata al controller
        this.isTitleLocked = controller.isBoardLocked(board.getTitle(), controller.getCurrentUser().getId());

        initUI();
        loadData();

        setSize(450, 400);
        setLocationRelativeTo(owner);
    }

    /**
     * Struttura gerarchicamente l'albero dei componenti grafici della finestra di dialogo.
     * Coordina l'assemblaggio dei pannelli di input e, condizionatamente allo stato
     * di blocco ({@code isTitleLocked}), istanzia i moduli di avviso visivo per l'utente.
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(GuiUtils.getBackgroundColor());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Iniezione condizionale del banner di avviso superiore
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
     * Genera un'etichetta di avviso testuale integrato con l'iconografia
     * di sistema per le segnalazioni di Warning.
     * Il modulo viene attivato esclusivamente se la bacheca risulta bloccata, fornendo
     * trasparenza sulle limitazioni in corso.
     *
     * @return L'oggetto {@code JLabel} configurato con cromie e testi di allerta.
     */
    private JLabel createLockWarningLabel() {
        JLabel lblWarning = new JLabel("<html><b>Attenzione:</b> La bacheca contiene task condivisi. Il titolo è bloccato, ma puoi modificare la descrizione.</html>");
        lblWarning.setForeground(new Color(231, 76, 60)); // Tonalità rossa per evidenziare il vincolo
        lblWarning.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        lblWarning.setBorder(new EmptyBorder(0, 0, 10, 0));
        return lblWarning;
    }

    /**
     * Istanzia e formatta il sotto-pannello ospitante l'etichetta e il campo di immissione per il titolo.
     * Applica dinamicamente le restrizioni di read-only al componente di input
     * qualora il flag architetturale di blocco risulti asserito.
     *
     * @return Il contenitore {@code JPanel} aggregato per l'intestazione.
     */
    private JPanel createTitleSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);

        JLabel lblTitle = GuiUtils.createBoldLabel("Nome della Bacheca:");
        txtTitle = GuiUtils.createStandardTextField(20);

        // Applica il vincolo di immutabilità esclusivamente al campo titolo
        txtTitle.setEditable(!isTitleLocked);
        if (isTitleLocked) {
            txtTitle.setToolTipText("Il titolo non può essere modificato perché contiene task condivisi.");
        }

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(txtTitle, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Istanzia e formatta il sotto-pannello ospitante l'etichetta e l'area di testo per la descrizione.
     * A differenza del titolo, questo componente garantisce sempre i permessi di scrittura,
     * incapsulando l'area di input all'interno di uno ScrollPane stilizzato.
     *
     * @return Il contenitore {@code JPanel} aggregato per il corpo testuale.
     */
    private JPanel createDescSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);

        JLabel lblDesc = GuiUtils.createBoldLabel("Descrizione:");

        txtDesc = new JTextArea();
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);

        // Garantisce che il campo descrizione sia perennemente editabile
        txtDesc.setEditable(true);
        GuiUtils.styleTextArea(txtDesc);

        JScrollPane scroll = GuiUtils.createModernScrollPane(txtDesc);
        scroll.setBorder(GuiUtils.createStandardBorder());

        panel.add(lblDesc, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Costruisce il pannello dei controlli di terminazione allocato nel settore inferiore.
     * Aggancia i listener di evento deputati alla chiusura del dialogo (Annulla) e
     * all'innesco della procedura transazionale di aggiornamento dei dati (Salva Modifiche).
     *
     * @return Il contenitore {@code JPanel} ospitante i trigger di azione.
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panel.setOpaque(false);

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dispose());
        panel.add(btnCancel);

        // Il trigger di salvataggio rimane operativo anche in stato di blocco (per la sola descrizione)
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

        // Imposta il pulsante di salvataggio come azione di default alla pressione del tasto Invio
        getRootPane().setDefaultButton(btnSave);

        return panel;
    }

    /**
     * Esegue il popolamento (binding) dei controlli grafici di input estraendo
     * lo stato informativo in quel momento residente nell'entità di dominio collegata (Board).
     */
    private void loadData() {
        txtTitle.setText(board.getTitle());
        if (board.getDescription() != null) {
            txtDesc.setText(board.getDescription());
        }
    }

    /**
     * Governa il flusso logico conclusivo per l'aggiornamento del record sulla base dati.
     * Intercetta e valida la congruità dell'input (lunghezza, campi vuoti), accerta il rispetto
     * del vincolo di univocità nominale (evitando conflitti con altre bacheche del medesimo utente)
     * e demanda l'effettiva operazione di persistenza (UPDATE) al livello del controller.
     *
     * @throws SQLException           Qualora la transazione di aggiornamento fallisca a livello di driver o di integrità relazionale.
     * @throws BusinessLogicException Qualora le verifiche di dominio restituiscano una violazione (es. duplicazione del nome).
     */
    private void saveChanges() throws SQLException, BusinessLogicException {
        String newTitle = txtTitle.getText().trim();
        String newDesc = txtDesc.getText().trim();

        // Validazione preliminare dei limiti strutturali
        if (newTitle.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Il titolo è obbligatorio.", "Errore validazione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newTitle.length() > 100) {
            JOptionPane.showMessageDialog(this, "Il titolo è troppo lungo (massimo 100 caratteri).", "Errore validazione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Accerta l'assenza di collisioni nominali nello spazio di lavoro dell'utente
        boolean nameExists = controller.getUserBoards().stream()
                .anyMatch(b -> b.getId() != board.getId() && b.getTitle().equalsIgnoreCase(newTitle));

        if (nameExists) {
            JOptionPane.showMessageDialog(this, "Hai già una bacheca chiamata \"" + newTitle + "\".", "Nome duplicato", JOptionPane.WARNING_MESSAGE);
            return;
        }

        /* * L'operazione di salvataggio procede anche se il titolo non è cambiato
         * (es. a causa delle restrizioni di lock), per garantire l'aggiornamento della descrizione.
         */
        if (controller.updateBoardDetails(board.getId(), newTitle, newDesc)) {
            // Riflette le mutazioni sull'oggetto caricato in memoria per l'aggiornamento istantaneo della UI madre
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