package gui.events;

import controller.Controller;
import gui.components.TaskCard;
import model.ToDo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Gestisce le interazioni del mouse applicate alle card dei task ({@link TaskCard}), coordinando
 * la logica di trascinamento (Drag &amp; Drop) per il riordino visivo con la gestione dei click
 * per l'apertura della visualizzazione dettagliata.
 * <p>
 * Implementa una strategia di filtraggio basata su una soglia di movimento ({@code DRAG_THRESHOLD})
 * per differenziare intenzionalmente tra:
 * <ul>
 * <li><b>Click Sinistro:</b> Pressione e rilascio statico, adibito all'invocazione dei dettagli del task.</li>
 * <li><b>Trascinamento (Drag):</b> Movimento dinamico del cursore che innesca il riposizionamento
 * dinamico delle componenti nel contenitore.</li>
 * </ul>
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class TaskDragListener extends MouseAdapter {

    /** Il pannello contenitore Swing che ospita la collezione ordinata di {@link TaskCard}. */
    private final JPanel container;

    /** Il riferimento al Controller per la sincronizzazione dell'ordinamento nel database. */
    private final Controller controller;

    /** La funzione di callback eseguita al termine di un'operazione di rilascio (Drop). */
    private final Runnable onDrop;

    /** Il riferimento alla card attualmente oggetto dell'operazione di trascinamento. */
    private TaskCard draggingCard = null;

    /** Il componente sorgente originale dell'evento (utilizzato per la conversione delle coordinate). */
    private Component originalSource = null;

    /** Il punto di coordinate iniziale acquisito al momento della pressione del mouse. */
    private Point initialClickPoint = null;

    /** Flag di stato che indica se l'interazione corrente è stata classificata come trascinamento attivo. */
    private boolean isDraggingAction = false;

    /** Soglia minima di spostamento in pixel necessaria per convertire un click in un drag. */
    private static final int DRAG_THRESHOLD = 5;

    /** Flag di controllo per l'attivazione o la sospensione temporanea del listener. */
    private boolean enabled = true;

    /**
     * Inizializza il listener associandolo a un contenitore specifico e ai servizi di logica di business.
     *
     * @param container  Il componente grafico {@link JPanel} che funge da genitore per i task.
     * @param controller Il riferimento al gestore della persistenza e delle regole di dominio.
     * @param onDrop     L'azione da eseguire per rinfrescare l'interfaccia dopo un riordinamento.
     */
    public TaskDragListener(JPanel container, Controller controller, Runnable onDrop) {
        this.container = container;
        this.controller = controller;
        this.onDrop = onDrop;
    }

    /**
     * Modifica lo stato di operatività del listener.
     * Se impostato su {@code false}, il listener neutralizza ogni tentativo di trascinamento
     * pur mantenendo attive le altre risposte agli eventi del mouse.
     *
     * @param enabled {@code true} per consentire le operazioni di drag; {@code false} per bloccarle.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Intercetta la pressione del tasto del mouse per avviare il monitoraggio dell'interazione.
     * Applica un filtro restrittivo sul tasto sinistro ({@code BUTTON1}) per evitare interferenze
     * con i menu contestuali (tasto destro) e identifica la card coinvolta risalendo
     * la gerarchia dei componenti.
     *
     * @param e L'evento del mouse {@link MouseEvent} captato.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        // Garantisce che solo l'input primario (tasto sinistro) attivi la logica di drag/click
        if (!SwingUtilities.isLeftMouseButton(e)) return;

        originalSource = e.getComponent();
        Component c = originalSource;

        // Risale la gerarchia dei componenti per localizzare la TaskCard radice
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
     * Coordina lo spostamento dinamico dei componenti durante il trascinamento attivo.
     * Valuta il superamento della soglia {@code DRAG_THRESHOLD} per attivare il feedback visivo
     * (cambio cursore) e gestisce lo scambio di posizione delle card basandosi sulla
     * proiezione verticale del cursore. Implementa inoltre una logica di auto-scrolling
     * integrata con il {@link JViewport} genitore.
     *
     * @param e L'evento di trascinamento {@link MouseEvent} captato.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggingCard == null || initialClickPoint == null) return;
        if (!enabled) return;

        // Algoritmo di attivazione differita del Drag per prevenire click accidentali
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

        // Identifica il componente bersaglio situato alla coordinata Y attuale
        Component target = container.getComponentAt(container.getWidth() / 2, currentY);

        if (target instanceof TaskCard card && target != draggingCard) {
            int targetIndex = getCardIndex(card);
            int currentIndex = getCardIndex(draggingCard);

            if (targetIndex != -1 && currentIndex != -1) {
                // Esegue lo swap visivo dei componenti nel layout
                container.add(draggingCard, targetIndex);
                container.revalidate();
                container.repaint();
            }
        }

        // Gestione dell'auto-scrolling basata sui margini sensibili del JViewport
        if (container.getParent() instanceof JViewport viewport) {
            Rectangle viewRect = viewport.getViewRect();
            int scrollMargin = 25;
            int scrollStep = 12;

            if (p.y < viewRect.y + scrollMargin) {
                viewRect.y = Math.max(0, viewRect.y - scrollStep);
                container.scrollRectToVisible(viewRect);
            }
            else if (p.y > viewRect.y + viewRect.height - scrollMargin) {
                viewRect.y = Math.min(container.getHeight() - viewRect.height, viewRect.y + scrollStep);
                container.scrollRectToVisible(viewRect);
            }
        }
    }

    /**
     * Finalizza l'interazione del mouse discriminando l'esito finale dell'azione.
     * Se l'azione non ha superato la soglia di drag, interpreta l'evento come un click
     * e richiede l'apertura del dialogo di dettaglio. Se l'azione era un drag, ripristina
     * i cursori standard e richiede la persistenza del nuovo ordinamento nel database.
     *
     * @param e L'evento di rilascio {@link MouseEvent} captato.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (draggingCard != null) {
            if (!isDraggingAction) {
                // Caso: Click semplice (invocazione visualizzazione dettagliata)
                if (SwingUtilities.isLeftMouseButton(e)) {
                    draggingCard.openDetails();
                }
            } else {
                // Caso: Fine trascinamento (consolidamento dell'ordine)
                container.setCursor(Cursor.getDefaultCursor());
                draggingCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                updatePositionsInDB();

                if (onDrop != null) onDrop.run();
            }
        }

        // Reset rigoroso dello stato interno per la gestione del ciclo di vita dell'evento
        draggingCard = null;
        originalSource = null;
        isDraggingAction = false;
        initialClickPoint = null;
    }

    /**
     * Identifica la posizione ordinale di una card all'interno del contenitore.
     *
     * @param card L'istanza di {@link TaskCard} di cui determinare l'indice.
     * @return L'indice posizionale (0-based) nell'albero dei componenti,
     * o {@code -1} se la card non risulta presente nel contenitore.
     */
    private int getCardIndex(TaskCard card) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            if (container.getComponent(i) == card) return i;
        }
        return -1;
    }

    /**
     * Sincronizza l'ordinamento visivo delle componenti con la persistenza sul database.
     * Itera sulla collezione di componenti presenti nel contenitore e inoltra
     * al controller le richieste di aggiornamento solo per i task la cui posizione
     * relativa ha subito una variazione effettiva.
     */
    private void updatePositionsInDB() {
        int positionCounter = 1;
        boolean errorOccurred = false;

        for (Component c : container.getComponents()) {
            if (c instanceof TaskCard card) {
                ToDo todo = card.getTodo();

                // Ottimizzazione: aggiorna solo se la posizione logica è variata
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