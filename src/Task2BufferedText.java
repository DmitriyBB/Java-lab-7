
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Task2BufferedText {

    public record TextStats(long lines, long chars) {
    }

    public static TextStats upperCaseTitles(Path in, Path out) throws IOException {
        long lines = 0;
        long chars = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(in), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(Files.newOutputStream(out), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                chars += line.length();
                Course course = Course.parse(line);
                writer.write(new Course(course.code(), course.title().toUpperCase(), course.hours()).toLine());
                writer.newLine();
            }
        }
        return new TextStats(lines, chars);
    }

    public static TextStats count(Path file, Charset charset) throws IOException {
        long lines = 0;
        long chars = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                chars += line.length();
            }
        }
        return new TextStats(lines, chars);
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path in = dir.resolve("catalog.txt");
        Path out = dir.resolve("catalog-upper.txt");
        Task1StringFiles.save(in, Course.sample());

        TextStats stats = upperCaseTitles(in, out);
        System.out.println("Обработано строк: " + stats.lines() + ", символов (без переводов строк): " + stats.chars());
        System.out.println("Результат:");
        Files.readAllLines(out).forEach(line -> System.out.println("  " + line));

        System.out.println("Граница bytes -> chars:");
        System.out.println("  байт на диске: " + Files.size(in));
        System.out.println("  символов при UTF-8:      " + count(in, StandardCharsets.UTF_8).chars());
        System.out.println("  символов при ISO-8859-1: " + count(in, StandardCharsets.ISO_8859_1).chars()
                + " (неверная кодировка: каждый байт стал отдельным символом)");
        String word = "Ёж";
        System.out.println("  \"" + word + "\": " + word.length() + " char, "
                + word.getBytes(StandardCharsets.UTF_8).length + " байта в UTF-8");
    }
}
