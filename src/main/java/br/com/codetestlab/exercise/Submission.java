package br.com.codetestlab.exercise;

import br.com.codetestlab.execution.ExecutionResult;
import br.com.codetestlab.execution.ExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false, length = 120)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionType submissionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ExecutionStatus status;

    @Column(nullable = false)
    private int testsRun;

    @Column(nullable = false)
    private int testsPassed;

    @Column(nullable = false)
    private int testsFailed;

    @Column(nullable = false)
    private int testsSkipped;

    @Column(nullable = false)
    private long durationMs;

    @Lob
    @Column(nullable = false)
    private String output;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Submission() {
    }

    public Submission(Exercise exercise, String studentName, SubmissionType submissionType, ExecutionResult result) {
        this.exercise = exercise;
        this.studentName = studentName;
        this.submissionType = submissionType;
        this.status = result.status();
        this.testsRun = result.testsRun();
        this.testsPassed = result.testsPassed();
        this.testsFailed = result.testsFailed();
        this.testsSkipped = result.testsSkipped();
        this.durationMs = result.durationMs();
        this.output = result.output() == null ? "" : result.output();
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Exercise getExercise() { return exercise; }
    public String getStudentName() { return studentName; }
    public SubmissionType getSubmissionType() { return submissionType; }
    public ExecutionStatus getStatus() { return status; }
    public int getTestsRun() { return testsRun; }
    public int getTestsPassed() { return testsPassed; }
    public int getTestsFailed() { return testsFailed; }
    public int getTestsSkipped() { return testsSkipped; }
    public long getDurationMs() { return durationMs; }
    public String getOutput() { return output; }
    public Instant getCreatedAt() { return createdAt; }
}
