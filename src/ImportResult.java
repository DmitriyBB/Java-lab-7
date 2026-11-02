
import java.util.List;

public sealed interface ImportResult {

    record Success(List<Course> courses) implements ImportResult {
    }

    record Failure(List<ImportError> errors) implements ImportResult {
    }
}
