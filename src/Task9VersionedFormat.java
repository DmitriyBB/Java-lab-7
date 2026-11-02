
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

public class Task9VersionedFormat {

    public static final int MAGIC = Task5BinaryCatalog.MAGIC;
    public static final int CURRENT_VERSION = 2;
    public static final int MAX_PAYLOAD = 64 * 1024 * 1024;

    public record Loaded(int sourceVersion, List<Course> courses) {
    }

    public static void writeV2(Path file, List<Course> courses) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream payload = new DataOutputStream(buffer)) {
            Task5BinaryCatalog.writeRecords(payload, courses);
        }
        byte[] bytes = buffer.toByteArray();
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(MAGIC);
            out.writeShort(CURRENT_VERSION);
            out.writeInt(courses.size());
            out.writeInt(bytes.length);
            out.writeLong(checksum(bytes));
            out.write(bytes);
        }
    }

    public static Loaded read(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new CatalogFormatException("Неизвестный заголовок: 0x" + Integer.toHexString(magic));
            }
            int version = in.readUnsignedShort();
            List<Course> courses = switch (version) {
                case 1 -> readV1Body(in);
                case 2 -> readV2Body(in);
                default -> throw new CatalogFormatException(
                        "Версия " + version + " не поддерживается (умеем 1.." + CURRENT_VERSION + ")");
            };
            if (in.read() != -1) {
                throw new CatalogFormatException("Лишние байты после данных");
            }
            return new Loaded(version, courses);
        } catch (EOFException e) {
            throw new CatalogFormatException("Файл обрезан", e);
        }
    }

    public static Loaded migrateToV2(Path file) throws IOException {
        Loaded loaded = read(file);
        if (loaded.sourceVersion() < CURRENT_VERSION) {
            Task6AtomicSave.replace(file, temp -> writeV2(temp, loaded.courses()), Files::move);
        }
        return loaded;
    }

    private static List<Course> readV1Body(DataInputStream in) throws IOException {
        return Task5BinaryCatalog.readRecords(in, in.readInt());
    }

    private static List<Course> readV2Body(DataInputStream in) throws IOException {
        int count = in.readInt();
        int length = in.readInt();
        long expected = in.readLong();
        if (length < 0 || length > MAX_PAYLOAD) {
            throw new CatalogFormatException("Недопустимая длина данных: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        if (checksum(bytes) != expected) {
            throw new CatalogFormatException("Checksum не совпадает, файл повреждён");
        }
        DataInputStream payload = new DataInputStream(new ByteArrayInputStream(bytes));
        List<Course> courses = Task5BinaryCatalog.readRecords(payload, count);
        if (payload.available() != 0) {
            throw new CatalogFormatException("Количество записей не совпадает с длиной данных");
        }
        return courses;
    }

    private static long checksum(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    static List<Course> published = List.of();

    static void loadAndPublish(Path file) {
        try {
            Loaded loaded = read(file);
            published = loaded.courses();
            System.out.println(file.getFileName() + ": версия " + loaded.sourceVersion()
                    + ", опубликовано " + published.size());
        } catch (IOException e) {
            System.out.println(file.getFileName() + ": отклонён — " + e.getMessage()
                    + "; опубликовано по-прежнему " + published.size());
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);

        Path file = dir.resolve("catalog-versioned.bin");
        Task5BinaryCatalog.write(file, Course.sample());
        System.out.println("Файл в формате v1, " + Files.size(file) + " байт");
        loadAndPublish(file);

        Loaded migrated = migrateToV2(file);
        System.out.println("Миграция " + migrated.sourceVersion() + " -> " + CURRENT_VERSION
                + ", теперь " + Files.size(file) + " байт");
        loadAndPublish(file);
        System.out.println("Старый читатель v1 на файле v2: ");
        try {
            Task5BinaryCatalog.read(file);
        } catch (CatalogFormatException e) {
            System.out.println("  " + e.getMessage());
        }

        byte[] bytes = Files.readAllBytes(file);

        byte[] corrupted = bytes.clone();
        corrupted[corrupted.length - 10] ^= 0x01;
        Path corruptedFile = dir.resolve("catalog-corrupted.bin");
        Files.write(corruptedFile, corrupted);
        loadAndPublish(corruptedFile);

        byte[] future = bytes.clone();
        future[5] = 3;
        Path futureFile = dir.resolve("catalog-v3.bin");
        Files.write(futureFile, future);
        loadAndPublish(futureFile);

        Path truncated = dir.resolve("catalog-v2-truncated.bin");
        Files.write(truncated, Arrays.copyOf(bytes, bytes.length - 3));
        loadAndPublish(truncated);
    }
}
