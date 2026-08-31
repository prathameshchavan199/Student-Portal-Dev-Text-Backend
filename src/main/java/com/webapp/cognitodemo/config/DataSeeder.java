package com.webapp.cognitodemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.cognitodemo.entity.User;
import com.webapp.cognitodemo.entity.assessment.*;
import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.entity.course.CourseReview;
import com.webapp.cognitodemo.repo.*;
import com.webapp.cognitodemo.service.AssessmentService;
import com.webapp.cognitodemo.service.CommunicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataSeeder implements ApplicationRunner {

    @Autowired private CourseRepo courseRepo;
    @Autowired private CourseReviewRepo courseReviewRepo;
    @Autowired private AssessmentCategoryRepo categoryRepo;
    @Autowired private AssessmentModuleRepo moduleRepo;
    @Autowired private AssessmentService assessmentService;
    @Autowired private CommunicationService communicationService;
    @Autowired private AssessmentTopicRepo topicRepo;
    @Autowired private UserRepo userRepo;

    @Override
    public void run(ApplicationArguments args) {
        seedCourses();
        seedAiFundamentalsCourse();
        seedReviews();
        seedAssessments();
        seedTopics();
        seedTpoAdminCollege();
    }

    // ── Courses ───────────────────────────────────────────────────────────────

    private void seedCourses() {
        if (courseRepo.count() > 0) return;

        courseRepo.saveAll(List.of(

                Course.builder()
                        .id("cloud-computing-masterclass")
                        .title("Cloud Computing Masterclass")
                        .category("onlineProgram")
                        .price(1299)
                        .duration("12 Weeks")
                        .level("Advanced")
                        .imageUrl("https://cdn.cyfenix.com/course-images/cloud.jpg")
                        .instructor("Alex Rivera")
                        .description("Master enterprise architecture, serverless deployments, and hybrid cloud strategies using the Azure stack and innovation framework.")
                        .courseArea("Development")
                        .topic("Cloud")
                        .format("Live Online")
                        .date("June 15 - June 17")
                        .time("10:00 AM - 01:00 PM")
                        .platform("Zoom Platform")
                        .startsIn("Starts in 2 days")
                        .seatsLeft(8)
                        .accent("blue")
                        .sessionsJson("""
                                [
                                  {"id":"summer-intake","title":"Session 1: Summer Intake","date":"June 15 - June 17, 2024","time":"09:00 - 17:00 EST"},
                                  {"id":"late-summer","title":"Session 2: Late Summer Intake","date":"July 20 - July 22, 2024","time":"09:00 - 17:00 EST"}
                                ]""")
                        .build(),

                Course.builder()
                        .id("cybersecurity-essentials")
                        .title("Cybersecurity Essentials")
                        .category("onlineProgram")
                        .price(899)
                        .duration("8 Weeks")
                        .level("Intermediate")
                        .imageUrl("https://cdn.cyfenix.com/course-images/cybersecurity.jpg")
                        .instructor("Sarah Chen")
                        .description("Build a practical security foundation across network defense, identity, risk reviews, and incident response.")
                        .courseArea("Business")
                        .topic("Security")
                        .format("Live Online")
                        .date("June 20 - June 22")
                        .time("02:00 PM - 05:00 PM")
                        .platform("Zoom Platform")
                        .startsIn("Starts in 5 days")
                        .seatsLeft(12)
                        .accent("purple")
                        .build(),

                Course.builder()
                        .id("react-essentials")
                        .title("React Essentials")
                        .category("onDemand")
                        .price(299)
                        .duration("6 Hours")
                        .level("Beginner")
                        .imageUrl("https://cdn.cyfenix.com/course-images/react.jpg")
                        .instructor("Maya Iyer")
                        .description("Build modern UIs with components, hooks, state management, and practical patterns you can reuse.")
                        .courseArea("Development")
                        .topic("Frontend")
                        .format("Self Paced")
                        .date("Available Now")
                        .time("Anytime")
                        .platform("Learning Portal")
                        .startsIn("Instant access")
                        .accent("orange")
                        .build(),

                Course.builder()
                        .id("advanced-react-patterns")
                        .title("Advanced React Patterns")
                        .category("onDemand")
                        .price(399)
                        .duration("9 Hours")
                        .level("Advanced")
                        .imageUrl("https://cdn.cyfenix.com/course-images/react-advanced.jpg")
                        .instructor("Noah Brooks")
                        .description("Level up reusable component design, performance work, data flows, and real production ergonomics.")
                        .courseArea("Development")
                        .topic("Frontend")
                        .format("Self Paced")
                        .date("Available Now")
                        .time("Anytime")
                        .platform("Learning Portal")
                        .startsIn("Instant access")
                        .accent("blue")
                        .build(),

                Course.builder()
                        .id("data-science-bootcamp")
                        .title("Data Science Bootcamp")
                        .category("offlineProgram")
                        .price(1499)
                        .duration("2 Weeks")
                        .level("Intermediate")
                        .imageUrl("https://cdn.cyfenix.com/course-images/data-science.jpg")
                        .instructor("Priya Mehta")
                        .description("In-person bootcamp covering Python, machine learning, analytics workflows, and real-world projects.")
                        .courseArea("Marketing")
                        .topic("Data")
                        .format("In Person")
                        .date("July 05 - July 19")
                        .time("10:00 AM - 04:00 PM")
                        .platform("Bengaluru Campus")
                        .location("Bengaluru")
                        .startsIn("Starts in 12 days")
                        .seatsLeft(6)
                        .accent("green")
                        .build(),

                Course.builder()
                        .id("fullstack-lab")
                        .title("Fullstack Lab")
                        .category("offlineProgram")
                        .price(1199)
                        .duration("9 Days")
                        .level("Beginner")
                        .imageUrl("https://cdn.cyfenix.com/course-images/fullstack.jpg")
                        .instructor("Chris Morgan")
                        .description("Build and ship a complete web product with backend APIs, database models, testing, and deployment.")
                        .courseArea("Development")
                        .topic("Fullstack")
                        .format("In Person")
                        .date("August 08 - August 16")
                        .time("09:30 AM - 03:30 PM")
                        .platform("Hyderabad Campus")
                        .location("Hyderabad")
                        .startsIn("Starts in 18 days")
                        .seatsLeft(10)
                        .accent("orange")
                        .build()
        ));

        System.out.println("[DataSeeder] Seeded 6 courses.");
    }

    /*
     * Seeds the "AI Fundamentals" on-demand course with a full Udemy-style
     * curriculum (modules + lessons) and a working video on Module 1 /
     * Lesson 1. Runs independently of seedCourses() so it inserts even
     * against a database that already has courses (idempotent — skips if
     * the course already exists).
     */
    private void seedAiFundamentalsCourse() {
        if (courseRepo.existsById("ai-fundamentals")) return;

        courseRepo.save(
                Course.builder()
                        .id("ai-fundamentals")
                        .title("AI Fundamentals")
                        .category("onDemand")
                        .price(499)
                        .duration("6 Hours")
                        .level("Beginner")
                        .imageUrl("https://cdn.cyfenix.com/course-images/ai-fundamentals.jpg")
                        .instructor("Dr. Ananya Rao")
                        .description("A hands-on introduction to artificial intelligence — how machines learn, the tools of the trade, and where AI is headed next.")
                        .courseArea("AI/ML")
                        .topic("AI/ML")
                        .format("Self Paced")
                        .date("Available Now")
                        .time("Anytime")
                        .platform("Learning Portal")
                        .startsIn("Instant access")
                        .seatsLeft(null)
                        .accent("purple")
                        .aboutCourse("AI Fundamentals walks you through the core ideas behind modern artificial intelligence — from what a model actually \"learns\" to how tools like ChatGPT and image generators work under the hood. Every module pairs a short video lesson with practical examples so you leave with real intuition, not just definitions.")
                        .youWillLearnJson(toJson(List.of(
                                "What AI, machine learning, and deep learning actually mean — and how they differ",
                                "How a model learns from data, step by step",
                                "The building blocks of neural networks",
                                "How large language models like GPT generate text",
                                "Practical, ethical, and business considerations for using AI"
                        )))
                        .curriculumJson(toJson(List.of(
                                module("Module 1: Introduction to AI", List.of(
                                        lesson("What is Artificial Intelligence?", "8:24", null, true),
                                        lesson("A Brief History of AI", "6:10", null, false),
                                        lesson("AI vs Machine Learning vs Deep Learning", "7:45", null, false)
                                )),
                                module("Module 2: How Machines Learn", List.of(
                                        lesson("Supervised vs Unsupervised Learning", "9:12", null, false),
                                        lesson("Training, Testing, and Validation Data", "6:30", null, false),
                                        lesson("Overfitting and Underfitting Explained", "5:55", null, false)
                                )),
                                module("Module 3: Neural Networks & Deep Learning", List.of(
                                        lesson("Anatomy of a Neural Network", "10:05", null, false),
                                        lesson("Activation Functions and Backpropagation", "8:40", null, false),
                                        lesson("Convolutional vs Recurrent Networks", "7:20", null, false)
                                )),
                                module("Module 4: Large Language Models", List.of(
                                        lesson("How GPT-style Models Generate Text", "9:50", null, false),
                                        lesson("Prompting and Fine-tuning Basics", "7:15", null, false)
                                )),
                                module("Module 5: AI in the Real World", List.of(
                                        lesson("Everyday Applications of AI", "6:45", null, false),
                                        lesson("Ethics, Bias, and Responsible AI", "8:05", null, false),
                                        lesson("Where AI is Headed Next", "5:30", null, false)
                                ))
                        )))
                        .build()
        );

        System.out.println("[DataSeeder] Seeded 'ai-fundamentals' course with video curriculum.");
    }

    private static final ObjectMapper SEED_MAPPER = new ObjectMapper();

    private String toJson(Object value) {
        try {
            return SEED_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize seed JSON", e);
        }
    }

    private Map<String, Object> module(String title, List<Map<String, Object>> lessons) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("lessons", lessons);
        return m;
    }

    /*
     * A curriculum lesson. When videoUrl is null the lesson has no playable
     * video yet (shown as "Coming soon" in the player); isPreview marks a
     * lesson as watchable without purchasing (not currently used by the UI
     * but reserved for a future free-preview feature).
     */
    private Map<String, Object> lesson(String title, String duration, String videoUrl, boolean isPreview) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("duration", duration);
        if (videoUrl != null) m.put("videoUrl", videoUrl);
        m.put("isPreview", isPreview);
        return m;
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    private void seedReviews() {
        if (courseReviewRepo.count() > 0) return;

        courseReviewRepo.saveAll(List.of(
                review("cloud-computing-masterclass", "Aman Verma",   5, "The sessions were clear, practical, and easy to follow. The Azure labs gave me real hands-on experience I could apply immediately at work."),
                review("cloud-computing-masterclass", "Priya Nair",   4, "Great balance of theory and hands-on practice. The instructor explained serverless concepts in a very approachable way."),
                review("cloud-computing-masterclass", "Rahul Mehta",  5, "Loved the structure of the course. The curriculum felt focused and the final capstone project was directly useful for my portfolio."),
                review("cloud-computing-masterclass", "Sneha Kapoor", 4, "Excellent course for anyone looking to transition into cloud roles. The hybrid cloud module was especially insightful."),

                review("cybersecurity-essentials", "Dev Sharma",     5, "Extremely practical coverage of network defense and identity management. The incident response simulations were the highlight."),
                review("cybersecurity-essentials", "Anita Rajan",    4, "Good depth on risk reviews and compliance frameworks. Would love more advanced exploit analysis in future modules."),
                review("cybersecurity-essentials", "Karan Patel",    5, "The instructor made complex security concepts digestible. Completed the course feeling genuinely prepared for real-world threats."),

                review("react-essentials", "Meera Joshi",    5, "Perfect for beginners. The hooks and state management sections clicked immediately and I was building my own projects by week 2."),
                review("react-essentials", "Arjun Nair",     4, "Very well structured. The component patterns taught here are clean and production-ready. Would recommend to anyone starting with React."),
                review("react-essentials", "Tanvi Desai",    5, "Loved the progressive difficulty. Each module built naturally on the last, making the learning curve feel smooth rather than steep."),

                review("advanced-react-patterns", "Vikram Iyer",   5, "This course elevated my React skills significantly. The render props and custom hook patterns are used daily in my work now."),
                review("advanced-react-patterns", "Pooja Bhat",    4, "Deep dives into performance optimization were excellent. The memoization strategies saved us real seconds in our production app."),

                review("data-science-bootcamp", "Suresh Menon",  5, "Two intense weeks that changed my career trajectory. The ML project we built in-person is now live in our company's analytics dashboard."),
                review("data-science-bootcamp", "Ritu Gupta",    4, "The Bengaluru campus facilities were great and the instructor was highly knowledgeable. Loved the team-based project format."),

                review("fullstack-lab", "Nikhil Rao",    5, "Nine days of building a real product end-to-end. Backend, database, frontend, and deployment — all covered with professional-grade guidance."),
                review("fullstack-lab", "Divya Singh",   4, "Challenging but immensely rewarding. The Hyderabad campus environment and instructor mentorship made the hard days feel worthwhile.")
        ));

        System.out.println("[DataSeeder] Seeded course reviews.");
    }

    private CourseReview review(String courseId, String reviewer, int rating, String text) {
        return CourseReview.builder()
                .courseId(courseId)
                .reviewerName(reviewer)
                .rating(rating)
                .reviewText(text)
                .build();
    }

    // ── Assessments ───────────────────────────────────────────────────────────

    private void seedAssessments() {
        if (categoryRepo.count() > 0) return;

        // ── Technical Skills ──────────────────────────────────────────────────
        categoryRepo.save(AssessmentCategory.builder()
                .id("technical-skills")
                .title("Technical Skills")
                .topbarLabel("Technical Skills")
                .heading("Technical Proficiency")
                .accentWord("Modules")
                .subtitle("Select a technical module to begin your skills assessment. Each session is timed and contributes to your overall competency score.")
                .badge("Code Wizard")
                .tip("Practice Data Structures and Algorithms regularly")
                .tag("Coding")
                .icon("FiCode")
                .testTitle("Technical Skills Assessment")
                .displayOrder(1)
                .build());

        moduleRepo.saveAll(List.of(
                mod("data-structures",  "technical-skills", "Data Structures",            "ADVANCED",     "FiLayers",   "mcq", "Evaluate mastery of complex data types, sorting algorithms, and space-time complexity analysis.",       "60 mins", 35, 1),
                mod("web-development",  "technical-skills", "Web Development",            "INTERMEDIATE", "FiCode",     "mcq", "Assess front-end frameworks, responsive design, and modern backend integration patterns.",             "45 mins", 25, 2),
                mod("algorithms",       "technical-skills", "Algorithms & Complexity",    "ADVANCED",     "FiCpu",      "mcq", "Test knowledge of algorithm design, Big-O analysis, recursion, dynamic programming, and optimization techniques.", "75 mins", 40, 3),
                mod("databases",        "technical-skills", "Database & SQL",             "INTERMEDIATE", "FiDatabase", "mcq", "Measure proficiency in relational databases, query optimization, indexing strategies, and normalization.", "50 mins", 30, 4),
                mod("oop",              "technical-skills", "Object-Oriented Programming","INTERMEDIATE", "FiPackage",  "mcq", "Validate understanding of OOP principles, design patterns, inheritance, encapsulation, and polymorphism.", "45 mins", 28, 5),
                mod("system-design",    "technical-skills", "System Design",              "ADVANCED",     "FiServer",   "mcq", "Demonstrate ability to architect scalable distributed systems, REST APIs, and infrastructure components.", "90 mins", 20, 6)
        ));

        McqSection ts1 = assessmentService.seedSection("technical-skills", "Core Concepts",       1);
        McqSection ts2 = assessmentService.seedSection("technical-skills", "Applied Programming",  2);
        McqSection ts3 = assessmentService.seedSection("technical-skills", "System Design",        3);

        assessmentService.seedQuestion(ts1.getId(), "What is the average time complexity of searching for an element in a Hash Table using a well-distributed hash function?", "O(1)", "O(log n)", "O(n)", "O(n log n)", 0, 1);
        assessmentService.seedQuestion(ts1.getId(), "Which data structure uses LIFO (Last In, First Out) ordering?", "Queue", "Stack", "Linked List", "Binary Tree", 1, 2);
        assessmentService.seedQuestion(ts1.getId(), "What is the worst-case time complexity of QuickSort?", "O(n)", "O(n log n)", "O(n²)", "O(log n)", 2, 3);
        assessmentService.seedQuestion(ts1.getId(), "In a Binary Search Tree, which traversal produces a sorted sequence of values?", "Pre-order", "Post-order", "In-order", "Level-order", 2, 4);

        assessmentService.seedQuestion(ts2.getId(), "Which HTTP method is idempotent and should be used to fully replace a resource?", "POST", "PATCH", "PUT", "DELETE", 2, 1);
        assessmentService.seedQuestion(ts2.getId(), "Which OOP principle restricts direct access to an object's internal state?", "Inheritance", "Polymorphism", "Abstraction", "Encapsulation", 3, 2);
        assessmentService.seedQuestion(ts2.getId(), "What design pattern ensures a class has only one instance throughout an application lifecycle?", "Factory", "Observer", "Singleton", "Decorator", 2, 3);
        assessmentService.seedQuestion(ts2.getId(), "Which collision resolution technique uses a linked list to store multiple elements that hash to the same index?", "Linear Probing", "Separate Chaining", "Double Hashing", "Quadratic Probing", 1, 4);

        assessmentService.seedQuestion(ts3.getId(), "Which architectural pattern separates an application into three interconnected components: Model, View, and Controller?", "Microservices", "MVC", "Event-driven", "Serverless", 1, 1);
        assessmentService.seedQuestion(ts3.getId(), "What is the primary purpose of a load balancer in a distributed system?", "Data caching", "Distributing traffic across servers", "Database replication", "Service discovery", 1, 2);

        // ── Problem Solving ───────────────────────────────────────────────────
        categoryRepo.save(AssessmentCategory.builder()
                .id("problem-solving")
                .title("Problem Solving")
                .topbarLabel("Problem Solving")
                .heading("Aptitude &")
                .accentWord("Reasoning")
                .subtitle("Select a reasoning module to evaluate your logical thinking, numerical ability, and decision-making under timed conditions.")
                .badge("Logic Master")
                .tip("Practice mental math and pattern recognition daily")
                .tag("Aptitude")
                .icon("FiEdit3")
                .testTitle("Problem Solving Assessment")
                .displayOrder(2)
                .build());

        moduleRepo.saveAll(List.of(
                mod("logical-reasoning",   "problem-solving", "Logical Reasoning",   "INTERMEDIATE", "FiTarget",   "mcq", "Evaluate deductive and inductive reasoning skills through structured argument and syllogism problems.", "40 mins", 30, 1),
                mod("numerical-aptitude",  "problem-solving", "Numerical Aptitude",  "INTERMEDIATE", "FiBarChart2","mcq", "Test speed and accuracy in arithmetic, percentages, ratios, and data sufficiency problems.", "35 mins", 25, 2),
                mod("pattern-recognition", "problem-solving", "Pattern Recognition", "ADVANCED",     "FiGrid",     "mcq", "Identify visual and numerical sequences, analogies, and abstract reasoning patterns within time limits.", "30 mins", 20, 3),
                mod("verbal-reasoning",    "problem-solving", "Verbal Reasoning",    "INTERMEDIATE", "FiFileText", "mcq", "Assess reading comprehension, critical analysis of passages, and inferential thinking from written content.", "40 mins", 28, 4),
                mod("critical-thinking",   "problem-solving", "Critical Thinking",   "ADVANCED",     "FiFilter",   "mcq", "Measure ability to evaluate arguments, identify assumptions, and draw logical conclusions from complex data.", "45 mins", 22, 5),
                mod("decision-making",     "problem-solving", "Decision Making",     "ADVANCED",     "FiZap",      "mcq", "Evaluate judgment under uncertainty, trade-off analysis, and structured problem resolution strategies.", "35 mins", 18, 6)
        ));

        McqSection ps1 = assessmentService.seedSection("problem-solving", "Logical Reasoning",  1);
        McqSection ps2 = assessmentService.seedSection("problem-solving", "Numerical & Verbal", 2);
        McqSection ps3 = assessmentService.seedSection("problem-solving", "Decision Making",    3);

        assessmentService.seedQuestion(ps1.getId(), "If all roses are flowers and some flowers are red, which conclusion is definitely true?", "All roses are red", "Some roses may be red", "No roses are red", "All flowers are roses", 1, 1);
        assessmentService.seedQuestion(ps1.getId(), "Complete the series: 2, 6, 18, 54, ___", "108", "162", "216", "180", 1, 2);
        assessmentService.seedQuestion(ps1.getId(), "A train travels at 60 km/h for 2 hours then at 80 km/h for 3 hours. What is the total distance covered?", "280 km", "360 km", "300 km", "420 km", 1, 3);
        assessmentService.seedQuestion(ps1.getId(), "In a row of students, Ram is 7th from the left and 13th from the right. How many students are in the row?", "18", "19", "20", "21", 1, 4);

        assessmentService.seedQuestion(ps2.getId(), "A shopkeeper marks goods 40% above cost price and gives a 20% discount. What is the profit percentage?", "10%", "12%", "15%", "8%", 1, 1);
        assessmentService.seedQuestion(ps2.getId(), "Choose the word most similar in meaning to \"ELOQUENT\":", "Silent", "Articulate", "Confused", "Timid", 1, 2);
        assessmentService.seedQuestion(ps2.getId(), "Water : Thirst :: Food : ___", "Drink", "Cook", "Hunger", "Nutrition", 2, 3);
        assessmentService.seedQuestion(ps2.getId(), "What fraction of 2 hours is 20 minutes?", "1/4", "1/6", "1/5", "1/3", 1, 4);

        assessmentService.seedQuestion(ps3.getId(), "A project is 80% complete with 10 days left but requires 15 more working days to finish. What is the best course of action?", "Continue as planned", "Request a deadline extension", "Reduce quality standards", "Cancel the project", 1, 1);
        assessmentService.seedQuestion(ps3.getId(), "Which cognitive bias causes people to favor information that confirms their pre-existing beliefs?", "Anchoring bias", "Sunk cost fallacy", "Confirmation bias", "Availability heuristic", 2, 2);

        // ── Communication ──────────────────────────────────────────────────────
        categoryRepo.save(AssessmentCategory.builder()
                .id("communication")
                .title("Communication")
                .topbarLabel("Communication")
                .heading("Soft Skill Module")
                .accentWord("")
                .subtitle("Refine your professional interaction capabilities. Select a specialised module to evaluate and improve your tech-centric communication proficiency.")
                .badge("Eloquent Engineer")
                .tip("Speak on topics for 2 minutes daily to build fluency")
                .tag("Communication")
                .icon("FiShield")
                .testTitle(null)
                .displayOrder(3)
                .build());

        moduleRepo.saveAll(List.of(
                mod("speaking-test", "communication", "Speaking Test", null, "FiMic",   "speaking-test", "Master the art of technical articulation. This module requires a <b>2-minute oral presentation</b> on a randomly assigned technical topic to assess clarity and confidence.", null, null, 1),
                mod("writing-test",  "communication", "Writing Test",  null, "FiEdit2", "writing-test",  "Enhance your technical documentation. Demonstrate your ability to craft a <b>Professional technical report</b> with precision and logical structure.", null, null, 2)
        ));

        // ── Data Skills ────────────────────────────────────────────────────────
        categoryRepo.save(AssessmentCategory.builder()
                .id("data-skills")
                .title("Data Skills")
                .topbarLabel("Data Skills")
                .heading("Data Proficiency")
                .accentWord("Modules")
                .subtitle("Select a data module to test your SQL, analytics, and visualization skills across practical real-world scenarios.")
                .badge("Data Wizard")
                .tip("Practice SQL queries on real datasets daily")
                .tag("Data")
                .icon("FiDatabase")
                .testTitle("Data Skills Assessment")
                .displayOrder(4)
                .build());

        moduleRepo.saveAll(List.of(
                mod("sql-fundamentals",   "data-skills", "SQL Fundamentals",         "INTERMEDIATE", "FiDatabase",   "mcq", "Evaluate query writing, joins, aggregations, subqueries, and relational database fundamentals.", "50 mins", 30, 1),
                mod("data-analysis",      "data-skills", "Data Analysis",            "INTERMEDIATE", "FiBarChart2",  "mcq", "Assess ability to clean, explore, and interpret datasets to extract actionable business insights.", "55 mins", 28, 2),
                mod("data-visualization", "data-skills", "Data Visualization",       "INTERMEDIATE", "FiPieChart",   "mcq", "Test knowledge of chart selection, dashboard design principles, and storytelling through visual data.", "40 mins", 22, 3),
                mod("statistics",         "data-skills", "Statistics & Probability", "ADVANCED",     "FiTrendingUp", "mcq", "Measure understanding of descriptive statistics, hypothesis testing, distributions, and regression analysis.", "60 mins", 35, 4),
                mod("ml-basics",          "data-skills", "Machine Learning Basics",  "ADVANCED",     "FiCpu",        "mcq", "Evaluate grasp of supervised and unsupervised algorithms, model evaluation metrics, and feature engineering.", "65 mins", 32, 5),
                mod("big-data",           "data-skills", "Big Data Concepts",        "ADVANCED",     "FiServer",     "mcq", "Test familiarity with distributed computing, Hadoop, Spark, data lakes, and large-scale pipeline design.", "50 mins", 26, 6)
        ));

        McqSection ds1 = assessmentService.seedSection("data-skills", "SQL & Databases",      1);
        McqSection ds2 = assessmentService.seedSection("data-skills", "Analytics & Statistics",2);
        McqSection ds3 = assessmentService.seedSection("data-skills", "Big Data & ML",         3);

        assessmentService.seedQuestion(ds1.getId(), "Which SQL clause is used to filter groups after a GROUP BY operation?", "WHERE", "FILTER", "HAVING", "AND", 2, 1);
        assessmentService.seedQuestion(ds1.getId(), "Which JOIN type returns all rows from both tables, filling NULL where there is no matching record?", "INNER JOIN", "LEFT JOIN", "FULL OUTER JOIN", "CROSS JOIN", 2, 2);
        assessmentService.seedQuestion(ds1.getId(), "In database normalization, which normal form eliminates partial functional dependencies?", "1NF", "2NF", "3NF", "BCNF", 1, 3);
        assessmentService.seedQuestion(ds1.getId(), "What does ACID stand for in the context of database transactions?", "Atomicity, Consistency, Isolation, Durability", "Access, Control, Integration, Data", "Aggregation, Consistency, Index, Delete", "Atomicity, Concurrency, Integrity, Distribution", 0, 4);

        assessmentService.seedQuestion(ds2.getId(), "Which statistical measure is LEAST affected by extreme outliers in a dataset?", "Mean", "Mode", "Median", "Standard Deviation", 2, 1);
        assessmentService.seedQuestion(ds2.getId(), "In machine learning, what does overfitting indicate about a trained model?", "It performs well on training data but poorly on unseen data", "It performs poorly on all data including training data", "It has too few parameters to learn patterns", "The training dataset is too small to be useful", 0, 2);
        assessmentService.seedQuestion(ds2.getId(), "Which chart type is best suited for showing the distribution of a continuous numerical variable?", "Bar chart", "Pie chart", "Histogram", "Line chart", 2, 3);
        assessmentService.seedQuestion(ds2.getId(), "A p-value of 0.03 with a significance level (α) of 0.05 means:", "Fail to reject the null hypothesis", "Reject the null hypothesis", "The test result is inconclusive", "The sample size is too small", 1, 4);

        assessmentService.seedQuestion(ds3.getId(), "Which of the following is a supervised learning algorithm?", "K-means Clustering", "PCA (Principal Component Analysis)", "Random Forest", "DBSCAN", 2, 1);
        assessmentService.seedQuestion(ds3.getId(), "What does ETL stand for in the context of data engineering pipelines?", "Execute, Transfer, Load", "Extract, Transform, Load", "Export, Test, Launch", "Encode, Transfer, Link", 1, 2);

        System.out.println("[DataSeeder] Seeded 4 assessment categories, modules, and MCQ questions.");
    }

    private AssessmentModule mod(String id, String categoryId, String title, String level,
                                  String icon, String type, String description,
                                  String duration, Integer questions, int order) {
        return AssessmentModule.builder()
                .id(id).categoryId(categoryId).title(title).level(level)
                .icon(icon).type(type).description(description)
                .duration(duration).questions(questions).displayOrder(order)
                .build();
    }

    // ── Communication Topics ──────────────────────────────────────────────────

    private void seedTopics() {
        if (topicRepo.count() > 0) return;

        // Speaking topics (5)
        communicationService.seedTopic("speaking",
            "The Future of Artificial Intelligence",
            "Describe a time you solved a difficult technical problem.",
            List.of());

        communicationService.seedTopic("speaking",
            "Cloud Computing in Modern Software Architecture",
            "Walk me through how you would design a cloud-native application.",
            List.of());

        communicationService.seedTopic("speaking",
            "Cybersecurity Best Practices for Developers",
            "Describe how you identified and handled a security vulnerability in your project.",
            List.of());

        communicationService.seedTopic("speaking",
            "The Impact of Open Source on Software Development",
            "Describe your experience contributing to or using open source software.",
            List.of());

        communicationService.seedTopic("speaking",
            "Agile Methodology in Team Projects",
            "Describe a challenge you faced working in an Agile team and how you resolved it.",
            List.of());

        // Writing prompts (5) — each with topic-specific keywords for evaluation
        communicationService.seedTopic("writing",
            "Explain the impact of Edge Computing on IoT Scalability.",
            "Discuss decentralized data processing, latency reduction, and bandwidth optimization in large-scale ecosystems.",
            List.of("edge","computing","iot","scalability","decentralized","latency","bandwidth","processing","ecosystem","sensor","gateway","device","cloud","network"));

        communicationService.seedTopic("writing",
            "Discuss the role of Microservices Architecture in modern software development.",
            "Cover service decomposition, API design, fault tolerance, and independent deployment strategies.",
            List.of("microservices","architecture","service","api","deployment","container","scalability","fault","tolerance","kubernetes","docker","independent","endpoint","communication"));

        communicationService.seedTopic("writing",
            "Explain how DevOps practices improve software delivery pipelines.",
            "Discuss CI/CD pipelines, automation, monitoring, and collaboration between development and operations teams.",
            List.of("devops","ci","cd","pipeline","deployment","automation","monitoring","testing","infrastructure","docker","kubernetes","collaboration","agile","release"));

        communicationService.seedTopic("writing",
            "Describe the importance of API design in building scalable distributed systems.",
            "Cover REST principles, versioning, authentication, rate limiting, and documentation strategies.",
            List.of("api","rest","endpoint","integration","versioning","authentication","documentation","request","response","protocol","interface","scalable","distributed","design"));

        communicationService.seedTopic("writing",
            "Explain the impact of Machine Learning on data-driven decision making.",
            "Discuss model selection, training data quality, bias mitigation, and deployment challenges.",
            List.of("machine","learning","model","algorithm","data","training","prediction","bias","accuracy","classification","neural","feature","dataset","deployment"));

        System.out.println("[DataSeeder] Seeded 5 speaking topics and 5 writing prompts.");
    }

    // ── TPO admin college scoping ────────────────────────────────────────────

    /*
     * Assigns a college to the known TPO admin account so the college-scoped
     * TPO panel has something to filter by. Idempotent — only fills it in
     * when missing, and does nothing if the account doesn't exist yet
     * (e.g. on a fresh DB before that user has signed up).
     */
    private void seedTpoAdminCollege() {
        userRepo.findByEmail("xisupa@forexzig.com").ifPresent(admin -> {
            if (admin.getCollege() == null || admin.getCollege().isBlank()) {
                admin.setCollege("Sunrise Institute of Technology");
                userRepo.save(admin);
                System.out.println("[DataSeeder] Set TPO admin college for xisupa@forexzig.com.");
            }
        });
    }
}
