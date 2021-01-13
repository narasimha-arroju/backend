package org.alas.backend.repositories;

import org.alas.backend.documents.Exam;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ExamRepository extends ReactiveMongoRepository<Exam, String> {

    Mono<Exam> findByExamId(String examId);

}
