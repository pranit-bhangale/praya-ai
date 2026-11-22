package com.pranit.praya.constant;

import java.util.List;

/**
 * Verified personal and profile constants for Pranit Bhangale.
 */
public final class ProfileConstants {

    private ProfileConstants() {}

    public static final String NAME = "Pranit Bhangale";
    public static final String DEFAULT_GITHUB_USERNAME = "pranit-bhangale";
    public static final String PORTFOLIO_URL = "https://pranitbhangale.vercel.app";
    public static final String GITHUB_URL = "https://github.com/pranit-bhangale";
    public static final String LINKEDIN_URL = "https://www.linkedin.com/in/pranit-bhangale-1745542a7";

    public static final String DEFAULT_BIO = "Full-Stack & Systems Engineer specializing in high-throughput distributed applications.";

    // Education & Academic Degrees
    public static final String EDUCATION_CONTENT = """
            1. Bachelor of Engineering (BE) in Electronics & Telecommunication from Savitribai Phule Pune University (SPPU).
            2. Post Graduate Diploma in Advanced Computing (PG-DAC) from the Institute of Emerging Technologies (IET), CDAC.
            Focused on Object-Oriented Programming, Data Structures, Operating Systems, Computer Networks, and Microservices.
            """;

    // Career & Architecture Philosophy
    public static final String PHILOSOPHY_CONTENT = """
            Backend Developer and Systems Engineer dedicated to crafting robust, fault-tolerant distributed applications.
            Core competencies: Clean Architecture, SOLID Principles, Test-Driven Development (TDD), High-throughput REST APIs, and Reactive Streaming.
            """;

    // Technical Stack
    public static final String TECH_STACK_CONTENT = """
            Primary Languages: Java (21, 25), Python, TypeScript, JavaScript, SQL, C++.
            Frameworks: Spring Framework, Spring Boot 3/4, Spring AI, Hibernate/JPA, React.js, ASP.NET Web API.
            Infrastructure: Docker, AWS, Render, Vercel, Git, PostgreSQL, MySQL, Redis.
            """;

    // Categories
    public static final String CATEGORY_BIO = "bio";
    public static final String CATEGORY_EDUCATION = "education";
    public static final String CATEGORY_EXPERIENCE = "experience";
    public static final String CATEGORY_SKILLS = "skills";
    public static final String CATEGORY_CONTACT = "contact";
    public static final String CATEGORY_PROFILE = "profile";
    public static final String CATEGORY_BLOG = "blog";
    public static final String CATEGORY_CERTIFICATION = "certification";
    public static final String CATEGORY_POSTS = "posts";

    // Tags
    public static final List<String> TAGS_EDUCATION = List.of(
            "education", "degree", "be", "sppu", "pune university", "cdac", "iet", "pg-dac"
    );
    public static final List<String> TAGS_PHILOSOPHY = List.of(
            "experience", "philosophy", "architecture", "solid", "clean architecture"
    );
    public static final List<String> TAGS_SKILLS = List.of(
            "github", "skills", "java", "spring boot", "react", "docker", "aws"
    );
    public static final List<String> TAGS_CONTACT = List.of(
            "linkedin", "profile", "contact", "network"
    );
    public static final List<String> TAGS_BLOG = List.of(
            "blog", "articles", "technical writing", "system design", "architecture"
    );
    public static final List<String> TAGS_CERTIFICATION = List.of(
            "certifications", "credentials", "diploma", "cdac", "sppu", "qualifications"
    );
    public static final List<String> TAGS_POSTS = List.of(
            "linkedin", "posts", "thought leadership", "engineering", "updates"
    );
}
