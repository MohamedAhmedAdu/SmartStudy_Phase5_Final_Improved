package com.smartstudy.service;

import com.smartstudy.model.AcademicTask;
import com.smartstudy.model.StudySession;
import com.smartstudy.model.TaskStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScheduleGenerator {
    private static final LocalTime DEFAULT_START = LocalTime.of(18, 0);
    private static final LocalTime STUDY_DAY_END = LocalTime.of(22, 0);
    private static final double MAX_SESSION_HOURS = 1.5;
    private static final double MIN_SESSION_HOURS = 0.25;
    private static final int PLAN_DAYS = 7;

    public record Result(List<StudySession> sessions, List<String> warnings) {}
    private record Ranked(AcademicTask task, double score) {}
    private record Slot(LocalDateTime start, double availableHours) {}

    /**
     * Generates a rolling seven-day plan beginning today. The plan is not tied to
     * the Monday-Sunday calendar week, so regenerating on a Sunday evening still
     * uses the student's full weekly availability across the next seven days.
     */
    public Result generate(List<AcademicTask> tasks, double weeklyHours, LocalDate planDate) {
        return generate(tasks, weeklyHours, planDate, LocalDateTime.now());
    }

    Result generate(List<AcademicTask> tasks, double weeklyHours, LocalDate planDate, LocalDateTime now) {
        if (weeklyHours <= 0) {
            throw new IllegalArgumentException("Available study hours must be positive.");
        }

        LocalDate planStart = now.toLocalDate();
        LocalDate planEnd = planStart.plusDays(PLAN_DAYS - 1L);
        List<String> warnings = new ArrayList<>();
        List<Ranked> ranked = new ArrayList<>();

        for (AcademicTask task : tasks) {
            if (task.status() == TaskStatus.COMPLETED || task.dueDate() == null) {
                continue;
            }
            if (!task.dueDate().isAfter(now)) {
                warnings.add("Overdue task needs manual attention: " + task.title() + ".");
                continue;
            }
            ranked.add(new Ranked(task, score(task, now)));
        }

        ranked.sort(Comparator.comparingDouble(Ranked::score).reversed());
        if (ranked.isEmpty()) {
            if (warnings.isEmpty()) {
                warnings.add("No pending tasks with future due dates are available.");
            }
            return new Result(List.of(), List.copyOf(warnings));
        }

        long availableDays = Math.max(1, ChronoUnit.DAYS.between(planStart, planEnd) + 1);
        // Spread the workload over roughly five study days while keeping a hard
        // maximum of four hours on any single day. The weeklyHours value remains
        // the absolute total limit for the complete rolling plan.
        double normalizedWeeklyHours = Math.max(MIN_SESSION_HOURS,
                Math.floor(weeklyHours * 4.0) / 4.0);
        double dailyCap = Math.min(4.0,
                roundUpQuarter(Math.max(1.0, normalizedWeeklyHours / Math.min(5.0, availableDays))));
        double remainingWeeklyHours = normalizedWeeklyHours;
        Map<LocalDate, Double> usedByDay = new HashMap<>();
        List<StudySession> sessions = new ArrayList<>();

        for (Ranked rankedTask : ranked) {
            AcademicTask task = rankedTask.task();
            double requiredHours = roundUpQuarter(Math.max(0.5, task.estimatedHours()));

            while (requiredHours > 0.01 && remainingWeeklyHours > 0.01) {
                Slot slot = findSlot(
                        planStart,
                        planEnd,
                        task.dueDate(),
                        usedByDay,
                        dailyCap,
                        now
                );
                if (slot == null) {
                    warnings.add("Not enough available time before the deadline for: " + task.title() + ".");
                    break;
                }

                double block = Math.min(MAX_SESSION_HOURS,
                        Math.min(requiredHours,
                                Math.min(remainingWeeklyHours, slot.availableHours())));

                if (block < MIN_SESSION_HOURS) {
                    warnings.add("The remaining time window is too short for: " + task.title() + ".");
                    break;
                }

                LocalDateTime end = slot.start().plusMinutes(Math.round(block * 60));
                sessions.add(new StudySession(
                        0,
                        slot.start(),
                        end,
                        block,
                        0,
                        task.taskId(),
                        task.title()
                ));
                usedByDay.merge(slot.start().toLocalDate(), block, Double::sum);
                requiredHours -= block;
                remainingWeeklyHours -= block;
            }

            if (requiredHours > 0.01 && remainingWeeklyHours <= 0.01) {
                warnings.add("Weekly study hours are fully allocated. Increase available hours to schedule all work.");
                break;
            }
        }

        sessions.sort(Comparator.comparing(StudySession::startTime));
        return new Result(List.copyOf(sessions), List.copyOf(warnings.stream().distinct().toList()));
    }

    public double score(AcademicTask task, LocalDateTime now) {
        long days = Math.max(1, ChronoUnit.DAYS.between(now.toLocalDate(), task.dueDate().toLocalDate()));
        double urgency = Math.min(100, 100.0 / days);
        double effort = Math.min(100, task.estimatedHours() * 5);
        double progressBoost = task.status() == TaskStatus.IN_PROGRESS ? 5 : 0;
        return task.gradeWeight() * 0.50 + urgency * 0.35 + effort * 0.15 + progressBoost;
    }

    private Slot findSlot(LocalDate planStart,
                          LocalDate planEnd,
                          LocalDateTime deadline,
                          Map<LocalDate, Double> usedByDay,
                          double dailyCap,
                          LocalDateTime now) {
        LocalDate finalDay = deadline.toLocalDate().isBefore(planEnd)
                ? deadline.toLocalDate()
                : planEnd;

        for (LocalDate day = planStart; !day.isAfter(finalDay); day = day.plusDays(1)) {
            double used = usedByDay.getOrDefault(day, 0.0);
            double freeByCap = dailyCap - used;
            if (freeByCap < MIN_SESSION_HOURS) {
                continue;
            }

            LocalDateTime base = day.atTime(DEFAULT_START);
            if (day.equals(now.toLocalDate())) {
                LocalDateTime earliest = roundUpToQuarterHour(now.plusMinutes(15));
                if (earliest.isAfter(base)) {
                    base = earliest;
                }
            }

            LocalDateTime start = base.plusMinutes(Math.round(used * 60));
            LocalDateTime endOfStudyDay = day.atTime(STUDY_DAY_END);
            LocalDateTime latestEnd = deadline.isBefore(endOfStudyDay) ? deadline : endOfStudyDay;
            double freeByClock = Duration.between(start, latestEnd).toMinutes() / 60.0;
            double available = Math.min(freeByCap, freeByClock);

            if (available >= MIN_SESSION_HOURS && !start.isBefore(now)) {
                return new Slot(start, available);
            }
        }
        return null;
    }

    private double roundUpQuarter(double hours) {
        return Math.ceil(hours * 4.0 - 1e-9) / 4.0;
    }

    private LocalDateTime roundUpToQuarterHour(LocalDateTime value) {
        int minute = value.getMinute();
        int extra = (15 - minute % 15) % 15;
        return value.plusMinutes(extra).withSecond(0).withNano(0);
    }
}
