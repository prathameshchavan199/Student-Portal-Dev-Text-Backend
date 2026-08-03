package com.webapp.cognitodemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Evaluates speaking test transcripts across 8 parameters (total 100 marks):
 *   Pronunciation(20), Fluency(15), Grammar(15), Vocabulary(10),
 *   Confidence(10), Speaking Pace(5), Topic Relevance(15), Content Quality(10)
 */
@Service
public class SpeakingEvaluationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> FILLERS = Set.of(
        "um","uh","like","basically","literally","actually","right","okay","so","well","hmm"
    );

    private static final Set<String> TECH_WORDS = Set.of(
        "algorithm","implementation","architecture","database","system","solution","framework","api",
        "server","client","deploy","debug","optimize","performance","scalable","integration","component",
        "module","function","variable","interface","protocol","network","security","authentication",
        "encryption","cache","query","endpoint","code","software","hardware","data","logic","error",
        "bug","feature","testing","pipeline","model","cloud","repository","sprint","agile","stack",
        "frontend","backend","microservice","docker","kubernetes","devops","runtime","latency","throughput"
    );

    private static final List<String> TRANSITIONS = List.of(
        "however","therefore","furthermore","additionally","moreover","consequently",
        "in contrast","as a result","for example","in conclusion","on the other hand"
    );

    private static final List<List<String>> STAR_GROUPS = List.of(
        List.of("situation","context","when","working on","during","at the time","we were"),
        List.of("task","challenge","problem","issue","needed to","had to","required","my role"),
        List.of("action","decided","implemented","developed","i used","i applied","i created","i fixed","approached","i wrote"),
        List.of("result","outcome","finally","eventually","achieved","resolved","improved","success","as a result","it worked")
    );

    /**
     * @param topicKeywords Words extracted from the topic + challenge text — used for Topic Relevance scoring.
     */
    public Map<String, Object> evaluate(String transcript, int elapsedSecs, List<String> topicKeywords) {
        String clean = transcript == null ? "" : transcript.trim();
        String date  = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US));
        String duration = fmtTime(elapsedSecs);

        String[] wordArr = clean.isEmpty() ? new String[0] : clean.split("\\s+");
        int wordCount = wordArr.length;

        if (wordCount < 5) return noSpeechResult(date, duration);

        String lower = clean.toLowerCase();

        String[] sentences = Arrays.stream(clean.split("[.!?]+"))
            .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        int sentCount  = Math.max(sentences.length, 1);
        double avgSentLen = (double) wordCount / sentCount;
        int wpm = elapsedSecs > 0 ? (int) Math.round((wordCount / (double) elapsedSecs) * 60) : 0;

        // ── Base metrics ─────────────────────────────────────────────────────────

        int fillerCount = 0;
        for (String w : wordArr) {
            if (FILLERS.contains(w.toLowerCase().replaceAll("[^a-z]", ""))) fillerCount++;
        }
        fillerCount += countOccurrences(lower, "you know");
        double fillerRatio = (double) fillerCount / wordCount;

        int repeatCount = 0;
        for (int i = 1; i < wordArr.length; i++) {
            if (wordArr[i].toLowerCase().equals(wordArr[i - 1].toLowerCase())) repeatCount++;
        }

        int techCount = 0;
        for (String w : wordArr) {
            if (TECH_WORDS.contains(w.toLowerCase().replaceAll("[^a-z]", ""))) techCount++;
        }

        Set<String> uniqueWords = new HashSet<>();
        for (String w : wordArr) uniqueWords.add(w.toLowerCase().replaceAll("[^a-z]", ""));
        double uniqueRatio = (double) uniqueWords.size() / wordCount;

        int transCount = (int) TRANSITIONS.stream().filter(lower::contains).count();

        int starHits = 0;
        for (List<String> group : STAR_GROUPS) {
            for (String kw : group) { if (lower.contains(kw)) { starHits++; break; } }
        }

        // How many topic keywords the user actually mentioned
        int topicKeywordHits = (int) topicKeywords.stream()
            .filter(kw -> kw.length() >= 3 && lower.contains(kw))
            .count();

        // ── Parameter scores ─────────────────────────────────────────────────────

        // 1. PRONUNCIATION (max 20) — proxy: filler ratio + repetitions + sentence clarity
        double pronunciation = 12;
        if      (fillerRatio < 0.02) pronunciation += 5;
        else if (fillerRatio < 0.05) pronunciation += 3;
        else if (fillerRatio < 0.10) pronunciation += 1;
        else if (fillerRatio > 0.15) pronunciation -= 4;
        if (repeatCount == 0)        pronunciation += 2;
        else if (repeatCount > 3)    pronunciation -= 2;
        if (avgSentLen >= 8 && avgSentLen <= 22) pronunciation += 1;
        int pPronunciation = clamp(0, 20, (int) Math.round(pronunciation));

        // 2. FLUENCY (max 15) — proxy: filler density + hesitation repeats
        double fluency = 7;
        if      (fillerRatio < 0.02) fluency += 5;
        else if (fillerRatio < 0.05) fluency += 3;
        else if (fillerRatio < 0.10) fluency += 1;
        else if (fillerRatio > 0.12) fluency -= 3;
        if (repeatCount == 0)        fluency += 2;
        else if (repeatCount > 2)    fluency -= 2;
        if (sentCount >= 4)          fluency += 1;
        int pFluency = clamp(0, 15, (int) Math.round(fluency));

        // 3. GRAMMAR (max 15) — proxy: sentence length naturalness + structure
        double grammar = 7;
        if (avgSentLen >= 8 && avgSentLen <= 22)    grammar += 4;
        else if (avgSentLen < 5 || avgSentLen > 32) grammar -= 4;
        if (sentCount >= 4)    grammar += 2;
        if (repeatCount == 0)  grammar += 2;
        else if (repeatCount > 2) grammar -= 2;
        int pGrammar = clamp(0, 15, (int) Math.round(grammar));

        // 4. VOCABULARY (max 10) — domain terms + richness
        double vocabulary = 2;
        vocabulary += Math.min(techCount * 1.5, 6);
        if (uniqueRatio >= 0.55) vocabulary += 1;
        if (uniqueRatio >= 0.65) vocabulary += 1;
        int pVocabulary = clamp(0, 10, (int) Math.round(vocabulary));

        // 5. CONFIDENCE (max 10) — appropriate pace + sufficient length
        double confidence = 3;
        if      (wpm >= 110 && wpm <= 160) confidence += 4;
        else if (wpm >= 90  && wpm <= 175) confidence += 2;
        else if (wpm > 0    && wpm < 80)   confidence -= 1;
        if (wordCount >= 60)  confidence += 2;
        if (wordCount >= 100) confidence += 1;
        int pConfidence = clamp(0, 10, (int) Math.round(confidence));

        // 6. SPEAKING PACE (max 5) — proximity to ideal 110-160 wpm
        int pPace;
        if      (wpm >= 110 && wpm <= 160) pPace = 5;
        else if (wpm >= 90  && wpm <= 175) pPace = 3;
        else if (wpm >= 70  && wpm <= 190) pPace = 2;
        else if (wpm > 0)                  pPace = 1;
        else                               pPace = 0;

        // 7. TOPIC RELEVANCE (max 15)
        //    Primary: topic keyword hits (from actual question/challenge text)
        //    Secondary: STAR method structure + discourse markers
        double topicRelevance = 2;
        topicRelevance += Math.min(topicKeywordHits * 1.5, 9); // keyword coverage: max 9
        topicRelevance += starHits >= 3 ? 3 : starHits >= 2 ? 2 : starHits >= 1 ? 1 : 0; // STAR bonus: max 3
        topicRelevance += transCount >= 1 ? 1 : 0;             // organised answer: max 1
        int pTopicRelevance = clamp(0, 15, (int) Math.round(topicRelevance));

        // 8. CONTENT QUALITY (max 10) — depth, length, examples, organisation
        double contentQuality = 2;
        if (wordCount >= 40)  contentQuality += 1;
        if (wordCount >= 60)  contentQuality += 1;
        if (wordCount >= 100) contentQuality += 1;
        if (wordCount >= 150) contentQuality += 1;
        if (techCount >= 2)   contentQuality += 1;
        if (techCount >= 4)   contentQuality += 1;
        if (sentCount >= 4)   contentQuality += 1;
        if (transCount >= 1)  contentQuality += 1;
        int pContentQuality = clamp(0, 10, (int) Math.round(contentQuality));

        int score = pPronunciation + pFluency + pGrammar + pVocabulary
                  + pConfidence    + pPace    + pTopicRelevance + pContentQuality;

        // Off-topic penalty: relevance below 8/15 deducts (8 − score) points from total
        if (pTopicRelevance < 8) {
            score = Math.max(0, score - (8 - pTopicRelevance));
        }

        // Badge
        String badge, badgeDesc;
        if (score >= 82) {
            badge = "FLUENT ENGINEER";
            badgeDesc = "You demonstrate strong technical command and confident articulation.";
        } else if (score >= 65) {
            badge = "DEVELOPING SPEAKER";
            badgeDesc = "A solid foundation — keep refining structure and vocabulary.";
        } else {
            badge = "EMERGING VOICE";
            badgeDesc = "Great start! Focus on structure, pace, and reducing filler words.";
        }

        // Strengths
        List<String> strengths = new ArrayList<>();
        if (pPronunciation >= 16) strengths.add("Excellent pronunciation and articulation — very clear delivery.");
        if (pFluency >= 12)       strengths.add("Smooth and fluent speech with minimal hesitation or filler words.");
        if (pGrammar >= 12)       strengths.add("Strong grammatical structure — well-formed sentences throughout.");
        if (pVocabulary >= 7)     strengths.add("Good vocabulary range — " + techCount + " technical terms used with precision.");
        if (pConfidence >= 8)     strengths.add("Confident and natural delivery with good speaking presence.");
        if (pPace >= 4)           strengths.add("Well-paced delivery at " + wpm + " wpm — easy to follow.");
        if (pTopicRelevance >= 11) strengths.add("Answer stays highly relevant to the question, covering the key topic concepts clearly.");
        if (pContentQuality >= 7) strengths.add("Comprehensive answer with good depth and appropriate use of examples.");
        if (strengths.isEmpty())  strengths.add("You completed the full assessment — a great first step toward fluency.");

        // Areas
        String keywordsHint = topicKeywords.isEmpty() ? ""
            : " — address concepts like: " + String.join(", ", topicKeywords.subList(0, Math.min(4, topicKeywords.size())));

        List<Map<String, String>> areas = new ArrayList<>();
        if (pPronunciation < 13) areas.add(area("Improve articulation", " — focus on clarity and reducing filler words like um, uh, hmm."));
        if (pFluency < 9)        areas.add(area("Reduce hesitation", " — " + fillerCount + " filler word(s) detected; aim for smoother delivery."));
        if (pGrammar < 9)        areas.add(area("Work on sentence structure", " — aim for complete, well-formed sentences of 10–20 words."));
        if (pVocabulary < 5)     areas.add(area("Expand your vocabulary", " — use domain-specific technical terms to demonstrate expertise."));
        if (pConfidence < 6) {
            if (wpm > 0 && wpm < 80)  areas.add(area("Speak with more confidence", " — at " + wpm + " wpm the pace is too slow; aim for 110–160 wpm."));
            else if (wpm > 175)       areas.add(area("Slow down slightly", " — at " + wpm + " wpm the delivery is too fast; aim for 110–160 wpm."));
            else                      areas.add(area("Build speaking confidence", " — practise delivering responses with a steady, natural pace."));
        }
        if (pPace < 3) {
            if (wpm > 0 && wpm < 90) areas.add(area("Increase your speaking pace", " — at " + wpm + " wpm, delivery is slow (ideal: 110–160 wpm)."));
            else if (wpm > 175)      areas.add(area("Slow down your speaking pace", " — at " + wpm + " wpm it is hard to follow (ideal: 110–160 wpm)."));
        }
        if (pTopicRelevance < 9)  areas.add(area("Answer the question more directly",
            topicKeywords.isEmpty()
                ? " — use the STAR method: Situation, Task, Action, Result."
                : keywordsHint + " and structure your answer using the STAR method."));
        if (pContentQuality < 5)  areas.add(area("Add more depth and examples", " — elaborate on your points and include specific examples."));
        if (areas.isEmpty())      areas.add(area("Keep practising", " — consistency is the key to becoming a confident communicator."));

        List<Map<String, Object>> skills = List.of(
            skill("PRONUNCIATION",    pPronunciation, 20),
            skill("FLUENCY",          pFluency,       15),
            skill("GRAMMAR",          pGrammar,       15),
            skill("VOCABULARY",       pVocabulary,    10),
            skill("CONFIDENCE",       pConfidence,    10),
            skill("SPEAKING PACE",    pPace,           5),
            skill("TOPIC RELEVANCE",  pTopicRelevance, 15),
            skill("CONTENT QUALITY",  pContentQuality, 10)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score",     score);
        result.put("badge",     badge);
        result.put("badgeDesc", badgeDesc);
        result.put("skills",    skills);
        result.put("strengths", strengths);
        result.put("areas",     areas);
        result.put("wordCount", wordCount);
        result.put("wpm",       wpm);
        result.put("duration",  duration);
        result.put("date",      date);
        return result;
    }

    public String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private Map<String, Object> noSpeechResult(String date, String duration) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("score", 0);
        r.put("badge", "NO SPEECH DETECTED");
        r.put("badgeDesc", "No speech was captured. Check your microphone and try again.");
        r.put("skills", List.of(
            skill("PRONUNCIATION",   0, 20),
            skill("FLUENCY",         0, 15),
            skill("GRAMMAR",         0, 15),
            skill("VOCABULARY",      0, 10),
            skill("CONFIDENCE",      0, 10),
            skill("SPEAKING PACE",   0,  5),
            skill("TOPIC RELEVANCE", 0, 15),
            skill("CONTENT QUALITY", 0, 10)
        ));
        r.put("strengths", List.of());
        r.put("areas", List.of(area("No speech detected", " — please allow microphone access and speak clearly.")));
        r.put("wordCount", 0);
        r.put("wpm",       0);
        r.put("duration",  duration);
        r.put("date",      date);
        return r;
    }

    private static String fmtTime(int s) {
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private static int clamp(int min, int max, int val) {
        return Math.min(max, Math.max(min, val));
    }

    private static int countOccurrences(String text, String sub) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) { count++; idx += sub.length(); }
        return count;
    }

    private static Map<String, Object> skill(String label, int raw, int max) {
        int pct = max > 0 ? (int) Math.round((raw * 100.0) / max) : 0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("pct",   pct);
        m.put("raw",   raw);
        m.put("max",   max);
        return m;
    }

    private static Map<String, String> area(String bold, String rest) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("bold", bold);
        m.put("rest", rest);
        return m;
    }
}
