package com.example.doctorservice.repository;

import com.example.doctorservice.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    boolean existsByEmail(String email);
    boolean existsByUserId(UUID userId);
    boolean existsByLicenseNumber(String licenseNumber);

    Optional<Doctor> findByEmail(String email);

    /** Look up the doctor profile linked to a user-service account. */
    Optional<Doctor> findByUserId(UUID userId);

    List<Doctor> findByIsActiveTrue();

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    List<Doctor> findByDepartmentIgnoreCase(String department);

    List<Doctor> findByVerifiedFalseOrderByCreatedAtAsc();
    List<Doctor> findByVerifiedTrueAndIsActiveTrue();
    List<Doctor> findByVerifiedTrueAndIsActiveTrueAndSpecializationIgnoreCase(String specialization);
    List<Doctor> findByVerifiedTrueAndIsActiveTrueAndDepartmentIgnoreCase(String department);
}
