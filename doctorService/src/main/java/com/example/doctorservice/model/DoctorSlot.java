package com.example.doctorservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single pre-created time slot that a doctor makes available for appointments.
 * <p>
 * Slot lifecycle: AVAILABLE → RESERVED (reserve) → AVAILABLE (release / compensation).
 * <p>
 * {@code @Version} provides optimistic locking — if two threads try to reserve the
 * same slot simultaneously the second will get an {@code OptimisticLockingFailureException}
 * which the service translates to {@code SlotNotAvailableException} (HTTP 409).
 */
@Entity
@Table(
    name = "doctor_slots",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_doctor_date_start",
        columnNames = {"doctor_id", "date", "start_time"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorSlot {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Column(name = "reserved_by")
    private UUID reservedBy;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
