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
 * Classe di utilità per la gestione dei file fisici all'interno del progetto.
 * Centralizza le operazioni di salvataggio delle immagini caricate e la relativa
 * rimozione dal disco rigido.
 * Implementata con costruttore privato per impedirne l'istanziazione.
 */
public class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    // Cartella base dove salvare le immagini
    private static final String STORAGE_DIR = "saved_images";

    /**
     * Impedisce l'istanziazione esterna della classe tramite un costruttore privato.
     * Lancia un'eccezione in caso di accesso tramite reflection.
     */
    private FileUtils() {
        throw new UnsupportedOperationException("Classe di utilità: istanziazione non consentita.");
    }

    /**
     * Copia un file immagine dalla sorgente alla cartella di storage dell'applicazione.
     * Genera un identificativo univoco (UUID) per il nome del file al fine di evitare
     * collisioni tra file aventi il medesimo nome originale.
     *
     * @param originalPath Il percorso assoluto del file sorgente individuato nel filesystem dell'utente.
     * @return Il percorso relativo del file salvato (es: "saved_images/uuid.jpg") o {@code null} in caso di errore.
     */
    public static String saveImage(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) {
            return null;
        }

        try {
            // Verifica l'esistenza della directory di destinazione o tenta di crearla
            File directory = new File(STORAGE_DIR);
            if (!directory.exists() && !directory.mkdirs()) {
                LOGGER.warning(() -> "Impossibile creare la cartella di storage: " + STORAGE_DIR);
                return null;
            }

            // Estrae l'estensione del file originale per preservare il formato
            String extension = "";
            int i = originalPath.lastIndexOf('.');
            if (i > 0) {
                extension = originalPath.substring(i);
            }

            // Genera un nome file univoco tramite UUID
            String uniqueFileName = UUID.randomUUID() + extension;

            // Definisce i riferimenti tramite le API NIO.2
            Path source = Paths.get(originalPath);
            Path destination = Paths.get(STORAGE_DIR, uniqueFileName);

            // Esegue la copia fisica del file sovrascrivendo eventuali file esistenti
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            return destination.toString();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Errore critico durante il salvataggio dell'immagine: " + originalPath);
            return null;
        }
    }

    /**
     * Rimuove un file dal supporto di memorizzazione dato il suo percorso.
     * Viene invocato tipicamente durante l'eliminazione di un task o la sostituzione di un'immagine.
     * Utilizza le moderne API NIO in sostituzione del metodo legacy File.delete.
     *
     * @param pathStr Il percorso (relativo o assoluto) del file da eliminare.
     * @return {@code true} se l'eliminazione avviene con successo, {@code false} altrimenti.
     */
    public static boolean deleteFile(String pathStr) {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            return false;
        }

        // Converte in Path e risolve il percorso se relativo al progetto
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