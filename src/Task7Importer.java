
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Task7Importer {

    public static ImportResult importCatalog(Path file) throws IOException {
        List<Course> courses = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            long number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Course course = Course.parse(line);
                    if (codes.add(course.code())) {
                        courses.add(course);
                    } else {
                        errors.add(new ImportError(number, "повтор кода " + course.code()));
                    }
                } catch (IllegalArgumentException e) {
                    errors.add(new ImportError(number, e.getMessage()));
                }
            }
        }
        if (!errors.isEmpty()) {
            return new ImportResult.Failure(List.copyOf(errors));
        }
        return new ImportResult.Success(List.copyOf(courses));
    }

    static List<Course> published = Course.sample();

    static void importAndPublish(Path file) throws IOException {
        switch (importCatalog(file)) {
            case ImportResult.Success success -> {
                published = success.courses();
                System.out.println("  опубликовано курсов: " + published.size());
            }
            case ImportResult.Failure failure -> {
                System.out.println("  импорт отклонён целиком, ошибок: " + failure.errors().size());
                failure.errors().forEach(error -> System.out.println("    " + error));
            }
        }
        System.out.println("  сейчас в каталоге: " + published.stream().map(Course::code).toList());
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);

        Path broken = dir.resolve("import-broken.txt");
        Files.writeString(broken, """
                NET-110;Сети;36
                OS-120;Операционные системы;сорок
                WEB-130;Веб-разработка;48
                NET-110;Сети ещё раз;36
                ;Без кода;10
                PY-140;Python;24
                """, StandardCharsets.UTF_8);
        System.out.println("Файл с ошибками:");
        importAndPublish(broken);

        Path good = dir.resolve("import-good.txt");
        Files.writeString(good, """
                NET-110;Сети;36
                WEB-130;Веб-разработка;48
                """, StandardCharsets.UTF_8);
        System.out.println("Корректный файл:");
        importAndPublish(good);
    }
}
