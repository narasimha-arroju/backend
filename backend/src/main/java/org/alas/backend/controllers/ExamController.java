package org.alas.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alas.backend.dto.*;
import org.alas.backend.handlers.JudgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.alas.backend.documents.Exam;

import org.alas.backend.handlers.ExamHandler;
import org.alas.backend.handlers.SubmissionHandler;

import java.io.IOException;
import java.time.Duration;


@RestController
public class ExamController {

    @Autowired
    private ExamHandler examHandler;

    @Autowired
    private SubmissionHandler submissionHandler;

    @Autowired
    private JudgeHandler judgeHandler;

    @PostMapping("/author/exams")
    public ResponseEntity<?> createExam(@RequestBody Exam exam) {
        examHandler.createExam(exam);
        return new ResponseEntity<>("Exam Created", HttpStatus.CREATED);
    }

    @GetMapping("/author/exams/{examId}")
    public ResponseEntity<?> getExamWithAnswersById(@PathVariable String examId) {
        Mono<Exam> examDataDTOMono = examHandler.getExamWithAnswersById(examId);
        return new ResponseEntity<>(examDataDTOMono, HttpStatus.OK);
    }

    @GetMapping("/author/exams/{examId}/end")
    public ResponseEntity<?> endExam(@PathVariable String examId) {
        examHandler.endExamByExamId(examId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/candidate/exams/{examId}/submit")
    public ResponseEntity<?> submitExam(@PathVariable String examId, @RequestParam String candidateId){
        submissionHandler.submitExam(examId, candidateId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/candidate/exams")
    public ResponseEntity<?> getAllExams(@RequestParam String candidateId) {
        Flux<ExamDTO> examFlux = examHandler.getAllExams(examHandler.getSubmissionStatusMap(candidateId));
        return new ResponseEntity<>(examFlux, HttpStatus.OK);
    }

    @GetMapping("/candidate/exams/{examId}")
    public ResponseEntity<?> getExamWithoutAnswersById(@PathVariable String examId, @RequestParam String candidateId) {
        Mono<?> examDataDTOMono = examHandler.getExamWithoutAnswersById(examId, candidateId);
        return new ResponseEntity<>(examDataDTOMono, HttpStatus.OK);
    }

    @PostMapping("/candidate/submission")
    public ResponseEntity<?> newSubmission(@RequestParam String examId, @RequestParam String candidateId,
                                           @RequestParam String questionType, @RequestBody String visit) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        switch (questionType) {
            case "mcq":
                VisitDTO visitDTO = objectMapper.readValue(visit, VisitDTO.class);
                Mono<?> responseMono = submissionHandler.addVisit(examId, candidateId, visitDTO);
                return new ResponseEntity<>(responseMono, HttpStatus.CREATED);
            case "coding":
                CodeDTO codeDTO = objectMapper.readValue(visit, CodeDTO.class);
                JudgeRequestDTO judgeRequestDTO = new JudgeRequestDTO(codeDTO.getLanguage_id(), codeDTO.getCode(), codeDTO.getStdin(), codeDTO.getExpectedOutput());
                Mono<GetSubmissionResponse> responseMono1 = judgeHandler.createSubmission(judgeRequestDTO)
                        .delayElement(Duration.ofMillis(5000))
                        .flatMap(creationResponse -> judgeHandler.getSubmission(creationResponse.getToken()));

                if (codeDTO.getSubmit())
                    responseMono1.subscribe(getSubmissionResponse ->
                            submissionHandler.addCodeSubmission(examId, candidateId, codeDTO.getQuestionId(), getSubmissionResponse)
                                    .subscribe());
                return new ResponseEntity<>(responseMono1, HttpStatus.CREATED);

            default:
                throw new IllegalStateException("Unexpected value: " + questionType);
        }
    }
}
