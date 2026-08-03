package com.webapp.cognitodemo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.cognitodemo.entity.assessment.*;
import com.webapp.cognitodemo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommunicationService {

    @Autowired private AssessmentTopicRepo topicRepo;
    @Autowired private SpeakingAttemptRepo speakingRepo;
    @Autowired private WritingAttemptRepo writingRepo;
    @Autowired private SpeakingEvaluationService speakingEval;
    @Autowired private WritingEvaluationService writingEval;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private static final Set<String> STOP_WORDS = Set.of(
        "the","and","for","are","was","were","with","that","this","from","have","has","had",
        "you","your","they","their","there","what","when","where","who","which","how","why",
        "but","not","can","could","will","would","should","may","might","more","than","just",
        "also","both","each","all","very","some","most","only","once","any","own","its","even",
        "about","into","during","after","before","while","been","being","does","did","use",
        "using","used","make","good","best","walk","through","describe","discuss","explain",
        "time","much","ways","upon","ever","then","them","him","her","his","she","out","one",
        "two","our","too","new","old","many","few","long","get","let","put","say","said",
        "such","these","those","here","come","give","know","see","think","look","want","need"
    );

    // ── Topic fetching ─────────────────────────────────────────────────────────

    public Map<String, Object> getRandomTopic(String type) {
        List<AssessmentTopic> topics = topicRepo.findByTypeAndActiveTrue(type);
        if (topics.isEmpty()) throw new RuntimeException("No topics available for type: " + type);
        AssessmentTopic topic = topics.get(random.nextInt(topics.size()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", topic.getId());
        out.put("topicText", topic.getTopicText());
        out.put("challenge", topic.getChallenge());
        if ("writing".equals(type)) {
            out.put("keywords", parseKeywords(topic.getKeywordsJson()));
        }
        return out;
    }

    // ── Speaking submission ────────────────────────────────────────────────────

    public Map<String, Object> submitSpeaking(String email, String moduleId, SpeakingSubmitRequest req) {
        long count = speakingRepo.countByUserEmailAndModuleId(email, moduleId);
        if (count >= 3) throw new IllegalStateException("Maximum 3 attempts reached for this module.");

        AssessmentTopic topic = topicRepo.findById(req.getTopicId())
            .orElseThrow(() -> new RuntimeException("Topic not found: " + req.getTopicId()));

        List<String> topicKeywords = extractTopicKeywords(topic.getTopicText(), topic.getChallenge());
        Map<String, Object> eval = speakingEval.evaluate(req.getTranscript(), req.getElapsedSecs(), topicKeywords);

        int score = (Integer) eval.get("score");
        int attemptNumber = (int) count + 1;

        SpeakingAttempt attempt = SpeakingAttempt.builder()
            .userEmail(email)
            .moduleId(moduleId)
            .topicId(req.getTopicId())
            .topicText(topic.getTopicText())
            .transcript(req.getTranscript())
            .elapsedSecs(req.getElapsedSecs())
            .score(score)
            .attemptNumber(attemptNumber)
            .badge((String) eval.get("badge"))
            .skillsJson(speakingEval.toJson(eval.get("skills")))
            .strengthsJson(speakingEval.toJson(eval.get("strengths")))
            .areasJson(speakingEval.toJson(eval.get("areas")))
            .wordCount((Integer) eval.get("wordCount"))
            .wpm((Integer) eval.get("wpm"))
            .build();
        speakingRepo.save(attempt);

        Map<String, Object> response = new LinkedHashMap<>(eval);
        response.put("attemptNumber", attemptNumber);
        return response;
    }

    // ── Writing submission ─────────────────────────────────────────────────────

    public Map<String, Object> submitWriting(String email, String moduleId, WritingSubmitRequest req) {
        long count = writingRepo.countByUserEmailAndModuleId(email, moduleId);
        if (count >= 3) throw new IllegalStateException("Maximum 3 attempts reached for this module.");

        AssessmentTopic topic = topicRepo.findById(req.getTopicId())
            .orElseThrow(() -> new RuntimeException("Topic not found: " + req.getTopicId()));

        List<String> keywords = parseKeywords(topic.getKeywordsJson());
        Map<String, Object> eval = writingEval.evaluate(req.getText(), keywords);

        int score = (Integer) eval.get("score");
        int attemptNumber = (int) count + 1;

        WritingAttempt attempt = WritingAttempt.builder()
            .userEmail(email)
            .moduleId(moduleId)
            .topicId(req.getTopicId())
            .topicText(topic.getTopicText())
            .text(req.getText())
            .score(score)
            .attemptNumber(attemptNumber)
            .badge((String) eval.get("badge"))
            .skillsJson(writingEval.toJson(eval.get("skills")))
            .strengthsJson(writingEval.toJson(eval.get("strengths")))
            .areasJson(writingEval.toJson(eval.get("areas")))
            .wordCount((Integer) eval.get("wordCount"))
            .build();
        writingRepo.save(attempt);

        Map<String, Object> response = new LinkedHashMap<>(eval);
        response.put("attemptNumber", attemptNumber);
        return response;
    }

    // ── Seeder helper ──────────────────────────────────────────────────────────

    public void seedTopic(String type, String topicText, String challenge, List<String> keywords) {
        try {
            AssessmentTopic t = AssessmentTopic.builder()
                .type(type)
                .topicText(topicText)
                .challenge(challenge)
                .keywordsJson(keywords.isEmpty() ? null : objectMapper.writeValueAsString(keywords))
                .active(true)
                .displayOrder((int) topicRepo.count() + 1)
                .build();
            topicRepo.save(t);
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed topic: " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Extracts meaningful words from the topic text and challenge to use as
     * relevance keywords. Filters out stop words and very short words so
     * the evaluator can measure whether the speaker addressed the actual topic.
     */
    private List<String> extractTopicKeywords(String topicText, String challenge) {
        String combined = (topicText == null ? "" : topicText)
                + " " + (challenge == null ? "" : challenge);
        return Arrays.stream(combined.toLowerCase().split("[^a-z]+"))
            .filter(w -> w.length() >= 3 && !STOP_WORDS.contains(w))
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(keywordsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Arrays.asList(keywordsJson.split(","));
        }
    }
}
