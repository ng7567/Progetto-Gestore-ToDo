package exception;

/**
 * Lanciata quando si tenta di registrare un username già presente nel database.
 */
public class UserAlreadyExistsException extends BusinessLogicException {
    public UserAlreadyExistsException(String username) {
        super("L'username '" + username + "' è già occupato. Scegli un nome diverso.");
    }
}