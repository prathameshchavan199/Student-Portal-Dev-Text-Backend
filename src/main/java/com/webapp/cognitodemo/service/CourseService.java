package com.webapp.cognitodemo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.entity.course.CourseRequest;
import com.webapp.cognitodemo.entity.course.CourseReview;
import com.webapp.cognitodemo.entity.course.CourseReviewRequest;
import com.webapp.cognitodemo.repo.CourseRepo;
import com.webapp.cognitodemo.repo.CourseReviewRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private CourseReviewRepo courseReviewRepo;

    @Autowired
    private S3Service s3Service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<Map<String, String>> COURSE_TYPES = List.of(
            Map.of("id", "onDemand",       "label", "On-Demand Courses",  "shortLabel", "On-Demand"),
            Map.of("id", "onlineProgram",  "label", "Online Programs",    "shortLabel", "Online"),
            Map.of("id", "offlineProgram", "label", "Offline Programs",   "shortLabel", "Offline")
    );

    public Map<String, Object> getCatalog() {
        List<Map<String, Object>> courses = courseRepo.findAll()
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return Map.of(
                "courseTypes", COURSE_TYPES,
                "courses", courses
        );
    }

    public Map<String, Object> getCourseById(String id) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + id));
        return toMap(course);
    }

    public Map<String, Object> createCourse(CourseRequest req) {
        if (courseRepo.existsById(req.getId())) {
            throw new IllegalArgumentException("A course with id '" + req.getId() + "' already exists");
        }
        Course course = buildCourse(req);
        courseRepo.save(course);
        return toMap(course);
    }

    public Map<String, Object> updateCourse(String id, CourseRequest req) {
        Course existing = courseRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + id));

        existing.setTitle(req.getTitle());
        existing.setCategory(req.getCategory());
        existing.setPrice(req.getPrice());
        existing.setDuration(req.getDuration());
        existing.setLevel(req.getLevel());
        existing.setImageUrl(req.getImageUrl());
        existing.setInstructor(req.getInstructor());
        existing.setDescription(req.getDescription());
        existing.setCourseArea(req.getCourseArea());
        existing.setTopic(req.getTopic());
        existing.setFormat(req.getFormat());
        existing.setDate(req.getDate());
        existing.setTime(req.getTime());
        existing.setPlatform(req.getPlatform());
        existing.setLocation(req.getLocation());
        existing.setStartsIn(req.getStartsIn());
        existing.setSeatsLeft(req.getSeatsLeft());
        existing.setAccent(req.getAccent());
        existing.setSessionsJson(serializeSessions(req));
        existing.setAboutCourse(req.getAboutCourse());
        existing.setYouWillLearnJson(serializeJson(req.getYouWillLearn()));
        existing.setCurriculumJson(serializeJson(req.getCurriculum()));

        courseRepo.save(existing);
        return toMap(existing);
    }

    public Map<String, Object> createReview(String courseId, CourseReviewRequest req) {
        if (!courseRepo.existsById(courseId)) {
            throw new NoSuchElementException("Course not found: " + courseId);
        }
        CourseReview review = CourseReview.builder()
                .courseId(courseId)
                .reviewerName(req.getReviewerName())
                .rating(req.getRating())
                .reviewText(req.getReviewText())
                .build();
        CourseReview saved = courseReviewRepo.save(review);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("courseId", saved.getCourseId());
        m.put("reviewerName", saved.getReviewerName());
        m.put("rating", saved.getRating());
        m.put("reviewText", saved.getReviewText());
        m.put("createdAt", saved.getCreatedAt().toString());
        return m;
    }

    public List<Map<String, Object>> getReviewsForCourse(String courseId) {
        return courseReviewRepo.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("reviewerName", r.getReviewerName());
                    m.put("rating", r.getRating());
                    m.put("reviewText", r.getReviewText());
                    m.put("createdAt", r.getCreatedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());
    }

    public String getImagePresignedUrl(String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));
        if (course.getImageKey() == null || course.getImageKey().isBlank()) {
            throw new NoSuchElementException("No S3 image found for course: " + courseId);
        }
        return s3Service.presignedUrl(course.getImageKey(), Duration.ofMinutes(15));
    }

    public Map<String, Object> uploadCourseImage(String courseId, MultipartFile file) throws IOException {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));
        String key = buildImageKey(course.getCategory(), courseId);
        s3Service.upload(key, file);
        course.setImageKey(key);
        courseRepo.save(course);
        return Map.of("imageKey", key);
    }

    private String buildImageKey(String category, String courseId) {
        return switch (category) {
            case "onlineProgram"  -> "CourseDetails/Online/online-"     + courseId + ".png";
            case "offlineProgram" -> "CourseDetails/Offline/offline-"   + courseId + ".png";
            default               -> "CourseDetails/OnDemand/ondemand-" + courseId + ".png";
        };
    }

    /*
     * A lesson can carry a video/pdf/doc *key* before the file itself has been
     * uploaded to S3 (content is added over time). Presigning a key that has no
     * object behind it still succeeds, but opening that URL makes S3 answer
     * with an XML "NoSuchKey" error — which the browser then renders straight
     * into the lesson's <video>/<iframe>. So confirm the object is really
     * there first, and report "not available yet" (404) if it isn't, so the
     * frontend can show a proper "coming soon" message instead.
     */
    private String presignIfExists(String key, String kind, int moduleIndex, int lessonIndex) {
        if (!s3Service.doesObjectExist(key)) {
            throw new NoSuchElementException(
                    "The " + kind + " for module " + moduleIndex + ", lesson " + lessonIndex + " isn't available yet");
        }
        return s3Service.presignedUrl(key, Duration.ofMinutes(30));
    }

    /*
     * Resolves the playable URL for a lesson's video.
     * Priority: a plain "videoUrl" on the lesson (e.g. a bundled/static asset
     * or external CDN link) is returned as-is; otherwise, if the lesson has
     * a "videoKey", a short-lived S3 presigned URL is generated.
     */
    public String getLessonVideoUrl(String courseId, int moduleIndex, int lessonIndex) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        Map<String, Object> lesson = getLesson(course, moduleIndex, lessonIndex);

        Object videoUrl = lesson.get("videoUrl");
        if (videoUrl instanceof String s && !s.isBlank()) {
            return s;
        }

        Object videoKey = lesson.get("videoKey");
        if (videoKey instanceof String s && !s.isBlank()) {
            return presignIfExists(s, "video", moduleIndex, lessonIndex);
        }

        throw new NoSuchElementException("No video found for module " + moduleIndex + ", lesson " + lessonIndex);
    }

    /*
     * Resolves the viewable URL for a lesson's PDF document.
     * Same priority as video: a plain "pdfUrl" on the lesson (e.g. a bundled/
     * static asset or external link) is returned as-is; otherwise, if the
     * lesson has a "pdfKey", a short-lived S3 presigned URL is generated.
     */
    public String getLessonPdfUrl(String courseId, int moduleIndex, int lessonIndex) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        Map<String, Object> lesson = getLesson(course, moduleIndex, lessonIndex);

        Object pdfUrl = lesson.get("pdfUrl");
        if (pdfUrl instanceof String s && !s.isBlank()) {
            return s;
        }

        Object pdfKey = lesson.get("pdfKey");
        if (pdfKey instanceof String s && !s.isBlank()) {
            return presignIfExists(s, "PDF", moduleIndex, lessonIndex);
        }

        throw new NoSuchElementException("No PDF found for module " + moduleIndex + ", lesson " + lessonIndex);
    }

    /*
     * Resolves the viewable URL for a lesson's Word document (.docx). Same
     * priority as video/pdf: a plain "docUrl" is returned as-is; otherwise,
     * if the lesson has a "docKey", a short-lived S3 presigned URL is
     * generated. Unlike video/pdf this URL isn't loaded directly by the
     * browser — it's embedded into an Office Online viewer link, since
     * browsers can't render .docx natively (see CourseController#getLessonDoc).
     */
    public String getLessonDocUrl(String courseId, int moduleIndex, int lessonIndex) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        Map<String, Object> lesson = getLesson(course, moduleIndex, lessonIndex);

        Object docUrl = lesson.get("docUrl");
        if (docUrl instanceof String s && !s.isBlank()) {
            return s;
        }

        Object docKey = lesson.get("docKey");
        if (docKey instanceof String s && !s.isBlank()) {
            return presignIfExists(s, "document", moduleIndex, lessonIndex);
        }

        throw new NoSuchElementException("No document found for module " + moduleIndex + ", lesson " + lessonIndex);
    }

    /*
     * A "knowledge check" lesson has no video/pdf/doc — instead it carries
     * an "mcq" array right on the lesson object in curriculumJson, e.g.:
     *   { "title": "Module 1 Knowledge Check",
     *     "mcq": [ { "text": "...", "options": ["A","B","C","D"], "correct": 0 }, ... ] }
     * This just returns that array as-is; there's no file/S3 involved.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getLessonMcq(String courseId, int moduleIndex, int lessonIndex) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        Map<String, Object> lesson = getLesson(course, moduleIndex, lessonIndex);

        Object mcq = lesson.get("mcq");
        if (mcq instanceof List<?> list && !list.isEmpty()) {
            return (List<Map<String, Object>>) list;
        }

        throw new NoSuchElementException("No knowledge check found for module " + moduleIndex + ", lesson " + lessonIndex);
    }

    /*
     * Uploads a video file to S3 for a specific lesson and stores the S3 key
     * on that lesson inside the course's curriculumJson.
     *
     * Mirrors the existing course-image convention (buildImageKey):
     *   CourseDetails/{OnDemand|Online|Offline}/{courseId}/module-{n}-lesson-{n}.mp4
     */
    public Map<String, Object> uploadLessonVideo(String courseId, int moduleIndex, int lessonIndex, MultipartFile file) throws IOException {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        String key = buildVideoKey(course.getCategory(), courseId, moduleIndex, lessonIndex);
        s3Service.upload(key, file);

        lesson.put("videoKey", key);
        lesson.remove("videoUrl"); // uploaded S3 video takes priority over any static/demo URL

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("videoKey", key);
    }

    private String buildVideoKey(String category, String courseId, int moduleIndex, int lessonIndex) {
        String folder = switch (category) {
            case "onlineProgram"  -> "CourseDetails/Online/"   + courseId;
            case "offlineProgram" -> "CourseDetails/Offline/"  + courseId;
            default               -> "CourseDetails/OnDemand/" + courseId;
        };
        return folder + "/module-" + (moduleIndex + 1) + "-lesson-" + (lessonIndex + 1) + ".mp4";
    }

    /*
     * Points a lesson at a video that's already sitting in S3 (e.g. uploaded
     * manually via the AWS console) without re-uploading anything.
     */
    public Map<String, Object> setLessonVideoKey(String courseId, int moduleIndex, int lessonIndex, String videoKey) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        if (!s3Service.doesObjectExist(videoKey)) {
            throw new IllegalArgumentException("No file found in S3 at key: " + videoKey);
        }

        lesson.put("videoKey", videoKey);
        lesson.remove("videoUrl");

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("videoKey", videoKey);
    }

    /*
     * Points a lesson at a PDF that's already sitting in S3 (e.g. uploaded
     * manually via the AWS console) without re-uploading anything.
     */
    public Map<String, Object> setLessonPdfKey(String courseId, int moduleIndex, int lessonIndex, String pdfKey) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        if (!s3Service.doesObjectExist(pdfKey)) {
            throw new IllegalArgumentException("No file found in S3 at key: " + pdfKey);
        }

        lesson.put("pdfKey", pdfKey);
        lesson.remove("pdfUrl");

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("pdfKey", pdfKey);
    }

    /*
     * Points a lesson at a Word document that's already sitting in S3 (e.g.
     * uploaded manually via the AWS console) without re-uploading anything.
     */
    public Map<String, Object> setLessonDocKey(String courseId, int moduleIndex, int lessonIndex, String docKey) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        if (!s3Service.doesObjectExist(docKey)) {
            throw new IllegalArgumentException("No file found in S3 at key: " + docKey);
        }

        lesson.put("docKey", docKey);
        lesson.remove("docUrl");

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("docKey", docKey);
    }

    /*
     * Sets/replaces a lesson's knowledge-check questions. Basic shape
     * validation only (each question needs text, at least 2 options, and a
     * correct index that's actually within range) — this is data entered by
     * an admin, not untrusted user input, so it doesn't need to be bulletproof,
     * just enough to catch an obvious typo before it reaches students.
     */
    public Map<String, Object> setLessonMcq(String courseId, int moduleIndex, int lessonIndex, List<Map<String, Object>> questions) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("At least one question is required");
        }
        for (int i = 0; i < questions.size(); i++) {
            validateMcqQuestion(i + 1, questions.get(i));
        }

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        lesson.put("mcq", questions);
        lesson.remove("videoKey");
        lesson.remove("videoUrl");
        lesson.remove("pdfKey");
        lesson.remove("pdfUrl");
        lesson.remove("docKey");
        lesson.remove("docUrl");

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("questionCount", questions.size());
    }

    /*
     * Appends question(s) to a lesson's existing knowledge check without
     * touching the ones already there (setLessonMcq, by contrast, replaces
     * the whole set). If the lesson has no knowledge check yet this creates
     * one — but refuses if the lesson already carries a video/PDF/document,
     * since silently wiping real lesson content is not what "add a question"
     * should ever do.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> addLessonMcqQuestions(String courseId, int moduleIndex, int lessonIndex, List<Map<String, Object>> newQuestions) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        if (newQuestions == null || newQuestions.isEmpty()) {
            throw new IllegalArgumentException("At least one question is required");
        }
        for (int i = 0; i < newQuestions.size(); i++) {
            validateMcqQuestion(i + 1, newQuestions.get(i));
        }

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        Object existing = lesson.get("mcq");
        List<Map<String, Object>> merged = new ArrayList<>();
        if (existing instanceof List<?> list) {
            merged.addAll((List<Map<String, Object>>) list);
        } else {
            for (String key : List.of("videoKey", "videoUrl", "pdfKey", "pdfUrl", "docKey", "docUrl")) {
                Object v = lesson.get(key);
                if (v instanceof String str && !str.isBlank()) {
                    throw new IllegalArgumentException(
                            "This lesson already has video/PDF/document content. Use the replace endpoint (POST .../mcq) if you really want to turn it into a knowledge check.");
                }
            }
        }
        merged.addAll(newQuestions);
        lesson.put("mcq", merged);

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("added", newQuestions.size(), "questionCount", merged.size());
    }

    /*
     * Removes the whole knowledge check from a lesson. The lesson is left
     * with no content (it shows as "not available yet" until something new
     * is attached).
     */
    public Map<String, Object> deleteLessonMcq(String courseId, int moduleIndex, int lessonIndex) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        if (!(lesson.get("mcq") instanceof List<?> list) || list.isEmpty()) {
            throw new NoSuchElementException("No knowledge check found for module " + moduleIndex + ", lesson " + lessonIndex);
        }
        int removed = list.size();
        lesson.remove("mcq");

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("removed", removed);
    }

    /*
     * Removes a single question (0-based questionIndex). Removing the last
     * remaining question removes the knowledge check entirely.
     */
    public Map<String, Object> deleteLessonMcqQuestion(String courseId, int moduleIndex, int lessonIndex, int questionIndex) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        List<Map<String, Object>> modules = parseCurriculum(course.getCurriculumJson());
        Map<String, Object> lesson = getLesson(modules, moduleIndex, lessonIndex);

        if (!(lesson.get("mcq") instanceof List<?> raw) || raw.isEmpty()) {
            throw new NoSuchElementException("No knowledge check found for module " + moduleIndex + ", lesson " + lessonIndex);
        }
        if (questionIndex < 0 || questionIndex >= raw.size()) {
            throw new NoSuchElementException("Question " + questionIndex + " not found (this knowledge check has " + raw.size() + " questions)");
        }

        List<Object> remaining = new ArrayList<>(raw);
        remaining.remove(questionIndex);
        if (remaining.isEmpty()) {
            lesson.remove("mcq");
        } else {
            lesson.put("mcq", remaining);
        }

        course.setCurriculumJson(serializeJson(modules));
        courseRepo.save(course);

        return Map.of("questionCount", remaining.size());
    }

    private void validateMcqQuestion(int number, Map<String, Object> q) {
        Object text = q.get("text");
        Object options = q.get("options");
        Object correct = q.get("correct");
        if (!(text instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Question " + number + " is missing text");
        }
        if (!(options instanceof List<?> opts) || opts.size() < 2) {
            throw new IllegalArgumentException("Question " + number + " needs at least 2 options");
        }
        if (!(correct instanceof Number n) || n.intValue() < 0 || n.intValue() >= opts.size()) {
            throw new IllegalArgumentException("Question " + number + " has an invalid 'correct' index");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getLesson(Course course, int moduleIndex, int lessonIndex) {
        return getLesson(parseCurriculum(course.getCurriculumJson()), moduleIndex, lessonIndex);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getLesson(List<Map<String, Object>> modules, int moduleIndex, int lessonIndex) {
        if (moduleIndex < 0 || moduleIndex >= modules.size()) {
            throw new NoSuchElementException("Module not found at index " + moduleIndex);
        }
        Object lessonsObj = modules.get(moduleIndex).get("lessons");
        if (!(lessonsObj instanceof List<?> lessons) || lessonIndex < 0 || lessonIndex >= lessons.size()) {
            throw new NoSuchElementException("Lesson not found at index " + lessonIndex);
        }
        Object lesson = lessons.get(lessonIndex);
        if (lesson instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        // Legacy lessons stored as plain strings can't hold a video — treat as not found.
        throw new NoSuchElementException("Lesson at index " + lessonIndex + " has no video metadata");
    }

    private List<Map<String, Object>> parseCurriculum(String json) {
        if (json == null || json.isBlank()) return new java.util.ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    public void deleteCourse(String id) {
        if (!courseRepo.existsById(id)) {
            throw new NoSuchElementException("Course not found: " + id);
        }
        courseRepo.deleteById(id);
    }

    private Course buildCourse(CourseRequest req) {
        return Course.builder()
                .id(req.getId())
                .title(req.getTitle())
                .category(req.getCategory())
                .price(req.getPrice())
                .duration(req.getDuration())
                .level(req.getLevel())
                .imageUrl(req.getImageUrl())
                .instructor(req.getInstructor())
                .description(req.getDescription())
                .courseArea(req.getCourseArea())
                .topic(req.getTopic())
                .format(req.getFormat())
                .date(req.getDate())
                .time(req.getTime())
                .platform(req.getPlatform())
                .location(req.getLocation())
                .startsIn(req.getStartsIn())
                .seatsLeft(req.getSeatsLeft())
                .accent(req.getAccent())
                .sessionsJson(serializeSessions(req))
                .aboutCourse(req.getAboutCourse())
                .youWillLearnJson(serializeJson(req.getYouWillLearn()))
                .curriculumJson(serializeJson(req.getCurriculum()))
                .build();
    }

    private String serializeSessions(CourseRequest req) {
        if (req.getSessions() == null || req.getSessions().isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(req.getSessions());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize sessions", e);
        }
    }

    private Map<String, Object> toMap(Course c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",          c.getId());
        map.put("title",       c.getTitle());
        map.put("category",    c.getCategory());
        map.put("price",       c.getPrice());
        map.put("duration",    nvl(c.getDuration()));
        map.put("level",       nvl(c.getLevel()));
        map.put("imageUrl",  nvl(c.getImageUrl()));
        map.put("imageKey",  c.getImageKey() != null ? c.getImageKey() : "");
        map.put("instructor",  nvl(c.getInstructor()));
        map.put("description", nvl(c.getDescription()));
        map.put("courseArea",  nvl(c.getCourseArea()));
        map.put("topic",       nvl(c.getTopic()));
        map.put("format",      nvl(c.getFormat()));
        map.put("date",        nvl(c.getDate()));
        map.put("time",        nvl(c.getTime()));
        map.put("platform",    nvl(c.getPlatform()));
        map.put("location",    c.getLocation());
        map.put("startsIn",    nvl(c.getStartsIn()));
        map.put("seatsLeft",   c.getSeatsLeft());
        map.put("accent",        nvl(c.getAccent()));
        map.put("sessions",      parseSessions(c.getSessionsJson()));
        map.put("aboutCourse",   nvl(c.getAboutCourse()));
        map.put("youWillLearn",  parseJsonList(c.getYouWillLearnJson()));
        map.put("curriculum",    parseJsonList(c.getCurriculumJson()));
        return map;
    }

    private String serializeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Object> parseSessions(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<Object> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list.isEmpty() ? null : list;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Object> parseJsonList(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}


