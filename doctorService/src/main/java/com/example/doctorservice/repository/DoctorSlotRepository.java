package com.example.doctorservice.repository;

import com.example.doctorservice.model.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, UUID> {
    List<DoctorSlot> findByDoctorIdAndDateOrderByStartTimeAsc(UUID doctorId, LocalDate date);

    List<DoctorSlot> findByDoctorIdOrderByStartTimeAsc(UUID doctorId);

    @Query("""
            SELECT s FROM DoctorSlot s
            WHERE s.doctor.id = :doctorId
              AND s.date       = :date
              AND s.status     = 'AVAILABLE'
            ORDER BY s.startTime ASC
            """)
    List<DoctorSlot> findAvailableByDoctorAndDate(
            @Param("doctorId") UUID doctorId,
            @Param("date") LocalDate date);
}
