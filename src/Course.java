
import java.util.List;

public record Course(String code, String title, int hours) {

    public Course {
        if (code == null || code.isBlank() || code.contains(";")) {
            throw new IllegalArgumentException("Неверный код курса: '" + code + "'");
        }
        if (title == null || title.isBlank() || title.contains(";")) {
            throw new IllegalArgumentException("Неверное название курса: '" + title + "'");
        }
        if (hours <= 0) {
            throw new IllegalArgumentException("Часы должны быть больше нуля: " + hours);
        }
    }

    public String toLine() {
        return code + ";" + title + ";" + hours;
    }

    public static Course parse(String line) {
        String[] parts = line.split(";", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Ожидается 3 поля через ';', получено " + parts.length);
        }
        int hours;
        try {
            hours = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Часы не число: '" + parts[2] + "'");
        }
        return new Course(parts[0].trim(), parts[1].trim(), hours);
    }

    public static List<Course> sample() {
        return List.of(
                new Course("JAVA-101", "Основы Java", 72),
                new Course("SQL-201", "Базы данных и SQL", 48),
                new Course("ALG-150", "Алгоритмы — структуры данных", 64));
    }
}
