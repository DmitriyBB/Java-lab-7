
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Task3TryWithResources {

    static final List<String> LOG = new ArrayList<>();

    static class TracedReader extends BufferedReader {
        boolean closed;

        TracedReader(Reader reader) {
            super(reader);
            LOG.add("открыт reader");
        }

        @Override
        public void close() throws IOException {
            LOG.add("закрыт reader");
            closed = true;
            super.close();
        }
    }

    static class TracedWriter extends BufferedWriter {
        boolean closed;

        TracedWriter(Writer writer) {
            super(writer);
            LOG.add("открыт writer");
        }

        @Override
        public void close() throws IOException {
            LOG.add("закрыт writer");
            closed = true;
            super.close();
        }
    }

    static TracedReader lastReader;
    static TracedWriter lastWriter;

    public static void copyUpperCase(Path in, Path out) throws IOException {
        try (TracedReader reader = new TracedReader(Files.newBufferedReader(in, StandardCharsets.UTF_8));
             TracedWriter writer = new TracedWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            lastReader = reader;
            lastWriter = writer;
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                if (line.contains("BROKEN")) {
                    LOG.add("исключение на строке " + number);
                    throw new IllegalStateException("Сбой в середине pipeline на строке " + number);
                }
                writer.write(line.toUpperCase());
                writer.newLine();
                LOG.add("записана строка " + number);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path in = dir.resolve("pipeline-in.txt");
        Path out = dir.resolve("pipeline-out.txt");
        Files.writeString(in, "java\nsql\nBROKEN\nalgorithms\n", StandardCharsets.UTF_8);

        try {
            copyUpperCase(in, out);
        } catch (IllegalStateException e) {
            LOG.add("catch: " + e.getMessage());
        }

        for (int i = 0; i < LOG.size(); i++) {
            System.out.println((i + 1) + ". " + LOG.get(i));
        }
        System.out.println("reader закрыт: " + lastReader.closed + ", writer закрыт: " + lastWriter.closed);
        System.out.println("Закрытие в обратном порядке открытия: "
                + (LOG.indexOf("закрыт writer") < LOG.indexOf("закрыт reader")));
        System.out.println("Ресурсы закрыты раньше catch: "
                + (LOG.indexOf("закрыт reader") < LOG.size() - 1));
        System.out.println("В выходном файле (то, что успели записать): " + Files.readAllLines(out));
    }
}
