
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Task5BinaryCatalog {

    public static final int MAGIC = 0x43525331;
    public static final int VERSION = 1;

    public static void write(Path file, List<Course> courses) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(MAGIC);
            out.writeShort(VERSION);
            out.writeInt(courses.size());
            writeRecords(out, courses);
        }
    }

    public static List<Course> read(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new CatalogFormatException("Неизвестный заголовок: 0x" + Integer.toHexString(magic));
            }
            int version = in.readUnsignedShort();
            if (version != VERSION) {
                throw new CatalogFormatException("Неподдерживаемая версия: " + version);
            }
            List<Course> courses = readRecords(in, in.readInt());
            if (in.read() != -1) {
                throw new CatalogFormatException("Лишние байты после записей");
            }
            return courses;
        } catch (EOFException e) {
            throw new CatalogFormatException("Файл обрезан", e);
        }
    }

    static void writeRecords(DataOutputStream out, List<Course> courses) throws IOException {
        for (Course course : courses) {
            out.writeUTF(course.code());
            out.writeUTF(course.title());
            out.writeInt(course.hours());
        }
    }

    static List<Course> readRecords(DataInputStream in, int count) throws IOException {
        if (count < 0) {
            throw new CatalogFormatException("Отрицательное количество записей: " + count);
        }
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            try {
                courses.add(new Course(in.readUTF(), in.readUTF(), in.readInt()));
            } catch (IllegalArgumentException e) {
                throw new CatalogFormatException("Запись " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(courses);
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path good = dir.resolve("catalog.bin");
        write(good, Course.sample());
        System.out.println("Записано байт: " + Files.size(good));
        System.out.println("Прочитано: " + read(good));

        byte[] bytes = Files.readAllBytes(good);

        byte[] future = bytes.clone();
        future[5] = 9;
        Path futureFile = dir.resolve("catalog-v9.bin");
        Files.write(futureFile, future);
        tryRead(futureFile);

        Path truncated = dir.resolve("catalog-truncated.bin");
        Files.write(truncated, Arrays.copyOf(bytes, bytes.length - 5));
        tryRead(truncated);

        Path notCatalog = dir.resolve("catalog.txt");
        Task1StringFiles.save(notCatalog, Course.sample());
        tryRead(notCatalog);
    }

    private static void tryRead(Path file) {
        try {
            System.out.println(file.getFileName() + ": прочитано " + read(file).size());
        } catch (IOException e) {
            System.out.println(file.getFileName() + ": отклонён — " + e.getMessage());
        }
    }
}
