package gui.events;

import controller.Controller;
import gui.components.TaskCard;
import model.ToDo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Gestisce le interazioni del mouse sulle card dei task, combinando la funzionalità
 * di trascinamento (Drag &amp; Drop) per il riordino con quella di click per l'apertura dei dettagli.
 * <p>
 * Questa classe implementa una logica per distinguere intenzionalmente tra:
 * <ul>
 * <li><b>Click:</b> Pressione e rilascio senza movimento significativo (apre il task).</li>
 * <li><b>Drag:</b> Pressione, movimento oltre una soglia minima e rilascio (sposta il task).</li>
 * </ul>
 */
public class TaskDragListener extends MouseAdapter {

    private final JPanel container;
    private final Controller controller;
    private final Runnable onDrop;

    private TaskCard draggingCard = null;
    private Component originalSource = null;

    // Variabili per la logica Click vs Drag
    private Point initialClickPoint = null;
    private boolean isDraggingAction = false;
    private static final int DRAG_THRESHOLD = 5;

    private boolean enabled = true;

    /**
     * Costruisce un nuovo listener per la gestione dei task.
     *
     * @param container  Il pannello Swing che contiene le {@link TaskCard}.
     * @param controller Il {@link Controller} per eseguire gli aggiornamenti sul database.
     * @param onDrop     Azione da eseguire dopo che un task è stato spostato e rilasciato.
     */
    public TaskDragListener(JPanel container, Controller controller, Runnable onDrop) {
        this.container = container;
        this.controller = controller;
        this.onDrop = onDrop;
    }

    /**
     * Imposta lo stato di attivazione del listener.
     * Se disabilitato, ignora ogni tentativo di trascinamento.
     *
     * @param enabled true per abilitare il drag, false per bloccarlo.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gestisce l'evento di pressione del tasto del mouse.
     * Interrompe immediatamente l'esecuzione se l'input non proviene dal tasto sinistro,
     * garantendo che il tasto destro non attivi alcuna logica di selezione o trascinamento.
     * Identifica la {@link TaskCard} coinvolta risalendo la gerarchia dei componenti
     * e memorizza le coordinate iniziali dell'interazione.
     *
     * @param e L'evento del mouse generato.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        // FILTRO TASTO DESTRO: Se non è il tasto sinistro, interrompe subito
        if (!SwingUtilities.isLeftMouseButton(e)) return;

        originalSource = e.getComponent();
        Component c = originalSource;

        while (c != null && !(c instanceof TaskCard)) {
            c = c.getParent();
        }

        if (c instanceof TaskCard card) {
            draggingCard = card;
            initialClickPoint = e.getPoint();
            isDraggingAction = false;
        }
    }

    /**
     * Gestisce il movimento del mouse mentre il tasto è premuto.
     * <p>
     * Verifica se la distanza percorsa supera la soglia minima impostata.
     * Se superata, attiva la modalità trascinamento (cambiando il cursore) e gestisce
     * lo scambio visivo delle card all'interno del contenitore.
     * Gestisce inoltre lo scorrimento automatico (auto-scroll) se il mouse raggiunge i bordi.
     *
     * @param e L'evento del mouse generato.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggingCard == null || initialClickPoint == null) return;
        if (!enabled) return; // Blocca il movimento

        // Verifica della soglia minima di movimento per attivare il Drag
        if (!isDraggingAction) {
            double distance = initialClickPoint.distance(e.getPoint());
            if (distance < DRAG_THRESHOLD) {
                return;
            }
            isDraggingAction = true;

            container.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            draggingCard.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        Point p = SwingUtilities.convertPoint(originalSource, e.getPoint(), container);
        int currentY = p.y;

        Component target = container.getComponentAt(container.getWidth() / 2, currentY);

        if (target instanceof TaskCard card && target != draggingCard) {
            int targetIndex = getCardIndex(card);
            int currentIndex = getCardIndex(draggingCard);

            if (targetIndex != -1 && currentIndex != -1) {
                container.add(draggingCard, targetIndex);
                container.revalidate();
                container.repaint();
            }
        }

        // Gestisce lo scorrimento automatico solo ai limiti dell'area visibile.
        // Lascia libera la rotella del mouse di funzionare fluidamente durante il drag.
        if (container.getParent() instanceof JViewport viewport) {
            Rectangle viewRect = viewport.getViewRect();
            int scrollMargin = 25; // Zona di tolleranza in pixel (bordo superiore e inferiore)

            int scrollStep = 12;

            // Sposta la vista verso l'alto se il cursore tocca il margine superiore
            if (p.y < viewRect.y + scrollMargin) {
                viewRect.y = Math.max(0, viewRect.y - scrollStep);
                container.scrollRectToVisible(viewRect);
            }
            // Sposta la vista verso il basso se il cursore tocca il margine inferiore
            else if (p.y > viewRect.y + viewRect.height - scrollMargin) {
                viewRect.y = Math.min(container.getHeight() - viewRect.height, viewRect.y + scrollStep);
                container.scrollRectToVisible(viewRect);
            }
        }
    }

    /**
     * Gestisce l'evento di rilascio del tasto del mouse.
     * Valuta se l'interazione conclusa rappresenta un semplice click o il termine
     * di un trascinamento. In caso di click sinistro autentico, richiede l'apertura
     * del dialogo dei dettagli; in caso di trascinamento, finalizza il riposizionamento
     * visivo e aggiorna l'ordinamento persistente nel database.
     * Infine, esegue il reset completo dello stato interno del listener.
     *
     * @param e L'evento del mouse generato.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        // Se draggingCard è null, significa che mousePressed ha ignorato l'evento (es. tasto destro)
        if (draggingCard != null) {
            if (!isDraggingAction) {
                // Esegue il controllo finale sul tasto per sicurezza prima di aprire i dettagli
                if (SwingUtilities.isLeftMouseButton(e)) {
                    draggingCard.openDetails();
                }
            } else {
                // Finalizza l'operazione di trascinamento
                container.setCursor(Cursor.getDefaultCursor());
                draggingCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                updatePositionsInDB();

                if (onDrop != null) onDrop.run();
            }
        }

        // Esegue il reset dello stato per prepararsi alla prossima interazione
        draggingCard = null;
        originalSource = null;
        isDraggingAction = false;
        initialClickPoint = null;
    }

    /**
     * Recupera l'indice attuale di una card all'interno del contenitore.
     *
     * @param card La card di cui cercare l'indice.
     * @return L'indice (0-based) o {@code -1} se la card non è trovata.
     */
    private int getCardIndex(TaskCard card) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            if (container.getComponent(i) == card) return i;
        }
        return -1;
    }

    /**
     * Aggiorna le posizioni nel database rispecchiando l'ordine visivo corrente.
     * <p>
     * Itera sui componenti e aggiorna il campo posizione nel database solo se
     * differisce dal valore attuale, ottimizzando le performance delle query.
     */
    private void updatePositionsInDB() {
        int positionCounter = 1;
        boolean errorOccurred = false;

        for (Component c : container.getComponents()) {
            if (c instanceof TaskCard card) {
                ToDo todo = card.getTodo();

                if (todo.getPositionOrder() != positionCounter) {
                    boolean success = controller.updateTodoPosition(todo.getId(), positionCounter, todo.getRole());
                    if (success) {
                        todo.setPositionOrder(positionCounter);
                    } else {
                        errorOccurred = true;
                    }
                }
                positionCounter++;
            }
        }

        if (errorOccurred) {
            JOptionPane.showMessageDialog(container,
                    "Errore durante il salvataggio dell'ordine.",
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
}