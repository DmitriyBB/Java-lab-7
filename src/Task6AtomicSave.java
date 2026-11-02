
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class Task6AtomicSave {

    public interface Mover {
        Path move(Path source, Path target, CopyOption... options) throws IOException;
    }

    public interface TempWriter {
        void write(Path temp) throws IOException;
    }

    public enum SaveMode { ATOMIC, NON_ATOMIC }

    public static SaveMode save(Path target, List<Course> courses) throws IOException {
        return save(target, courses, Files::move);
    }

    public static SaveMode save(Path target, List<Course> courses, Mover mover) throws IOException {
        return replace(target, temp -> {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (Course course : courses) {
                    writer.write(course.toLine());
                    writer.newLine();
                }
                writer.flush();
            }
        }, mover);
    }

    public static SaveMode replace(Path target, TempWriter tempWriter, Mover mover) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Path temp = Files.createTempFile(dir, target.getFileName() + ".", ".tmp");
        try {
            tempWriter.write(temp);
            try {
                mover.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return SaveMode.ATOMIC;
            } catch (AtomicMoveNotSupportedException e) {
                mover.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                return SaveMode.NON_ATOMIC;
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path target = dir.resolve("catalog-atomic.txt");

        System.out.println("1) Обычное сохранение: " + save(target, Course.sample()));
        System.out.println("   содержимое: " + Files.readAllLines(target));

        Mover noAtomic = (source, destination, options) -> {
            if (Arrays.asList(options).contains(StandardCopyOption.ATOMIC_MOVE)) {
                throw new AtomicMoveNotSupportedException(source.toString(), destination.toString(), "ФС не умеет");
            }
            return Files.move(source, destination, options);
        };
        List<Course> updated = List.of(new Course("NET-110", "Сети", 36));
        System.out.println("2) ФС без atomic move: " + save(target, updated, noAtomic));
        System.out.println("   содержимое: " + Files.readAllLines(target));

        List<Course> broken = Arrays.asList(new Course("OS-120", "Операционные системы", 40), null);
        try {
            save(target, broken);
        } catch (NullPointerException e) {
            System.out.println("3) Сбой при записи временного файла: " + e.getClass().getSimpleName());
        }
        System.out.println("   целевой файл не тронут: " + Files.readAllLines(target));
        try (var files = Files.list(dir)) {
            System.out.println("   осталось .tmp файлов: " + files.filter(p -> p.toString().endsWith(".tmp")).count());
        }
    }
}
