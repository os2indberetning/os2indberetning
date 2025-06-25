package dk.digitalidentity.indberetning.model.entity;

import dk.digitalidentity.indberetning.model.entity.enums.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "error_logs")
public class ErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "solved_date", nullable = true)
    private LocalDateTime solutionDate;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private Person assignedTo;

    @ManyToOne
    @JoinColumn(name = "solved_by", nullable = true)
    private Person solutionBy;

    @OneToOne(optional = false, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private ErrorResponse errorResponse;

    @Column(name = "error_code")
    private int errorCode;

    @Column(name = "error_text")
    private String errorText;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type")
    private ErrorType errorType;

    @OneToOne(optional = false, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private ErrorComment comment;

    @Column(name = "email_sent")
    private boolean emailSent;

    @ManyToOne
    @JoinColumn(name = "report_id")
    private Report report;
}
