package com.smartstudy.service;

import com.smartstudy.model.ExtractedItem;
import com.smartstudy.model.TaskType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyllabusParser {
    private static final int MAX_TITLE_LENGTH = 180;

    private static final Pattern WEIGHT = Pattern.compile("(?<!\\d)(\\d{1,3}(?:\\.\\d+)?)\\s*%");
    private static final Pattern DATE = Pattern.compile(
            "(?i)(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|" +
            "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|" +
            "Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{4})"
    );

    private static final Pattern ASSIGNMENT = Pattern.compile("(?i)\\b(assignment|project|coursework|homework)\\b");
    private static final Pattern EXAM = Pattern.compile("(?i)\\b(mid[- ]?term(?:\\s+exam)?|final\\s+exam|exam(?:ination)?)\\b");
    private static final Pattern QUIZ = Pattern.compile("(?i)\\bquiz(?:zes)?\\b");
    private static final Pattern POLICY_TEXT = Pattern.compile(
            "(?i)\\b(absence|attendance policy|dismissal|waived|academic integrity|plagiarism|misconduct|" +
            "disciplinary|appeal policy|withdrawal policy|classroom policy)\\b"
    );

    public List<ExtractedItem> parse(Path file) throws Exception {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        List<String> lines;

        if (name.endsWith(".pdf")) {
            try (var document = Loader.loadPDF(file.toFile())) {
                String text = new PDFTextStripper().getText(document);
                lines = text.lines().toList();
            }
        } else if (name.endsWith(".docx")) {
            try (InputStream input = Files.newInputStream(file);
                 XWPFDocument document = new XWPFDocument(input)) {
                lines = extractDocxLines(document);
            }
        } else {
            throw new IllegalArgumentException("Only PDF and DOCX files are supported.");
        }

        return parseLines(lines);
    }

    public List<ExtractedItem> parseText(String text) {
        return parseLines(text == null ? List.of() : text.lines().toList());
    }

    private List<String> extractDocxLines(XWPFDocument document) {
        List<String> lines = new ArrayList<>();
        document.getParagraphs().forEach(paragraph -> lines.add(paragraph.getText()));

        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                for (XWPFTableCell cell : row.getTableCells()) {
                    String value = normalize(cell.getText());
                    if (!value.isBlank()) {
                        cells.add(value);
                    }
                }
                if (!cells.isEmpty()) {
                    lines.add(String.join(" | ", cells));
                }
            }
        }
        return lines;
    }

    private List<ExtractedItem> parseLines(List<String> sourceLines) {
        Map<String, ExtractedItem> unique = new LinkedHashMap<>();
        LocalDateTime extractedAt = LocalDateTime.now();
        List<String> normalizedLines = sourceLines.stream().map(this::normalize).filter(line -> !line.isBlank()).toList();
        List<String> candidates = new ArrayList<>(normalizedLines);

        // Some syllabi place the assessment name on one line and its date/weight on the next.
        for (int index = 0; index < normalizedLines.size(); index++) {
            String line = normalizedLines.get(index);
            if (detectType(line) == null || DATE.matcher(line).find() || WEIGHT.matcher(line).find()) {
                continue;
            }
            if (index + 1 < normalizedLines.size()) {
                candidates.add(line + " | " + normalizedLines.get(index + 1));
            }
            if (index + 2 < normalizedLines.size()) {
                candidates.add(line + " | " + normalizedLines.get(index + 1) + " | " + normalizedLines.get(index + 2));
            }
        }

        for (String raw : candidates) {
            String line = normalize(raw);
            if (!isPlausibleAssessmentLine(line)) {
                continue;
            }

            TaskType type = detectType(line);
            if (type == null) {
                continue;
            }

            Matcher dateMatcher = DATE.matcher(line);
            Matcher weightMatcher = WEIGHT.matcher(line);
            boolean hasDate = dateMatcher.find();
            boolean hasWeight = weightMatcher.find();

            // A keyword by itself is too weak. A real assessment line must also carry a date or weighting.
            if (!hasDate && !hasWeight) {
                continue;
            }

            LocalDateTime dueDate = hasDate ? parseDate(dateMatcher.group(1)) : null;
            double weight = hasWeight ? parseWeight(weightMatcher.group(1)) : 0.0;
            String title = extractTitle(line, type);

            if (!isPlausibleTitle(title)) {
                continue;
            }

            ExtractedItem item = new ExtractedItem(
                    0,
                    title,
                    type,
                    dueDate,
                    extractedAt,
                    weight,
                    false,
                    0,
                    null
            );
            String key = type + "|" + title.toLowerCase(Locale.ROOT) + "|" + dueDate + "|" + weight;
            unique.putIfAbsent(key, item);
        }

        return List.copyOf(unique.values());
    }

    private boolean isPlausibleAssessmentLine(String line) {
        if (line.isBlank() || line.length() > 420) {
            return false;
        }
        if (POLICY_TEXT.matcher(line).find()) {
            return false;
        }
        return detectType(line) != null;
    }

    private TaskType detectType(String value) {
        if (QUIZ.matcher(value).find()) {
            return TaskType.QUIZ;
        }
        if (EXAM.matcher(value).find()) {
            return TaskType.EXAM;
        }
        if (ASSIGNMENT.matcher(value).find()) {
            return TaskType.ASSIGNMENT;
        }
        return null;
    }

    private String extractTitle(String line, TaskType type) {
        String title = DATE.matcher(line).replaceAll(" ");
        title = WEIGHT.matcher(title).replaceAll(" ");
        title = title.replaceAll("(?i)\\b(due(?:\\s+date)?|deadline|submission date|exam date|weight(?:ing)?|grade percentage|percentage|marks?)\\b", " ");
        title = title.replaceAll("(?i)\\b(assessment component|assessment item|assessment type|component)\\b", " ");
        title = title.replaceAll("^[\\s|:;,.\\-–—#0-9.)]+", "");
        title = title.replaceAll("[\\s|:;,.\\-–—]+$", "");
        title = normalize(title);

        if (title.isBlank()) {
            title = switch (type) {
                case ASSIGNMENT -> "Assignment";
                case EXAM -> "Exam";
                case QUIZ -> "Quiz";
            };
        }

        return truncateAtWord(title, MAX_TITLE_LENGTH);
    }

    private boolean isPlausibleTitle(String title) {
        if (title.isBlank()) {
            return false;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        if (POLICY_TEXT.matcher(lower).find()) {
            return false;
        }
        int words = title.split("\\s+").length;
        if (words > 24) {
            return false;
        }
        // Long prose sentences are generally policy text, not assessment names.
        return !(title.length() > 130 && (title.contains(",") || title.endsWith(".")));
    }

    private double parseWeight(String value) {
        try {
            double weight = Double.parseDouble(value);
            return weight >= 0 && weight <= 100 ? weight : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private LocalDateTime parseDate(String input) {
        String cleaned = input.trim()
                .replace(",", "")
                .replaceAll("(?i)(\\d)(st|nd|rd|th)", "$1")
                .replaceAll("/(\\d{2})$", "/20$1")
                .replaceAll("-(\\d{2})$", "-20$1");

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("uuuu-M-d"),
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("M/d/uuuu"),
                DateTimeFormatter.ofPattern("M-d-uuuu"),
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d uuuu").toFormatter(Locale.ENGLISH),
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d uuuu").toFormatter(Locale.ENGLISH)
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(cleaned, formatter).atTime(23, 59);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("[\\t\\r\\n]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String truncateAtWord(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        String shortened = value.substring(0, maxLength).trim();
        int finalSpace = shortened.lastIndexOf(' ');
        if (finalSpace > 80) {
            shortened = shortened.substring(0, finalSpace);
        }
        return shortened + "…";
    }
}
