package com.example.doctorservice.service;

import com.example.doctorservice.dto.ReserveSlotRequest;
import com.example.doctorservice.dto.SlotResponse;
import com.example.doctorservice.exception.SlotNotAvailableException;
import com.example.doctorservice.exception.SlotNotFoundException;
import com.example.doctorservice.model.DoctorSlot;
import com.example.doctorservice.model.SlotStatus;
import com.example.doctorservice.repository.DoctorSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final DoctorSlotRepository slotRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // RESERVE  (AVAILABLE → RESERVED)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SlotResponse reserveSlot(UUID slotId, ReserveSlotRequest request) {
        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotNotAvailableException(slotId);
        }
        slot.setStatus(SlotStatus.RESERVED);
        slot.setReservedBy(request.getPatientId());
        slot.setAppointmentId(request.getAppointmentId());
        return toSlotResponse(slotRepository.save(slot));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RELEASE  (RESERVED → AVAILABLE)  — saga compensation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SlotResponse releaseSlot(UUID slotId) {
        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setReservedBy(null);
        slot.setAppointmentId(null);
        return toSlotResponse(slotRepository.save(slot));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SlotResponse getSlotById(UUID slotId) {
        return toSlotResponse(
                slotRepository.findById(slotId)
                        .orElseThrow(() -> new SlotNotFoundException(slotId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapper
    // ─────────────────────────────────────────────────────────────────────────

    private SlotResponse toSlotResponse(DoctorSlot s) {
        return SlotResponse.builder()
                .id(s.getId())
                .doctorId(s.getDoctor().getId())
                .doctorName(s.getDoctor().getName())
                .date(s.getDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .reservedBy(s.getReservedBy())
                .appointmentId(s.getAppointmentId())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
