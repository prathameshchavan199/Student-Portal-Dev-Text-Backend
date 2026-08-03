package com.webapp.cognitodemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WritingEvaluationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> TECH_WORDS = Set.of(
        "algorithm","implementation","architecture","database","system","solution","framework","api",
        "server","client","deploy","optimize","performance","scalable","integration","component",
        "module","function","interface","protocol","network","security","authentication",
        "encryption","cache","query","endpoint","code","software","hardware","data","logic",
        "edge","computing","iot","latency","bandwidth","decentralized","processing","throughput",
        "microservice","docker","kubernetes","devops","runtime","pipeline","model","cloud",
        "testing","agile","feature","scalability","ecosystem","infrastructure","sensor",
        "gateway","node","cluster","distributed","concurrent","asynchronous","synchronous"
    );

    private static final List<String> TRANSITIONS = List.of(
        "however","therefore","furthermore","additionally","moreover","consequently",
        "in contrast","as a result","for example","in conclusion","on the other hand",
        "first","second","finally","in addition","nevertheless","thus","hence","specifically"
    );

    private static final List<List<String>> ARG_GROUPS = List.of(
        List.of("context","currently","today","background","introduction","overview","traditionally"),
        List.of("challenge","problem","issue","concern","limitation","drawback","constraint","difficulty"),
        List.of("solution","approach","method","technique","strategy","implementation","using","by applying"),
        List.of("result","benefit","advantage","improvement","enables","allows","achieves","outcome")
    );

    private static final List<String> EVIDENCE_MARKERS = List.of(
        "because","for example","for instance","such as","this means","this allows",
        "as a result","therefore","consequently","compared with","in practice"
    );

    private static final Pattern[] MECHANICS_PATTERNS = {
        Pattern.compile("\\bi\\b"),
        Pattern.compile("\\s+[,.!?;]"),
        Pattern.compile("[,.!?;:]{2,}"),
        Pattern.compile("\\b(the|a|an|and|or|but|to|of|in|on|for|with)\\s+\\1\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern CONCLUSION_PATTERN =
        Pattern.compile("\\b(in conclusion|to conclude|overall|finally|therefore|as a result)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern CAUSE_EFFECT_PATTERN =
        Pattern.compile("\\b(reduce|reduces|improve|improves|enable|enables|allow|allows|increase|decrease|optimize|optimise)\\b", Pattern.CASE_INSENSITIVE);

    public Map<String, Object> evaluate(String text, List<String> promptKeywords) {
        String clean = (text == null ? "" : text)
            .replaceAll("\\*\\*?|__?|\\[.*?]|\\(.*?\\)", "").trim();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US));

        String[] wordArr = clean.isEmpty() ? new String[0] : clean.split("\\s+");
        int wordCount = wordArr.length;

        if (wordCount < 5) {
            return noContentResult(date);
        }

        String lower = clean.toLowerCase();
        String[] sentences = Arrays.stream(clean.split("[.!?]+"))
            .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        int sentCount = Math.max(sentences.length, 1);
        double avgSentLen = (double) wordCount / sentCount;

        String[] paragraphs = Arrays.stream(text.split("\\n\\n+"))
            .filter(p -> !p.trim().isEmpty()).toArray(String[]::new);
        int paraCount = Math.max(paragraphs.length, 1);

        // Sentence length variance
        double[] sentLens = Arrays.stream(sentences)
            .mapToDouble(s -> s.split("\\s+").length).toArray();
        double meanLen = Arrays.stream(sentLens).average().orElse(0);
        double variance = Arrays.stream(sentLens).map(l -> Math.abs(l - meanLen)).average().orElse(0);

        // Normalized word list
        List<String> normWords = Arrays.stream(wordArr)
            .map(w -> w.toLowerCase().replaceAll("[^a-z]", ""))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        int techCount  = (int) normWords.stream().filter(TECH_WORDS::contains).count();
        int promptHits = (int) promptKeywords.stream().filter(lower::contains).count();
        int evidenceHits = (int) EVIDENCE_MARKERS.stream().filter(lower::contains).count();
        Set<String> uniqueNorm = new HashSet<>(normWords);
        double uniqueRatio = (double) uniqueNorm.size() / Math.max(normWords.size(), 1);
        int transCount = (int) TRANSITIONS.stream().filter(lower::contains).count();
        int argHits = (int) ARG_GROUPS.stream()
            .filter(g -> g.stream().anyMatch(lower::contains)).count();

        // Token frequency stats for lowMeaningResponse
        Map<String, Integer> tokenCounts = new HashMap<>();
        for (String w : normWords) tokenCounts.merge(w, 1, Integer::sum);
        int dominantCount = tokenCounts.values().stream().mapToInt(i -> i).max().orElse(0);
        double dominantTokenRatio = (double) dominantCount / Math.max(normWords.size(), 1);
        double repeatedTokenRatio = 1.0 - ((double) uniqueNorm.size() / Math.max(normWords.size(), 1));

        // Keyboard-mash detection: real text has vowels in the vast majority of words;
        // random key sequences (kjZSbcZMXCN, ZSJKdhZ etc.) produce consonant-only tokens
        long vowelWordCount = normWords.stream()
            .filter(w -> w.matches(".*[aeiouy].*")).count();
        double vowelWordRatio = (double) vowelWordCount / Math.max(normWords.size(), 1);

        // Mechanics issues
        int mechanicsIssueCount = 0;
        for (Pattern p : MECHANICS_PATTERNS) {
            Matcher m = p.matcher(clean);
            while (m.find()) mechanicsIssueCount++;
        }

        boolean hasConclusion  = CONCLUSION_PATTERN.matcher(clean).find();
        boolean hasCauseEffect = CAUSE_EFFECT_PATTERN.matcher(clean).find();

        // hasQuestionFocus: promptHits >= 4 AND first two topic keywords appear
        boolean hasQuestionFocus = promptHits >= 4 && promptKeywords.size() >= 2
            && lower.contains(promptKeywords.get(0))
            && lower.contains(promptKeywords.get(1));

        // Topic-specific technical bonuses using keyword positions (generalized)
        boolean techBonus1 = promptKeywords.size() >= 7
            ? (lower.contains(promptKeywords.get(5)) && lower.contains(promptKeywords.get(6)))
            : promptHits >= 5;
        boolean techBonus2 = promptKeywords.size() >= 12
            ? (lower.contains(promptKeywords.get(9)) || lower.contains(promptKeywords.get(10)) || lower.contains(promptKeywords.get(11)))
            : promptHits >= 3;

        // Relevance bonus: third topic keyword (generalizes the original "scalability" check)
        boolean relevanceBonus = promptKeywords.size() >= 4
            ? (lower.contains(promptKeywords.get(2)) || lower.contains(promptKeywords.get(3)))
            : (lower.contains("scalability") || lower.contains("scale"));

        boolean lowMeaningResponse = vowelWordRatio < 0.4            // keyboard-mash / no real words
            || (promptHits == 0
                && (uniqueRatio < 0.35 || dominantTokenRatio >= 0.3 || repeatedTokenRatio >= 0.65));

        // ── Scores ──────────────────────────────────────────────────────────────
        double clarity = 56;
        if (avgSentLen >= 10 && avgSentLen <= 24) clarity += 18;
        else if (avgSentLen < 7)  clarity -= 10;
        else if (avgSentLen > 32) clarity -= 14;
        if (sentCount >= 6)                   clarity += 8;
        if (variance >= 4 && variance <= 18)  clarity += 8;
        if (uniqueRatio >= 0.55)              clarity += 6;
        int clarityScore = clamp(25, 100, (int) Math.round(clarity));

        double technical = 42;
        technical += Math.min(techCount * 5.0, 35);
        technical += Math.min(promptHits * 3.0, 18);
        if (techBonus1)    technical += 8;
        if (techBonus2)    technical += 5;
        if (hasCauseEffect) technical += 5;
        int technicalScore = clamp(25, 100, (int) Math.round(technical));

        double relevance = 40;
        relevance += Math.min(promptHits * 5.0, 35);
        if (hasQuestionFocus)  relevance += 12;
        if (relevanceBonus)    relevance += 8;
        if (wordCount < 80)    relevance -= 10;
        int relevanceScore = clamp(25, 100, (int) Math.round(relevance));

        double evidence = 36;
        evidence += Math.min(evidenceHits * 8.0, 32);
        evidence += argHits * 6;
        if (hasCauseEffect)   evidence += 8;
        if (wordCount >= 150) evidence += 8;
        if (wordCount >= 220) evidence += 4;
        int evidenceScore = clamp(25, 100, (int) Math.round(evidence));

        double organization = 42;
        organization += argHits * 9;
        if (paraCount >= 2) organization += 8;
        if (paraCount >= 3) organization += 5;
        if (transCount >= 2) organization += 8;
        else if (transCount >= 1) organization += 4;
        if (hasConclusion) organization += 5;
        organization -= Math.min(mechanicsIssueCount * 4.0, 18);
        int organizationScore = clamp(25, 100, (int) Math.round(organization));

        if (lowMeaningResponse) {
            clarityScore      = 0;
            technicalScore    = 0;
            relevanceScore    = 0;
            evidenceScore     = 0;
            organizationScore = 0;
        }

        int score = (int) Math.round(
            clarityScore      * 0.20 +
            technicalScore    * 0.24 +
            relevanceScore    * 0.20 +
            evidenceScore     * 0.18 +
            organizationScore * 0.18
        );

        // Badge
        String badge, badgeDesc;
        if (score >= 82) {
            badge     = "ADVANCED TECHNICAL WRITER";
            badgeDesc = "Your response shows clear structure, strong technical accuracy, and well-supported explanation.";
        } else if (score >= 65) {
            badge     = "PROFICIENT WRITER";
            badgeDesc = "Your writing has a solid foundation. Keep improving evidence, flow, and technical precision.";
        } else {
            badge     = "FOUNDATIONAL WRITER";
            badgeDesc = "Focus on answering the prompt directly with organized points, technical vocabulary, and examples.";
        }

        // Strengths
        List<String> strengths = new ArrayList<>();
        if (promptHits >= 6)      strengths.add("The response stays closely aligned with the prompt's core topics and key technical concepts.");
        if (evidenceHits >= 2)    strengths.add("Good explanatory support using examples, cause-effect reasoning, or practical implications.");
        if (techCount >= 4)       strengths.add("Strong technical vocabulary — " + techCount + " domain-specific terms used effectively.");
        if (transCount >= 2)      strengths.add("Good use of transition phrases that improve readability and flow.");
        if (argHits >= 3)         strengths.add("Well-structured argument covering context, challenge, approach, and outcome.");
        if (avgSentLen >= 10 && avgSentLen <= 25) strengths.add("Clear and readable sentence length — well-balanced prose.");
        if (variance >= 4)        strengths.add("Good sentence variety — alternating lengths keep the reader engaged.");
        if (wordCount >= 200)     strengths.add("Comprehensive response at " + wordCount + " words — good depth of analysis.");
        if (uniqueRatio >= 0.65)  strengths.add("Rich vocabulary demonstrating broad domain knowledge.");
        if (strengths.isEmpty())  strengths.add("You completed the writing assessment — a strong first step in technical communication.");
        if (lowMeaningResponse)   strengths.clear();

        // Areas
        String keywordsHint = promptKeywords.size() >= 5
            ? String.join(", ", promptKeywords.subList(0, 5))
            : String.join(", ", promptKeywords);

        List<Map<String, String>> areas = new ArrayList<>();
        if (lowMeaningResponse) areas.add(area("Submit meaningful content", " - repeated or off-topic filler text cannot be evaluated as a valid writing response."));
        if (mechanicsIssueCount > 1) areas.add(area("Proofread mechanics", " - " + mechanicsIssueCount + " punctuation, capitalization, or repeated-word issue(s) detected."));
        if (promptHits < 4)    areas.add(area("Address the prompt more directly", " — cover the key concepts: " + keywordsHint + "."));
        if (evidenceHits < 2)  areas.add(area("Add evidence and explanation", " - use examples or cause-effect reasoning to support each claim."));
        if (techCount < 3)     areas.add(area("Include more technical terms", " — demonstrate domain-specific knowledge with precise vocabulary."));
        if (argHits < 2)       areas.add(area("Structure your argument", " — cover the context, challenge, your approach, and the outcome."));
        if (transCount < 2)    areas.add(area("Use transition phrases", " — words like \"however\", \"therefore\", \"as a result\" improve flow."));
        if (wordCount < 120)   areas.add(area("Expand your response", " — at " + wordCount + " words, add more depth (aim for 150–300)."));
        if (avgSentLen > 30)   areas.add(area("Break up long sentences", " — avg " + (int) Math.round(avgSentLen) + " words/sentence is hard to follow."));
        if (paraCount < 2)     areas.add(area("Organise into paragraphs", " — separate your introduction, main points, and conclusion."));
        if (areas.isEmpty())   areas.add(area("Keep practising", " — consistent technical writing builds confidence and clarity."));

        List<Map<String, Object>> skills = List.of(
            skill("CLARITY & READABILITY",     clarityScore),
            skill("TECHNICAL ACCURACY",        technicalScore),
            skill("PROMPT RELEVANCE",          relevanceScore),
            skill("EVIDENCE & EXPLANATION",    evidenceScore),
            skill("ORGANIZATION & MECHANICS",  organizationScore)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", score);
        result.put("badge", badge);
        result.put("badgeDesc", badgeDesc);
        result.put("skills", skills);
        result.put("strengths", strengths);
        result.put("areas", areas);
        result.put("wordCount", wordCount);
        result.put("date", date);
        return result;
    }

    public String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private Map<String, Object> noContentResult(String date) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("score", 0);
        r.put("badge", "NO CONTENT");
        r.put("badgeDesc", "No writing was submitted. Please write your response and try again.");
        r.put("skills", List.of(
            skill("CLARITY & READABILITY", 0),
            skill("TECHNICAL ACCURACY", 0),
            skill("PROMPT RELEVANCE", 0),
            skill("EVIDENCE & EXPLANATION", 0),
            skill("ORGANIZATION & MECHANICS", 0)
        ));
        r.put("strengths", List.of());
        r.put("areas", List.of(area("No content submitted", " — please write your response.")));
        r.put("wordCount", 0);
        r.put("date", date);
        return r;
    }

    private static int clamp(int min, int max, int val) {
        return Math.min(max, Math.max(min, val));
    }

    private static Map<String, Object> skill(String label, int pct) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("pct", pct);
        return m;
    }

    private static Map<String, String> area(String bold, String rest) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("bold", bold);
        m.put("rest", rest);
        return m;
    }
}
