package jku.se.g03ue02code.controller;

import jku.se.g03ue02code.entity.Grade;
import jku.se.g03ue02code.entity.Submission;
import jku.se.g03ue02code.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * REST controller for managing submissions, including functionality for file upload,
 * file download, grading, updating, and deleting submissions.
 */
@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private SubmissionService submissionService;

    /**
     * Submits a new file submission for the authenticated user.
     *
     * @param file      the file to be submitted.
     * @param principal the OAuth2 user information of the authenticated user.
     * @return the created Submission object.
     * @throws IllegalStateException if the user is not authenticated.
     */
    @PostMapping("/submit")
    public Submission submit(@RequestParam("file") MultipartFile file,
                             @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            throw new IllegalStateException("User not authenticated");
        }

        String userEmail = principal.getAttribute("email");
        logger.info("Submitting new file for user: {}", userEmail);
        Submission submission = new Submission();
        submission.setUserEmail(userEmail);
        return submissionService.createSubmission(submission, file);
    }

    /**
     * Updates an existing file submission by ID.
     *
     * @param id   the ID of the submission to update.
     * @param file the new file to replace the existing submission.
     * @return the updated Submission object.
     */
    @PutMapping("/update/{id}")
    public Submission update(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return submissionService.updateSubmission(id, file);
    }

    /**
     * Deletes a submission by ID.
     *
     * @param id the ID of the submission to delete.
     */
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        submissionService.deleteSubmission(id);
    }

    /**
     * Updates the grading for a specific submission by ID.
     *
     * @param id      the ID of the submission to be graded.
     * @param grading the grade to assign to the submission.
     * @return the updated Submission object with the new grading.
     */
    @PutMapping("/grading/{id}")
    public Submission updateGrading(@PathVariable Long id, @RequestParam("grading") Grade grading) {
        return submissionService.updateGrading(id, grading);
    }

    /**
     * Retrieves the list of submissions for the authenticated user.
     *
     * @param principal the OAuth2 user information of the authenticated user.
     * @return a list of the user's submissions.
     * @throws IllegalStateException if the user is not authenticated.
     */
    @GetMapping("/my")
    public List<Submission> getMySubmissions(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            logger.error("User not authenticated");
            throw new IllegalStateException("User not authenticated");
        }
        String userEmail = principal.getAttribute("email");
        logger.info("Fetching submissions for user: {}", userEmail);
        return submissionService.findByUserEmail(userEmail);
    }

    /**
     * Retrieves the list of submissions for a specific user by their email address.
     *
     * @param email the email address of the user.
     * @return a list of submissions for the specified user.
     */
    @GetMapping("/user/{email}")
    public List<Submission> getUserSubmissions(@PathVariable String email) {
        logger.info("Fetching submissions for user with email: {}", email);
        return submissionService.findByUserEmail(email);
    }

    /**
     * Finds a specific submission by ID.
     *
     * @param id the ID of the submission to find.
     * @return the Submission object if found.
     */
    @GetMapping("/{id}")
    public Submission findById(@PathVariable Long id) {
        return submissionService.findById(id);
    }

    /**
     * Retrieves the file data of a specific submission as a downloadable resource.
     *
     * @param id the ID of the submission file to retrieve.
     * @return a ResponseEntity containing the file data as a downloadable resource.
     */
    @GetMapping("/file/{id}")
    public ResponseEntity<Resource> getFile(@PathVariable Long id) {
        Submission submission = submissionService.findById(id);
        ByteArrayResource resource = new ByteArrayResource(submission.getFileData());
        String contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + submission.getFileName() + "\"")
                .body(resource);
    }
}