package model;

import java.util.Objects;

/**
 * Rappresenta un utente registrato nel sistema.
 * Gestisce le credenziali di accesso e l'identificativo univoco necessario per
 * stabilire le relazioni di possesso e condivisione con bacheche e task.
 */
public class User {
    private int id;
    private final String username;
    private final String password;

    /**
     * Costruisce un oggetto utente completo recuperato dalla base di dati.
     *
     * @param id       L'identificativo univoco assegnato dal database.
     * @param username Il nome utente registrato.
     * @param password La stringa rappresentante l'hash della password.
     */
    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    /**
     * Costruisce un oggetto utente parziale utilizzato durante la fase di registrazione.
     * L'identificativo viene omesso poiché la sua generazione è delegata al database.
     *
     * @param username Il nome utente scelto per il nuovo account.
     * @param password La password scelta dall'utente (gestita come hash dal sistema).
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Restituisce l'identificativo univoco dell'utente.
     *
     * @return L'ID numerico dell'utente.
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'identificativo univoco dell'utente.
     *
     * @param id L'ID assegnato dal sistema di persistenza.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce il nome utente.
     *
     * @return La stringa contenente lo username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Restituisce la password (hash) dell'utente.
     *
     * @return La stringa contenente l'hash delle credenziali.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Fornisce una rappresentazione testuale dell'utente.
     * Esclude intenzionalmente la password per garantire la sicurezza dei dati
     * durante eventuali operazioni di log o debug.
     *
     * @return Una stringa riassuntiva contenente ID e username.
     */
    @Override
    public String toString() {
        return "User{" + "id=" + id + ", username='" + username + "'}";
    }

    /**
     * Confronta l'istanza corrente con un altro oggetto per verificarne l'uguaglianza.
     * La verifica è basata sulla combinazione di ID univoco e username.
     *
     * @param o L'oggetto da confrontare con l'utente corrente.
     * @return {@code true} se rappresentano lo stesso utente, {@code false} altrimenti.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(username, user.username);
    }

    /**
     * Calcola il valore hash dell'utente.
     * Utilizza l'ID e lo username come basi per la generazione del codice hash.
     *
     * @return Il valore hash calcolato.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}