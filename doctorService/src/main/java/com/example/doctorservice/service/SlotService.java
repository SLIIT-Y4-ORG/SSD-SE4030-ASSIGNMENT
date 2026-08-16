package com.example.doctorservice.service;

import com.example.doctorservice.dto.ReserveSlotRequest;
import com.example.doctorservice.dto.SlotResponse;

import java.util.UUID;

public interface SlotService {

    SlotResponse reserveSlot(UUID slotId, ReserveSlotRequest request);

    SlotResponse releaseSlot(UUID slotId);

    SlotResponse getSlotById(UUID slotId);
}
