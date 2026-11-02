
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Task4LazyLines {

    public static long countByPrefix(Path file, String prefix) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.filter(line -> line.startsWith(prefix)).count();
        }
    }

    public static Stream<String> brokenLines(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines;
        }
    }

    public static Stream<String> openLines(Path file) throws IOException {
        return Files.lines(file, StandardCharsets.UTF_8)
                .onClose(() -> System.out.println("    onClose: файл закрыт"));
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path file = dir.resolve("catalog.txt");
        Task1StringFiles.save(file, Course.sample());

        System.out.println("1) Правильно, stream внутри try-with-resources:");
        System.out.println("    курсов на 'J': " + countByPrefix(file, "J"));

        System.out.println("2) Ошибка: stream возвращён из try-with-resources:");
        try {
            System.out.println("    " + brokenLines(file).count());
        } catch (IllegalStateException e) {
            System.out.println("    " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println("3) Ошибка: stream отдали наружу, вызывающий его не закрыл:");
        System.out.println("    строк: " + openLines(file).count() + " (onClose не вызван, файл остался открыт)");

        System.out.println("4) Правильно: stream отдали наружу, вызывающий владеет им и закрывает:");
        try (Stream<String> lines = openLines(file)) {
            System.out.println("    строк: " + lines.count());
        }
    }
}
