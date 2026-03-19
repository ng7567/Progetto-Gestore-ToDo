package model;

import java.util.Objects;

/**
 * Rappresenta un utente registrato nell'applicazione.
 * Contiene le credenziali di accesso e il codice identificativo usato per
 * collegare l'utente alle sue bacheche e ai suoi task.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class User {

    /** Il codice numerico univoco assegnato all'utente dal database. */
    private int id;

    /** Il nome scelto dall'utente per l'accesso. */
    private final String username;

    /** La password dell'utente, salvata e gestita in formato hash per sicurezza. */
    private final String password;

    /**
     * Crea un utente con tutti i dati letti dal database.
     *
     * @param id       Il codice univoco assegnato dal database.
     * @param username Il nome utente.
     * @param password La password dell'utente (in formato hash).
     */
    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    /**
     * Crea un nuovo utente durante la fase di registrazione.
     * L'ID non serve perché viene generato in automatico dal database al momento del salvataggio.
     *
     * @param username Il nome scelto per il nuovo account.
     * @param password La password scelta per l'accesso (in formato hash).
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Legge il codice univoco dell'utente.
     *
     * @return L'ID numerico dell'utente.
     */
    public int getId() {
        return id;
    }

    /**
     * Assegna il codice identificativo all'utente.
     *
     * @param id L'ID numerico da impostare.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Legge il nome utente.
     *
     * @return Il nome usato per l'accesso.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Legge la password dell'utente.
     *
     * @return La password salvata (in formato hash).
     */
    public String getPassword() {
        return password;
    }

    /**
     * Restituisce una rappresentazione in formato testo dell'utente.
     * La password viene nascosta di proposito per evitare di esporre
     * dati sensibili se questo metodo viene usato nei log di sistema.
     *
     * @return Una stringa con l'ID e l'username dell'utente.
     */
    @Override
    public String toString() {
        return "User{" + "id=" + id + ", username='" + username + "'}";
    }

    /**
     * Confronta l'utente con un altro oggetto per capire se sono uguali.
     * Due utenti sono uguali se hanno lo stesso ID e lo stesso username.
     *
     * @param o L'oggetto da confrontare.
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
     * Genera un codice numerico (hash) per l'utente,
     * basandosi sul suo ID e sul suo username.
     *
     * @return Il valore hash calcolato.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}