package bd.fasol;

import bd.fasol.model.*;
import bd.fasol.repository.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FasolDoctorApplication {
    public static void main(String[] args) {
        SpringApplication.run(FasolDoctorApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(CropRepository crops, DiseaseRepository diseases, TreatmentRepository treatments) {
        return args -> {
            if (crops.count() > 0) return;
            Crop rice = new Crop();
            rice.nameBn = "ধান";
            rice.nameEn = "Rice";
            rice.imageUrl = "https://api.example.com/assets/crops/rice.png";
            rice.selectable = true;
            crops.save(rice);
            String[][] rows = {{"ব্লাস্ট", "Blast"}, {"ব্রাউন স্পট", "Brown spot"}, {"সুস্থ", "Healthy"}, {"লিফ স্মাট", "Leaf smut"}, {"রাইস টুংরো", "Rice Tungro"}, {"শীথ ব্লাইট", "Sheath blight"}};
            for (String[] row : rows) {
                Disease disease = new Disease();
                disease.crop = rice;
                disease.modelClassLabel = disease.nameBn = row[0];
                disease.nameEn = row[1];
                diseases.save(disease);
                Treatment treatment = new Treatment();
                treatment.disease = disease;
                // Placeholder only — not agronomist-verified.
                treatment.textBn = "কৃষিবিদ-যাচাইবিহীন অস্থায়ী পরামর্শ; বিশেষজ্ঞের সাথে যোগাযোগ করুন।";
                treatment.sourceNote = "Placeholder only — not agronomist-verified";
                treatments.save(treatment);
            }
        };
    }
}
