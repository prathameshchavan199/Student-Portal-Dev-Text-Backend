package com.webapp.cognitodemo.config;

import com.webapp.cognitodemo.entity.assessment.*;
import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.repo.*;
import com.webapp.cognitodemo.service.AssessmentService;
import com.webapp.cognitodemo.service.CommunicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    @Autowired private CourseRepo courseRepo;
    @Autowired private AssessmentCategoryRepo categoryRepo;
    @Autowired private AssessmentModuleRepo moduleRepo;
    @Autowired private AssessmentService assessmentService;
    @Autowired private CommunicationService communicationService;
    @Autowired private AssessmentTopicRepo topicRepo;

    @Override
    public void run(ApplicationArguments args) {
        seedCourses();
        seedAssessments();
        seedTopics();
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
}
