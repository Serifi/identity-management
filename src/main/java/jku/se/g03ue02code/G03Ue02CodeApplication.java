package jku.se.g03ue02code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jku.se.g03ue02code.entity.Submission;
import jku.se.g03ue02code.entity.Grade;
import jku.se.g03ue02code.repository.SubmissionRepository;

/**
 * Main application class for initializing and running the G03Ue02Code application.
 */
@SpringBootApplication
public class G03Ue02CodeApplication {
	private static final Logger log = LoggerFactory.getLogger(G03Ue02CodeApplication.class);

	/**
	 * Main method to launch the Spring Boot application.
	 *
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(G03Ue02CodeApplication.class, args);
	}

	void createSubmissions(String userEmail, int count, boolean graded, SubmissionRepository submissionRepository) {
		for (int i = 1; i <= count; i++) {
			Submission submission = new Submission();
			submission.setUserEmail(userEmail);
			submission.setFileName("submission_" + i + ".pdf");
			submission.setFileData(("Dummy content for submission " + i).getBytes());

			if (graded) {
				submission.setGrading(Grade.SEHR_GUT);
			}

			submissionRepository.save(submission);
		}
	}

	/**
	 * Initializes the database with sample submission data upon application startup.
	 *
	 * @param submissionRepository Repository to manage Submission entities.
	 * @return A CommandLineRunner that sets up initial data in the H2 database.
	 */
	@Bean
	CommandLineRunner initDatabase(final SubmissionRepository submissionRepository) {
		return args -> {
			submissionRepository.deleteAll();

			createSubmissions("tom@gmail.com", 1, true, submissionRepository);
			createSubmissions("tom@gmail.com", 2, false, submissionRepository);

			createSubmissions("lukas@gmail.com", 3, true, submissionRepository);
			createSubmissions("lukas@gmail.com", 4, false, submissionRepository);

			createSubmissions("laura@gmail.com", 5, true, submissionRepository);
			createSubmissions("laura@gmail.com", 6, false, submissionRepository);
		};
	}
}