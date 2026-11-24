package com.pranit.praya.tool;

import com.pranit.praya.dto.Contact;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ContactTool {

    public static final String NAME = "Pranit Bhangale";
    public static final String EMAIL = "pranitbhangale14@gmail.com";
    public static final String PORTFOLIO_URL = "https://pranitbhangale.vercel.app";
    public static final String LINKEDIN_URL = "https://www.linkedin.com/in/pranit-bhangale-1745542a7";
    
    @Tool(name = "contact_details", description = """
            Returns Pranit's contact details: name, email, portfolio website and LinkedIn profile.
            Use it when someone asks how to contact, reach, email or hire Pranit.
            """
    )
    public Contact getContactDetails() {
        return Contact.builder()
                .name(NAME)
                .email(EMAIL)
                .portfolioUrl(PORTFOLIO_URL)
                .linkedinUrl(LINKEDIN_URL)
                .build();
    }
}