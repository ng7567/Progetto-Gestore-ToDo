package util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Logger;

/**
 * Classe di utilità per la sicurezza e la crittografia delle password.
 * Usa l'algoritmo BCrypt per generare un hash sicuro, proteggendo le password
 * degli utenti da attacchi informatici
 * grazie all'aggiunta automatica di un "salt" casuale.
 * Ha un costruttore privato per vietarne la creazione di oggetti.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class SecurityUtils {

    /** Logger per tracciare eventuali errori o formati non validi durante la verifica delle password. */
    private static final Logger LOGGER = Logger.getLogger(SecurityUtils.class.getName());

    /**
     * Costruttore privato.
     * Lancia un'eccezione se viene richiamato per sbaglio, poiché questa classe
     * deve contenere solo metodi statici.
     *
     * @throws UnsupportedOperationException Sempre lanciata al richiamo.
     */
    private SecurityUtils() {
        throw new UnsupportedOperationException("Classe di utilità: istanziazione non consentita.");
    }

    /**
     * Genera un hash sicuro partendo da una password in chiaro.
     * Crea in automatico un "salt" casuale per ogni salvataggio, in modo che
     * due password uguali generino comunque due hash completamente diversi nel database.
     *
     * @param plainTextPassword La password in chiaro scritta dall'utente.
     * @return La stringa con l'hash calcolato, oppure {@code null} se la password fornita è nulla.
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null) return null;
        // gensalt() genera un salt casuale con le impostazioni di sicurezza predefinite
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /**
     * Controlla se una password in chiaro corrisponde a un hash salvato in precedenza.
     * BCrypt legge in automatico il "salt" contenuto nell'hash per fare il confronto
     * in modo sicuro.
     *
     * @param plainTextPassword La password in chiaro da controllare.
     * @param storedHash        L'hash della password letto dal database.
     * @return {@code true} se la password è corretta, {@code false} altrimenti.
     */
    public static boolean checkPassword(String plainTextPassword, String storedHash) {
        if (plainTextPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainTextPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Gestisce il caso in cui l'hash letto dal database non sia nel formato BCrypt corretto
            LOGGER.warning(() -> "SecurityUtils: Formato dell'hash non valido individuato nel database.");
            return false;
        }
    }
}