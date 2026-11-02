
public record ImportError(long line, String message) {

    @Override
    public String toString() {
        return "строка " + line + ": " + message;
    }
}
