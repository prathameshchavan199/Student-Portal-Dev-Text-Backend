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
        map.put("imageUrl",    nvl(c.getImageUrl()));
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
