package com.pranit.praya.constant;

import com.pranit.praya.model.ProjectRecord;

import java.util.List;

/**
 * Verified constants for Pranit's software projects and baseline catalog.
 */
public final class ProjectConstants {

    private ProjectConstants() {}

    // Open-Connect Live Engine
    public static final String ID_OPEN_CONNECT = "open-connect";
    public static final String NAME_OPEN_CONNECT = "Open-Connect Live Engine";
    public static final String DESC_OPEN_CONNECT = "Full-duplex real-time chat system with STOMP over WebSockets and AES-256-GCM message encryption at rest.";
    public static final List<String> TECH_OPEN_CONNECT = List.of(
            "Java 21", "Spring Boot", "PostgreSQL", "React", "STOMP", "WebSockets", "Docker"
    );
    public static final List<String> HIGHLIGHTS_OPEN_CONNECT = List.of(
            "Full-duplex STOMP over WebSockets for bi-directional live communication",
            "AES-256-GCM message payload encryption at rest",
            "Host presence heartbeat detection and session management",
            "Asynchronous email notification daemon for unread chat alerts"
    );

    // Habit Tracker Web Application
    public static final String ID_HABIT_TRACKER = "habit-tracker";
    public static final String NAME_HABIT_TRACKER = "Habit Tracker Web Application";
    public static final String DESC_HABIT_TRACKER = "Full-stack habit and routine management platform featuring analytical streak tracking and secure authorization.";
    public static final List<String> TECH_HABIT_TRACKER = List.of(
            "React.js", "ASP.NET Web API", "MySQL", "Docker", "AWS"
    );
    public static final List<String> HIGHLIGHTS_HABIT_TRACKER = List.of(
            "Clean Architecture with decoupled domain business rules",
            "Comprehensive automated streak tracking",
            "Docker containerization deployed on AWS infrastructure"
    );

    // Spring MVC To-Do Application
    public static final String ID_TODO_APP = "todo-app";
    public static final String NAME_TODO_APP = "Spring MVC To-Do Application";
    public static final String DESC_TODO_APP = "Production-ready task management backend utilizing robust JPA persistence and relational normalization.";
    public static final List<String> TECH_TODO_APP = List.of(
            "Java", "Spring Boot", "Hibernate", "JPA", "MySQL"
    );
    public static final List<String> HIGHLIGHTS_TODO_APP = List.of(
            "Relational data model with transaction demarcation",
            "Thymeleaf / REST API endpoints with bean validation",
            "Layered architecture adhering strictly to SOLID principles"
    );

    // Smart Floor Surface Cleaning Bot
    public static final String ID_CLEANING_BOT = "cleaning-bot";
    public static final String NAME_CLEANING_BOT = "Smart Floor Surface Cleaning Bot";
    public static final String DESC_CLEANING_BOT = "Autonomous IoT floor cleaning robot featuring obstacle detection and wireless video telemetry.";
    public static final List<String> TECH_CLEANING_BOT = List.of(
            "ESP32", "ESP32-CAM", "Ultrasonic Sensors", "C++", "FreeRTOS", "IoT"
    );
    public static final List<String> HIGHLIGHTS_CLEANING_BOT = List.of(
            "Dual-core ESP32 microcontroller with FreeRTOS multitasking",
            "Ultrasonic rangefinders for autonomous obstacle avoidance",
            "Embedded ESP32-CAM live video web server telemetry"
    );

    public static List<ProjectRecord> getBaselineProjects(String username) {
        String baseRepoUrl = "https://github.com/" + username;
        return List.of(
                ProjectRecord.builder()
                        .id(ID_OPEN_CONNECT)
                        .name(NAME_OPEN_CONNECT)
                        .description(DESC_OPEN_CONNECT)
                        .techStack(TECH_OPEN_CONNECT)
                        .liveUrl(ProfileConstants.PORTFOLIO_URL)
                        .repoUrl(baseRepoUrl + "/" + ID_OPEN_CONNECT)
                        .highlights(HIGHLIGHTS_OPEN_CONNECT)
                        .build(),

                ProjectRecord.builder()
                        .id(ID_HABIT_TRACKER)
                        .name(NAME_HABIT_TRACKER)
                        .description(DESC_HABIT_TRACKER)
                        .techStack(TECH_HABIT_TRACKER)
                        .liveUrl(ProfileConstants.PORTFOLIO_URL)
                        .repoUrl(baseRepoUrl + "/" + ID_HABIT_TRACKER)
                        .highlights(HIGHLIGHTS_HABIT_TRACKER)
                        .build(),

                ProjectRecord.builder()
                        .id(ID_TODO_APP)
                        .name(NAME_TODO_APP)
                        .description(DESC_TODO_APP)
                        .techStack(TECH_TODO_APP)
                        .liveUrl(ProfileConstants.PORTFOLIO_URL)
                        .repoUrl(baseRepoUrl)
                        .highlights(HIGHLIGHTS_TODO_APP)
                        .build(),

                ProjectRecord.builder()
                        .id(ID_CLEANING_BOT)
                        .name(NAME_CLEANING_BOT)
                        .description(DESC_CLEANING_BOT)
                        .techStack(TECH_CLEANING_BOT)
                        .liveUrl(ProfileConstants.PORTFOLIO_URL)
                        .repoUrl(baseRepoUrl)
                        .highlights(HIGHLIGHTS_CLEANING_BOT)
                        .build()
        );
    }
}
