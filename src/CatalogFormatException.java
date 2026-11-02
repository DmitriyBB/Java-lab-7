
import java.io.IOException;

public class CatalogFormatException extends IOException {

    public CatalogFormatException(String message) {
        super(message);
    }

    public CatalogFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
