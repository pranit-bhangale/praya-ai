package com.pranit.praya.tool;

import com.pranit.praya.dto.Entry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EducationTool {

    public static final Entry PG_DAC = new Entry(
            "Post Graduation in Advanced Computing (PG-DAC)",
            "Institute of Emerging Technologies (IET)",
            "2025 - 2026");

    public static final Entry BACHELOR_ENTC = new Entry(
            "Bachelor of Engineering in Electronics and Telecommunication Engineering (E&TC)",
            "Savitribai Phule Pune University",
            "2021 - 2025");

    public static final Entry HSC = new Entry(
            "Higher Secondary Education (12th - Science)",
            "Maharashtra State Board",
            "2019 - 2021");

    public static final Entry SSC = new Entry(
            "Secondary Education (10th)",
            "Maharashtra State Board",
            "2018 - 2019");

    public static final List<Entry> EDUCATION = List.of(PG_DAC, BACHELOR_ENTC, HSC, SSC);

    @Tool(name = "education_details", description = """
            Returns Pranit's education history, latest first: degree, institution and years
            (PG-DAC, B.E. in E&TC, 12th and 10th). Use it for questions about studies,
            degrees, qualifications, college, university or schooling.
            """
    )
    public List<Entry> getEducation() {
        return EDUCATION;
    }
}