
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public class Task1StringFiles {

    public static void save(Path file, List<Course> courses) throws IOException {
        if (Files.isDirectory(file)) {
            throw new IOException("Ожидался файл, а это каталог: " + file);
        }
        StringBuilder text = new StringBuilder();
        for (Course course : courses) {
            text.append(course.toLine()).append('\n');
        }
        Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
    }

    public static List<Course> load(Path file) throws IOException {
        if (Files.isDirectory(file)) {
            throw new IOException("Ожидался файл, а это каталог: " + file);
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return text.lines()
                .filter(line -> !line.isBlank())
                .map(Course::parse)
                .toList();
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path file = dir.resolve("catalog.txt");

        save(file, Course.sample());
        List<Course> loaded = load(file);
        System.out.println("Сохранено и прочитано: " + loaded);
        System.out.println("Совпадает с исходным: " + loaded.equals(Course.sample()));
        System.out.println("Байт в файле: " + Files.size(file) + ", символов: " + Files.readString(file).length());

        tryLoad(dir.resolve("missing.txt"));
        tryLoad(dir);
    }

    private static void tryLoad(Path file) {
        System.out.print("load(" + file + "): ");
        try {
            List<Course> courses = load(file);
            System.out.println("загружено " + courses.size());
        } catch (NoSuchFileException e) {
            System.out.println("файла нет (" + e.getClass().getSimpleName() + "), каталог НЕ загружен");
        } catch (IOException e) {
            System.out.println(e.getMessage() + ", каталог НЕ загружен");
        }
    }
}
