package dto;

import model.Priority;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.List;

/**
 * Oggetto di trasferimento dati (Data Transfer Object) implementato come Record.
 * Incapsula tutti i parametri necessari alla creazione di un nuovo ToDo.
 * Utilizzato per ridurre la complessità delle firme dei metodi nel Controller e nel DAO.
 *
 * @param boardId         L'identificativo della bacheca di appartenenza.
 * @param title           Il titolo testuale del task.
 * @param description     La descrizione dettagliata del task.
 * @param expiryDate      La data e ora di scadenza (può essere {@code null}).
 * @param priority        Il livello di priorità assegnato (tramite enum {@link Priority}).
 * @param link            Il collegamento URL opzionale.
 * @param imagePath       Il percorso dell'immagine allegata opzionale.
 * @param backgroundColor Il colore di sfondo personalizzato per la scheda (oggetto {@link Color}).
 * @param collaborators   La lista degli username ({@link String}) con cui condividere il task.
 * @param positionOrder   L'indice numerico di ordinamento all'interno della bacheca.
 */
public record TodoCreationDTO(
        int boardId,
        String title,
        String description,
        Timestamp expiryDate,
        Priority priority,
        String link,
        String imagePath,
        Color backgroundColor,
        List<String> collaborators,
        int positionOrder
) {}