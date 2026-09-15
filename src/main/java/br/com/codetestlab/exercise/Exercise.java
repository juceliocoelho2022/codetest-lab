package br.com.codetestlab.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "exercises")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 180)
    private String className;

    @Lob
    @Column(nullable = false)
    private String hiddenTestCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Exercise() {
    }

    public Exercise(String title, String description, String className, String hiddenTestCode) {
        this.title = title;
        this.description = description;
        this.className = className;
        this.hiddenTestCode = hiddenTestCode;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getClassName() { return className; }
    public String getHiddenTestCode() { return hiddenTestCode; }
    public Instant getCreatedAt() { return createdAt; }
}
