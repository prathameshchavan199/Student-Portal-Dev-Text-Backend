package com.webapp.cognitodemo.config;

import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.repo.CourseRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    @Autowired
    private CourseRepo courseRepo;

    @Override
    public void run(ApplicationArguments args) {
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
}
