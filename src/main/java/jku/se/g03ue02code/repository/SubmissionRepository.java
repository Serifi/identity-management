package jku.se.g03ue02code.repository;

import jku.se.g03ue02code.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository interface for Submission entities.
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    /**
     * Finds submissions by user email.
     *
     * @param userEmail the email of the user
     * @return list of submissions
     */
    List<Submission> findByUserEmail(String userEmail);
}
