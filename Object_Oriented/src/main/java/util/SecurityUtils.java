package util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Logger;

/**
 * Classe di utilità per la gestione della sicurezza e della crittografia delle password.
 * Utilizza l'algoritmo BCrypt (basato su Blowfish) che integra un meccanismo di
 * salting adattivo per proteggere le credenziali da attacchi a dizionario e rainbow table.
 */
public class SecurityUtils {

    private static final Logger LOGGER = Logger.getLogger(SecurityUtils.class.getName());

    /**
     * Impedisce l'istanziazione della classe tramite un costruttore privato,
     * trattandosi di una classe di utilità con soli metodi statici.
     */
    private SecurityUtils() {
        throw new UnsupportedOperationException("Classe di utilità: istanziazione non consentita.");
    }

    /**
     * Genera un hash sicuro a partire da una password in chiaro.
     * Crea automaticamente un salt casuale unico per ogni operazione, garantendo che
     * password identiche producano hash differenti nel database.
     *
     * @param plainTextPassword La stringa della password inserita dall'utente.
     * @return La stringa dell'hash risultante pronta per la persistenza, o {@code null} se l'input è nullo.
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null) return null;
        // gensalt() genera un salt casuale con fattore di costo predefinito
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /**
     * Verifica la corrispondenza tra una password in chiaro e un hash memorizzato.
     * Estrae il salt dall'hash fornito e lo utilizza per verificare se la password
     * inserita genera il medesimo output crittografico.
     *
     * @param plainTextPassword La password testuale da sottoporre a verifica.
     * @param storedHash        L'hash precedentemente salvato nel database.
     * @return {@code true} se la password coincide con l'hash, {@code false} altrimenti.
     */
    public static boolean checkPassword(String plainTextPassword, String storedHash) {
        if (plainTextPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainTextPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Gestisce i casi in cui il formato dell'hash nel database non sia conforme allo standard BCrypt
            LOGGER.warning(() -> "SecurityUtils: Formato dell'hash non valido individuato nel database.");
            return false;
        }
    }
}