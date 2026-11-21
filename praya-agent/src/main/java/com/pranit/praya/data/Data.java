package com.pranit.praya.data;

import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Verified static data sourced directly from:
 * - Portfolio: https://pranitbhangale.vercel.app
 * - GitHub: https://github.com/pranit-bhangale
 * - LinkedIn Profile
 */
@UtilityClass
public class Data {

    public static final List<ProjectRecord> SEED_PROJECTS = List.of(
            ProjectRecord.builder()
                    .id("open-connect")
                    .name("Open-Connect Live Engine")
                    .description("Full-duplex real-time chat system with STOMP over WebSockets and AES-256-GCM message encryption at rest.")
                    .techStack(List.of("Java 21", "Spring Boot", "PostgreSQL", "React", "STOMP", "WebSockets", "Docker"))
                    .liveUrl("https://pranitbhangale.vercel.app")
                    .repoUrl("https://github.com/pranit-bhangale/open-connect")
                    .highlights(List.of(
                            "Full-duplex STOMP over WebSockets for bi-directional live communication",
                            "AES-256-GCM message payload encryption at rest",
                            "Host presence heartbeat detection and session management",
                            "Asynchronous email notification daemon for unread chat alerts"
                    ))
                    .build(),

            ProjectRecord.builder()
                    .id("habit-tracker")
                    .name("Habit Tracker Web Application")
                    .description("Full-stack habit and routine management platform featuring analytical streak tracking and secure authorization.")
                    .techStack(List.of("React.js", "ASP.NET Web API", "MySQL", "Docker", "AWS"))
                    .liveUrl("https://pranitbhangale.vercel.app")
                    .repoUrl("https://github.com/pranit-bhangale/habit-tracker")
                    .highlights(List.of(
                            "Clean Architecture with decoupled domain business rules",
                            "Comprehensive automated streak tracking",
                            "Docker containerization deployed on AWS infrastructure"
                    ))
                    .build(),

            ProjectRecord.builder()
                    .id("todo-app")
                    .name("Spring MVC To-Do Application")
                    .description("Production-ready task management backend utilizing robust JPA persistence and relational normalization.")
                    .techStack(List.of("Java", "Spring Boot", "Hibernate", "JPA", "MySQL"))
                    .liveUrl("https://pranitbhangale.vercel.app")
                    .repoUrl("https://github.com/pranit-bhangale")
                    .highlights(List.of(
                            "Relational data model with transaction demarcation",
                            "Thymeleaf / REST API endpoints with bean validation",
                            "Layered architecture adhering strictly to SOLID principles"
                    ))
                    .build(),

            ProjectRecord.builder()
                    .id("cleaning-bot")
                    .name("Smart Floor Surface Cleaning Bot")
                    .description("Autonomous IoT floor cleaning robot featuring obstacle detection and wireless video telemetry.")
                    .techStack(List.of("ESP32", "ESP32-CAM", "Ultrasonic Sensors", "C++", "FreeRTOS", "IoT"))
                    .liveUrl("https://pranitbhangale.vercel.app")
                    .repoUrl("https://github.com/pranit-bhangale")
                    .highlights(List.of(
                            "Real-time video streaming over Wi-Fi using ESP32-CAM",
                            "Ultrasonic obstacle avoidance and automatic route redirection",
                            "Dual motor driver integration for precision floor scrubbing"
                    ))
                    .build()
    );

    public static final List<KnowledgeItem> SEED_KNOWLEDGE = List.of(
            KnowledgeItem.builder()
                    .id("bio")
                    .category("bio")
                    .title("About Pranit Bhangale")
                    .content("Pranit Bhangale is a Backend Developer and Systems Engineer passionate about building secure, scalable, and resilient distributed systems. He holds a Bachelor of Engineering (BE) in Electronics and Telecommunication from Savitribai Phule Pune University and completed a Post Graduate Diploma in Advanced Computing (PG-DAC) from the Institute of Emerging Technologies (IET), CDAC. He specializes in Java, Spring Boot, Microservices, and Cloud-Native architectures.")
                    .tags(List.of("bio", "about", "engineer", "education", "degree", "iet", "cdac", "pune university"))
                    .build(),

            KnowledgeItem.builder()
                    .id("skills")
                    .category("skills")
                    .title("Technical Skills & Tooling")
                    .content("Languages: Java (21, 25), Python, TypeScript, JavaScript, SQL, C++. Frameworks: Spring Framework, Spring Boot 3/4, Spring AI, Hibernate/JPA, React.js, ASP.NET Web API. Databases: PostgreSQL, MySQL, Redis. DevOps & Tools: Docker, AWS, Render, Vercel, Git, GitHub, IntelliJ IDEA, Postman, Linux.")
                    .tags(List.of("skills", "java", "spring boot", "spring ai", "react", "postgresql", "docker", "aws", "tech stack"))
                    .build(),

            KnowledgeItem.builder()
                    .id("contact")
                    .category("contact")
                    .title("Contact & Web Profiles")
                    .content("Portfolio: https://pranitbhangale.vercel.app | GitHub: https://github.com/pranit-bhangale | LinkedIn: https://linkedin.com/in/pranit-bhangale | Location: Pune / Maharashtra, India.")
                    .tags(List.of("contact", "links", "portfolio", "github", "linkedin", "email", "profiles"))
                    .build()
    );
}
