
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Task8StreamingImport {

    public record Report(long lines, long imported, long failed, long totalHours,
                         List<ImportError> firstErrors, boolean cancelled) {
    }

    public static Report importLarge(Path file, int maxStoredErrors, long failLimit) throws IOException {
        long lines = 0;
        long imported = 0;
        long failed = 0;
        long totalHours = 0;
        List<ImportError> errors = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                try {
                    Course course = Course.parse(line);
                    imported++;
                    totalHours += course.hours();
                } catch (IllegalArgumentException e) {
                    failed++;
                    if (errors.size() < maxStoredErrors) {
                        errors.add(new ImportError(lines, e.getMessage()));
                    }
                    if (failed >= failLimit) {
                        return new Report(lines, imported, failed, totalHours, List.copyOf(errors), true);
                    }
                }
            }
        }
        return new Report(lines, imported, failed, totalHours, List.copyOf(errors), false);
    }

    public static void generate(Path file, long lines, long badEvery) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (long i = 1; i <= lines; i++) {
                if (i % badEvery == 0) {
                    writer.write("BAD-" + i + ";Испорченная строка без часов");
                } else {
                    writer.write("C-" + i + ";Курс номер " + i + " с длинным названием для объёма;" + (i % 100 + 1));
                }
                writer.newLine();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        long lines = args.length > 0 ? Long.parseLong(args[0]) : 3_000_000;
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path file = dir.resolve("big-import.txt");
        generate(file, lines, 100_000);

        long mb = 1024 * 1024;
        System.out.println("Размер файла: " + Files.size(file) / mb + " МБ, доступная куча: "
                + Runtime.getRuntime().maxMemory() / mb + " МБ");

        print("Полный проход (порог 1000 ошибок, хранить 5):", importLarge(file, 5, 1000));
        print("Отмена после 3 ошибок:", importLarge(file, 5, 3));

        Files.delete(file);
    }

    private static void print(String title, Report report) {
        System.out.println(title);
        System.out.println("  строк: " + report.lines() + ", импортировано: " + report.imported()
                + ", ошибок: " + report.failed() + ", часов всего: " + report.totalHours()
                + ", отменён: " + report.cancelled());
        report.firstErrors().forEach(error -> System.out.println("    " + error));
    }
}
