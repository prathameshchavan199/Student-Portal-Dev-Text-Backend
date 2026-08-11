package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.assessment.SpeakingAttempt;
import com.webapp.cognitodemo.entity.assessment.UserAttempt;
import com.webapp.cognitodemo.entity.assessment.WritingAttempt;
import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.entity.course.CourseProgress;
import com.webapp.cognitodemo.entity.payment.Payment;
import com.webapp.cognitodemo.entity.registration.StudentRegistrationDraft;
import com.webapp.cognitodemo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired private RegistrationDraftRepo draftRepo;
    @Autowired private UserRepo              userRepo;
    @Autowired private UserAttemptRepo       attemptRepo;
    @Autowired private SpeakingAttemptRepo   speakingRepo;
    @Autowired private WritingAttemptRepo    writingRepo;
    @Autowired private AssessmentModuleRepo  moduleRepo;
    @Autowired private PaymentRepo           paymentRepo;
    @Autowired private CourseRepo            courseRepo;
    @Autowired private CourseProgressRepo    courseProgressRepo;

    public Map<String, Object> getSummary(String email) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registration", computeRegistration(email));
        result.put("assessment",   computeAssessment(email));
        result.put("enrollment",   computeEnrollment(email));
        result.put("courses",      computeCourses(email));
        result.put("sessions",     computeSessions(email));
        return result;
    }

    // ── Registration Progress ──────────────────────────────────────────────────

    private Map<String, Object> computeRegistration(String email) {
        boolean registered = userRepo.findByEmail(email)
            .map(u -> u.isRegistration()).orElse(false);

        Optional<StudentRegistrationDraft> opt = draftRepo.findByEmail(email);
        if (opt.isEmpty()) {
            return registrationResult(List.of(
                step("Basic Data",  false),
                step("Education",   false),
                step("Projects",    false),
                step("Experience",  false)
            ), 0, registered);
        }

        StudentRegistrationDraft d = opt.get();
        List<Map<String, Object>> steps = List.of(
            step("Basic Data",
                notBlank(d.getFullName()) && notBlank(d.getPhone()) && notBlank(d.getCountry())),
            step("Education",
                notBlank(d.getQualificationAfter10th()) || notBlank(d.getSchool()) || notBlank(d.getTenthGpa())),
            step("Projects",
                Boolean.FALSE.equals(d.getHasProjects())
                    || (d.getProjects() != null && d.getProjects().stream()
                        .anyMatch(p -> notBlank(p.getTitle())))),
            step("Experience",
                Boolean.FALSE.equals(d.getHasWorkExperience())
                    || (d.getPositions() != null && d.getPositions().stream()
                        .anyMatch(p -> notBlank(p.getCompanyName()) || notBlank(p.getRole()))))
        );

        int done = (int) steps.stream().filter(s -> Boolean.TRUE.equals(s.get("done"))).count();
        int pct  = (int) Math.round((done / (double) steps.size()) * 100);
        return registrationResult(steps, pct, registered);
    }

    private Map<String, Object> registrationResult(List<Map<String, Object>> steps, int pct, boolean registered) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pct",        pct);
        m.put("registered", registered);
        m.put("steps",      steps);
        return m;
    }

    private Map<String, Object> step(String label, boolean done) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("done",  done);
        return m;
    }

    // ── Assessment Scores ─────────────────────────────────────────────────────

    private Map<String, Object> computeAssessment(String email) {

        // MCQ: best score % per module
        Map<String, Double> bestMcq = new HashMap<>();
        for (UserAttempt a : attemptRepo.findByUserEmail(email)) {
            double pct = a.getTotal() > 0 ? (a.getScore() * 100.0 / a.getTotal()) : 0;
            bestMcq.merge(a.getModuleId(), pct, Math::max);
        }

        // Map each MCQ moduleId → categoryId
        Map<String, List<Double>> byCat = new HashMap<>();
        for (Map.Entry<String, Double> e : bestMcq.entrySet()) {
            moduleRepo.findById(e.getKey()).ifPresent(mod ->
                byCat.computeIfAbsent(mod.getCategoryId(), k -> new ArrayList<>()).add(e.getValue()));
        }

        // Speaking/writing — best score per module (score already 0–100)
        OptionalDouble bestSpeaking = speakingRepo.findByUserEmailOrderByAttemptedAtDesc(email).stream()
            .mapToDouble(SpeakingAttempt::getScore).max();
        OptionalDouble bestWriting  = writingRepo.findByUserEmailOrderByAttemptedAtDesc(email).stream()
            .mapToDouble(WritingAttempt::getScore).max();

        // Aggregate skill groups
        List<Double> techScores = new ArrayList<>();
        if (byCat.containsKey("technical-skills")) techScores.addAll(byCat.get("technical-skills"));
        if (byCat.containsKey("data-skills"))      techScores.addAll(byCat.get("data-skills"));
        double technical    = avg(techScores);
        double analytical   = avg(byCat.get("problem-solving"));

        List<Double> commScores = new ArrayList<>();
        if (bestSpeaking.isPresent()) commScores.add(bestSpeaking.getAsDouble());
        if (bestWriting.isPresent())  commScores.add(bestWriting.getAsDouble());
        double communication = avg(commScores.isEmpty() ? null : commScores);

        // Mastery = average only across categories where the user has data
        List<Double> masteryInputs = new ArrayList<>();
        if (technical     > 0) masteryInputs.add(technical);
        if (analytical    > 0) masteryInputs.add(analytical);
        if (communication > 0) masteryInputs.add(communication);
        int masteryScore = masteryInputs.isEmpty() ? 0
            : (int) Math.round(masteryInputs.stream().mapToDouble(d -> d).average().getAsDouble());

        int totalAttempted = bestMcq.size()
            + (bestSpeaking.isPresent() ? 1 : 0)
            + (bestWriting.isPresent()  ? 1 : 0);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("technical",      (int) Math.round(technical));
        m.put("analytical",     (int) Math.round(analytical));
        m.put("communication",  (int) Math.round(communication));
        m.put("masteryScore",   masteryScore);
        m.put("totalAttempted", totalAttempted);
        return m;
    }

    private double avg(List<Double> list) {
        if (list == null || list.isEmpty()) return 0;
        return list.stream().mapToDouble(d -> d).average().orElse(0);
    }

    // ── Enrollment Breakdown ──────────────────────────────────────────────────

    private Map<String, Object> computeEnrollment(String email) {
        // Auto-create REGISTERED rows for any PAID courses that don't have one yet
        for (Payment p : paymentRepo.findByUserEmailAndStatus(email, "PAID")) {
            boolean exists = courseProgressRepo
                    .findByUserEmailAndCourseId(email, p.getCourseId())
                    .isPresent();
            if (!exists) {
                CourseProgress cp = new CourseProgress();
                cp.setUserEmail(email);
                cp.setCourseId(p.getCourseId());
                cp.setCourseName(p.getCourseName());
                cp.setStatus("REGISTERED");
                cp.setProgressPct(0);
                courseProgressRepo.save(cp);
            }
        }

        List<CourseProgress> all = courseProgressRepo.findByUserEmail(email);
        int registered = all.size(); // total purchased courses regardless of progress status
        int inProgress = (int) all.stream().filter(cp -> "IN_PROGRESS".equals(cp.getStatus())).count();
        int completed  = (int) all.stream().filter(cp -> "COMPLETED".equals(cp.getStatus())).count();
        int certified  = (int) all.stream().filter(cp -> "CERTIFIED".equals(cp.getStatus())).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("registered",  registered);
        m.put("inProgress",  inProgress);
        m.put("completed",   completed);
        m.put("certified",   certified);
        return m;
    }

    // ── Courses ───────────────────────────────────────────────────────────────

    private Map<String, Object> computeCourses(String email) {
        // Deduplicate by courseId — multiple PAID records can exist for the same course
        Map<String, Payment> uniquePaid = new LinkedHashMap<>();
        for (Payment p : paymentRepo.findByUserEmailAndStatus(email, "PAID")) {
            uniquePaid.putIfAbsent(p.getCourseId(), p);
        }
        Set<String> paidIds = uniquePaid.keySet();

        List<Map<String, Object>> enrolled = uniquePaid.values().stream().map(p -> {
            Optional<Course> c = courseRepo.findById(p.getCourseId());
            int pct = courseProgressRepo
                    .findByUserEmailAndCourseId(email, p.getCourseId())
                    .map(CourseProgress::getProgressPct)
                    .orElse(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",       p.getCourseId());
            row.put("title",    c.map(Course::getTitle).orElse(p.getCourseName()));
            row.put("category", c.map(Course::getCategory).orElse(""));
            row.put("imageUrl", c.map(Course::getImageUrl).orElse(""));
            row.put("format",   c.map(Course::getFormat).orElse(""));
            row.put("progress", pct);
            return row;
        }).collect(Collectors.toList());

        List<Map<String, Object>> whatsNew = courseRepo.findAll().stream()
            .filter(c -> !paidIds.contains(c.getId()))
            .limit(4)
            .map(c -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",    c.getId());
                row.put("title", c.getTitle());
                row.put("level", c.getLevel());
                row.put("price", c.getPrice());
                return row;
            }).collect(Collectors.toList());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enrolled", enrolled);
        m.put("whatsNew", whatsNew);
        return m;
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    private Map<String, Object> computeSessions(String email) {
        // Collect unique paid course IDs for this student
        Set<String> paidIds = paymentRepo.findByUserEmailAndStatus(email, "PAID")
                .stream()
                .map(Payment::getCourseId)
                .collect(Collectors.toSet());

        long onDemand = 0, online = 0, offline = 0;
        for (String courseId : paidIds) {
            Optional<Course> opt = courseRepo.findById(courseId);
            if (opt.isEmpty()) continue;
            String fmt = opt.get().getFormat() == null ? "" : opt.get().getFormat().toLowerCase();
            if (fmt.contains("demand") || fmt.contains("self")) {
                onDemand++;
            } else if (fmt.contains("offline") || fmt.contains("campus")
                    || fmt.contains("person")  || fmt.contains("classroom")) {
                offline++;
            } else {
                online++;
            }
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("onDemand", (int) onDemand);
        m.put("online",   (int) online);
        m.put("offline",  (int) offline);
        return m;
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
