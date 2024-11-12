package jku.se.g03ue02code.service;

import jku.se.g03ue02code.entity.Grade;
import jku.se.g03ue02code.entity.Submission;
import jku.se.g03ue02code.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * Creates a new submission with the uploaded file.
     *
     * @param submission The submission entity containing metadata (e.g., user email).
     * @param file       The uploaded file to be associated with the submission.
     * @return The saved submission entity.
     */
    public Submission createSubmission(Submission submission, MultipartFile file) {
        try {
            submission.setFileName(file.getOriginalFilename());
            submission.setFileData(file.getBytes());
            return submissionRepository.save(submission);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file data", e);
        }
    }

    /**
     * Updates the file associated with an existing submission.
     *
     * @param id   The ID of the submission to update.
     * @param file The new file to be stored in the submission.
     * @return The updated submission entity.
     */
    public Submission updateSubmission(Long id, MultipartFile file) {
        Optional<Submission> existingSubmissionOpt = submissionRepository.findById(id);
        if (existingSubmissionOpt.isEmpty()) {
            throw new RuntimeException("Submission not found with id: " + id);
        }

        Submission existingSubmission = existingSubmissionOpt.get();
        try {
            existingSubmission.setFileName(file.getOriginalFilename());
            existingSubmission.setFileData(file.getBytes());
            return submissionRepository.save(existingSubmission);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update file data", e);
        }
    }

    /**
     * Deletes a submission by its ID.
     *
     * @param id The ID of the submission to delete.
     */
    public void deleteSubmission(Long id) {
        if (!submissionRepository.existsById(id)) {
            throw new RuntimeException("Could not find submission with ID: " + id);
        }
        submissionRepository.deleteById(id);
    }

    /**
     * Updates the grading for an existing submission.
     *
     * @param id      The ID of the submission to grade.
     * @param grading The grade to assign to the submission.
     * @return The updated submission entity with the new grade.
     */
    public Submission updateGrading(Long id, Grade grading) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + id));
        submission.setGrading(grading);
        return submissionRepository.save(submission);
    }

    /**
     * Retrieves submissions based on a user's email.
     *
     * @param email The email of the user whose submissions to retrieve.
     * @return A list of submissions associated with the specified email.
     */
    public List<Submission> findByUserEmail(String email) {
        return submissionRepository.findByUserEmail(email);
    }

    /**
     * Retrieves a specific submission by its ID.
     *
     * @param id The ID of the submission to retrieve.
     * @return The requested submission entity.
     */
    public Submission findById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + id));
    }
}