package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe di utilità per la gestione dei file all'interno del progetto.
 * Centralizza il salvataggio delle immagini allegate ai task e la loro
 * successiva eliminazione dal disco.
 * Ha un costruttore privato per vietarne la creazione di oggetti.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class FileUtils {

    /** Logger per tracciare eventuali errori durante le operazioni sui file. */
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    /** Nome della cartella principale dove vengono salvate le immagini dell'applicazione. */
    private static final String STORAGE_DIR = "saved_images";

    /**
     * Costruttore privato.
     * Lancia un'eccezione se viene richiamato per sbaglio (es. tramite reflection).
     *
     * @throws UnsupportedOperationException Sempre lanciata al richiamo.
     */
    private FileUtils() {
        throw new UnsupportedOperationException("Classe di utilità: istanziazione non consentita.");
    }

    /**
     * Copia un file immagine dal computer dell'utente alla cartella di salvataggio dell'app.
     * Genera in automatico un nome univoco (UUID) per il file copiato,
     * in modo da evitare errori se due file originali hanno lo stesso nome.
     *
     * @param originalPath Il percorso assoluto del file originale da copiare.
     * @return Il percorso del nuovo file salvato (es: "saved_images/uuid.jpg")
     * oppure {@code null} se l'operazione fallisce.
     */
    public static String saveImage(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) {
            return null;
        }

        try {
            // Controlla se la cartella di destinazione esiste, altrimenti la crea
            File directory = new File(STORAGE_DIR);
            if (!directory.exists() && !directory.mkdirs()) {
                LOGGER.warning(() -> "Impossibile creare la cartella di salvataggio: " + STORAGE_DIR);
                return null;
            }

            // Recupera l'estensione del file originale per mantenere il formato corretto (es. .jpg, .png)
            String extension = "";
            int i = originalPath.lastIndexOf('.');
            if (i > 0) {
                extension = originalPath.substring(i);
            }

            // Crea un nome file casuale e univoco
            String uniqueFileName = UUID.randomUUID() + extension;

            // Prepara i percorsi usando le API moderne di Java (NIO.2)
            Path source = Paths.get(originalPath);
            Path destination = Paths.get(STORAGE_DIR, uniqueFileName);

            // Copia fisicamente il file, sovrascrivendo se necessario
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            return destination.toString();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore critico durante il salvataggio dell'immagine: " + originalPath);
            return null;
        }
    }

    /**
     * Elimina un file dal disco partendo dal suo percorso.
     * Viene usato di solito quando si elimina un task o si cambia l'immagine allegata.
     *
     * @param pathStr Il percorso (relativo o assoluto) del file da cancellare.
     * @return {@code true} se il file viene eliminato correttamente, {@code false} altrimenti.
     */
    public static boolean deleteFile(String pathStr) {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            return false;
        }

        // Converte la stringa in un oggetto Path e calcola il percorso completo se è relativo
        Path path = Paths.get(pathStr);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }

        Path finalPath = path;

        try {
            if (!Files.exists(path)) {
                LOGGER.warning(() -> "Tentativo di eliminazione fallito. File non trovato: " + finalPath.toAbsolutePath());
                return false;
            }

            Files.delete(path);
            LOGGER.info(() -> "File eliminato con successo: " + finalPath.getFileName());
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore durante l'eliminazione del file: " + finalPath.toAbsolutePath());
            return false;
        }
    }
}