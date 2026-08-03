package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.assessment.*;
import com.webapp.cognitodemo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    @Autowired private AssessmentCategoryRepo categoryRepo;
    @Autowired private AssessmentModuleRepo moduleRepo;
    @Autowired private McqSectionRepo sectionRepo;
    @Autowired private McqQuestionRepo questionRepo;
    @Autowired private UserAttemptRepo attemptRepo;
    @Autowired private SpeakingAttemptRepo speakingAttemptRepo;
    @Autowired private WritingAttemptRepo writingAttemptRepo;

    // ── Categories ───────────────────────────────────────────────────────────

    public List<Map<String, Object>> getAllCategories() {
        return categoryRepo.findAll().stream()
                .sorted(Comparator.comparingInt(AssessmentCategory::getDisplayOrder))
                .map(this::categoryToMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getCategoryWithModules(String categoryId) {
        AssessmentCategory cat = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + categoryId));
        List<Map<String, Object>> modules = moduleRepo
                .findByCategoryIdOrderByDisplayOrder(categoryId)
                .stream().map(this::moduleToMap).collect(Collectors.toList());
        Map<String, Object> result = categoryToMap(cat);
        result.put("modules", modules);
        return result;
    }

    public AssessmentCategory createCategory(AssessmentCategoryRequest req) {
        if (categoryRepo.existsById(req.getId()))
            throw new IllegalArgumentException("Category already exists: " + req.getId());
        return categoryRepo.save(buildCategory(req));
    }

    public AssessmentCategory updateCategory(String id, AssessmentCategoryRequest req) {
        AssessmentCategory cat = categoryRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + id));
        applyCategory(cat, req);
        return categoryRepo.save(cat);
    }

    public void deleteCategory(String id) {
        if (!categoryRepo.existsById(id))
            throw new NoSuchElementException("Category not found: " + id);
        categoryRepo.deleteById(id);
    }

    // ── Modules ──────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getModulesForCategory(String categoryId) {
        return moduleRepo.findByCategoryIdOrderByDisplayOrder(categoryId)
                .stream().map(this::moduleToMap).collect(Collectors.toList());
    }

    public AssessmentModule createModule(AssessmentModuleRequest req) {
        if (moduleRepo.existsById(req.getId()))
            throw new IllegalArgumentException("Module already exists: " + req.getId());
        return moduleRepo.save(buildModule(req));
    }

    public AssessmentModule updateModule(String id, AssessmentModuleRequest req) {
        AssessmentModule mod = moduleRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Module not found: " + id));
        applyModule(mod, req);
        return moduleRepo.save(mod);
    }

    public void deleteModule(String id) {
        if (!moduleRepo.existsById(id))
            throw new NoSuchElementException("Module not found: " + id);
        moduleRepo.deleteById(id);
    }

    // ── MCQ Questions ─────────────────────────────────────────────────────────

    public Map<String, Object> getQuestionsForCategory(String categoryId) {
        AssessmentCategory cat = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + categoryId));
        List<McqSection> sections = sectionRepo.findByCategoryIdOrderBySectionOrder(categoryId);
        List<Map<String, Object>> sectionList = sections.stream().map(sec -> {
            List<Map<String, Object>> questions = questionRepo
                    .findBySectionIdOrderByQuestionOrder(sec.getId())
                    .stream().map(this::questionToMap).collect(Collectors.toList());
            Map<String, Object> secMap = new LinkedHashMap<>();
            secMap.put("id", sec.getId());
            secMap.put("label", sec.getTitle());
            secMap.put("sectionOrder", sec.getSectionOrder());
            secMap.put("questions", questions);
            return secMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categoryId", categoryId);
        result.put("testTitle", cat.getTestTitle() != null ? cat.getTestTitle() : cat.getTitle() + " Assessment");
        result.put("sections", sectionList);
        return result;
    }

    public McqQuestion createQuestion(McqQuestionRequest req) {
        return questionRepo.save(buildQuestion(req));
    }

    public McqQuestion updateQuestion(Long id, McqQuestionRequest req) {
        McqQuestion q = questionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found: " + id));
        applyQuestion(q, req);
        return questionRepo.save(q);
    }

    public void deleteQuestion(Long id) {
        if (!questionRepo.existsById(id))
            throw new NoSuchElementException("Question not found: " + id);
        questionRepo.deleteById(id);
    }

    // ── Overall Progress Stats ────────────────────────────────────────────────

    public Map<String, Object> getProgress(String email) {
        // Best score % per MCQ module
        Map<String, Double> bestMcq = new HashMap<>();
        for (UserAttempt a : attemptRepo.findByUserEmail(email)) {
            double pct = a.getTotal() > 0 ? (a.getScore() * 100.0 / a.getTotal()) : 0;
            bestMcq.merge(a.getModuleId(), pct, Math::max);
        }

        // Group MCQ scores by category
        Map<String, List<Double>> byCat = new HashMap<>();
        for (Map.Entry<String, Double> e : bestMcq.entrySet()) {
            moduleRepo.findById(e.getKey()).ifPresent(mod ->
                byCat.computeIfAbsent(mod.getCategoryId(), k -> new ArrayList<>()).add(e.getValue()));
        }

        // Speaking / writing best scores (already 0-100)
        OptionalDouble bestSpeaking = speakingAttemptRepo
                .findByUserEmailOrderByAttemptedAtDesc(email).stream()
                .mapToDouble(SpeakingAttempt::getScore).max();
        OptionalDouble bestWriting = writingAttemptRepo
                .findByUserEmailOrderByAttemptedAtDesc(email).stream()
                .mapToDouble(WritingAttempt::getScore).max();

        // Skill averages
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
        int masteryPct = masteryInputs.isEmpty() ? 0
                : (int) Math.round(masteryInputs.stream().mapToDouble(d -> d).average().getAsDouble());

        int totalAttempted = bestMcq.size()
                + (bestSpeaking.isPresent() ? 1 : 0)
                + (bestWriting.isPresent()  ? 1 : 0);

        // avgScore on 0-10 scale, 1 decimal
        double avgScore = Math.round(masteryPct / 10.0 * 10.0) / 10.0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("completed",  totalAttempted);
        m.put("avgScore",   avgScore);
        m.put("overallPct", masteryPct);
        return m;
    }

    private double avg(List<Double> list) {
        if (list == null || list.isEmpty()) return 0;
        return list.stream().mapToDouble(d -> d).average().orElse(0);
    }

    // ── Attempts ─────────────────────────────────────────────────────────────

    public Map<String, List<Map<String, Object>>> getUserAttempts(String email) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        // MCQ attempts — exclude speaking/writing which now have dedicated tables
        attemptRepo.findByUserEmail(email).stream()
            .filter(a -> !"speaking-test".equals(a.getModuleId()) && !"writing-test".equals(a.getModuleId()))
            .forEach(a -> result.computeIfAbsent(a.getModuleId(), k -> new ArrayList<>()).add(attemptToMap(a)));

        // Speaking attempts — sorted ascending so attempt 1 is first in the list
        speakingAttemptRepo.findByUserEmailOrderByAttemptedAtDesc(email).stream()
            .sorted(Comparator.comparingInt(SpeakingAttempt::getAttemptNumber))
            .forEach(a -> result.computeIfAbsent(a.getModuleId(), k -> new ArrayList<>()).add(speakingAttemptToMap(a)));

        // Writing attempts — same ordering
        writingAttemptRepo.findByUserEmailOrderByAttemptedAtDesc(email).stream()
            .sorted(Comparator.comparingInt(WritingAttempt::getAttemptNumber))
            .forEach(a -> result.computeIfAbsent(a.getModuleId(), k -> new ArrayList<>()).add(writingAttemptToMap(a)));

        return result;
    }

    public Map<String, Object> submitAttempt(String email, String moduleId, UserAttemptRequest req) {
        int count = attemptRepo.countByUserEmailAndModuleId(email, moduleId);
        if (count >= 3)
            throw new IllegalStateException("Maximum attempts (3) reached for module: " + moduleId);
        UserAttempt attempt = UserAttempt.builder()
                .userEmail(email).moduleId(moduleId)
                .score(req.getScore()).total(req.getTotal())
                .attemptNumber(count + 1)
                .build();
        return attemptToMap(attemptRepo.save(attempt));
    }

    // ── Seeder helpers (called from DataSeeder) ───────────────────────────────

    public McqSection seedSection(String categoryId, String title, int order) {
        return sectionRepo.save(McqSection.builder()
                .categoryId(categoryId).title(title).sectionOrder(order).build());
    }

    public void seedQuestion(Long sectionId, String text,
                             String a, String b, String c, String d,
                             int correct, int order) {
        questionRepo.save(McqQuestion.builder()
                .sectionId(sectionId).questionText(text)
                .optionA(a).optionB(b).optionC(c).optionD(d)
                .correctAnswer(correct).questionOrder(order).build());
    }

    // ── Builders & appliers ───────────────────────────────────────────────────

    private AssessmentCategory buildCategory(AssessmentCategoryRequest req) {
        AssessmentCategory c = new AssessmentCategory();
        c.setId(req.getId());
        applyCategory(c, req);
        return c;
    }

    private void applyCategory(AssessmentCategory c, AssessmentCategoryRequest req) {
        c.setTitle(req.getTitle()); c.setTopbarLabel(req.getTopbarLabel());
        c.setHeading(req.getHeading()); c.setAccentWord(req.getAccentWord());
        c.setSubtitle(req.getSubtitle()); c.setBadge(req.getBadge());
        c.setTip(req.getTip()); c.setTag(req.getTag());
        c.setIcon(req.getIcon()); c.setTestTitle(req.getTestTitle());
        c.setDisplayOrder(req.getDisplayOrder());
    }

    private AssessmentModule buildModule(AssessmentModuleRequest req) {
        AssessmentModule m = new AssessmentModule();
        m.setId(req.getId());
        applyModule(m, req);
        return m;
    }

    private void applyModule(AssessmentModule m, AssessmentModuleRequest req) {
        m.setCategoryId(req.getCategoryId()); m.setTitle(req.getTitle());
        m.setLevel(req.getLevel()); m.setDescription(req.getDescription());
        m.setTag(req.getTag()); m.setType(req.getType());
        m.setIcon(req.getIcon()); m.setDuration(req.getDuration());
        m.setQuestions(req.getQuestions()); m.setDisplayOrder(req.getDisplayOrder());
    }

    private void applyQuestion(McqQuestion q, McqQuestionRequest req) {
        q.setSectionId(req.getSectionId()); q.setQuestionText(req.getQuestionText());
        q.setOptionA(req.getOptionA()); q.setOptionB(req.getOptionB());
        q.setOptionC(req.getOptionC()); q.setOptionD(req.getOptionD());
        q.setCorrectAnswer(req.getCorrectAnswer()); q.setQuestionOrder(req.getQuestionOrder());
    }

    private McqQuestion buildQuestion(McqQuestionRequest req) {
        McqQuestion q = new McqQuestion();
        applyQuestion(q, req);
        return q;
    }

    // ── Map projections ───────────────────────────────────────────────────────

    private Map<String, Object> categoryToMap(AssessmentCategory c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("title", c.getTitle());
        m.put("topbarLabel", c.getTopbarLabel()); m.put("heading", c.getHeading());
        m.put("accentWord", c.getAccentWord()); m.put("subtitle", c.getSubtitle());
        m.put("badge", c.getBadge()); m.put("tip", c.getTip());
        m.put("tag", c.getTag()); m.put("icon", c.getIcon());
        m.put("testTitle", c.getTestTitle()); m.put("displayOrder", c.getDisplayOrder());
        return m;
    }

    private Map<String, Object> moduleToMap(AssessmentModule mod) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mod.getId()); m.put("categoryId", mod.getCategoryId());
        m.put("title", mod.getTitle()); m.put("level", mod.getLevel());
        m.put("description", mod.getDescription()); m.put("tag", mod.getTag());
        m.put("type", mod.getType()); m.put("icon", mod.getIcon());
        m.put("duration", mod.getDuration()); m.put("questions", mod.getQuestions());
        m.put("displayOrder", mod.getDisplayOrder());
        return m;
    }

    private Map<String, Object> questionToMap(McqQuestion q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("text", q.getQuestionText());
        m.put("options", List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()));
        m.put("correct", q.getCorrectAnswer());
        m.put("points", 2.0);
        return m;
    }

    private Map<String, Object> attemptToMap(UserAttempt a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("moduleId", a.getModuleId());
        m.put("score", a.getScore());
        m.put("total", a.getTotal());
        m.put("attemptNumber", a.getAttemptNumber());
        m.put("attemptedAt", a.getAttemptedAt() != null ? a.getAttemptedAt().toString() : null);
        return m;
    }

    private Map<String, Object> speakingAttemptToMap(SpeakingAttempt a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("moduleId", a.getModuleId());
        m.put("score", a.getScore());
        m.put("total", 100);
        m.put("attemptNumber", a.getAttemptNumber());
        m.put("attemptedAt", a.getAttemptedAt() != null ? a.getAttemptedAt().toString() : null);
        return m;
    }

    private Map<String, Object> writingAttemptToMap(WritingAttempt a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("moduleId", a.getModuleId());
        m.put("score", a.getScore());
        m.put("total", 100);
        m.put("attemptNumber", a.getAttemptNumber());
        m.put("attemptedAt", a.getAttemptedAt() != null ? a.getAttemptedAt().toString() : null);
        return m;
    }
}
