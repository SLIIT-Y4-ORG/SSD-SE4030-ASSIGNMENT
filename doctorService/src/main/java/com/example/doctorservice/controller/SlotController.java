package com.example.doctorservice.controller;

import com.example.doctorservice.dto.ReserveSlotRequest;
import com.example.doctorservice.dto.SlotResponse;
import com.example.doctorservice.service.SlotService;
import com.example.doctorservice.util.AuthHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotService slotService;
    private final AuthHelper authHelper;

    public SlotController(SlotService slotService, AuthHelper authHelper) {
        this.slotService = slotService;
        this.authHelper = authHelper;
    }

    @GetMapping("/{slotId}")
    public ResponseEntity<SlotResponse> getSlot(@RequestHeader("Authorization") String authHeader, @PathVariable UUID slotId) {
        authHelper.requireAuth(authHeader);
        return ResponseEntity.ok(slotService.getSlotById(slotId));
    }

    @PostMapping("/{slotId}/reserve")
    public ResponseEntity<SlotResponse> reserveSlot(@RequestHeader("Authorization") String authHeader, @PathVariable UUID slotId, @RequestBody ReserveSlotRequest request) {
        authHelper.requireAuth(authHeader);
        return ResponseEntity.ok(slotService.reserveSlot(slotId, request));
    }

    @PostMapping("/{slotId}/release")
    public ResponseEntity<SlotResponse> releaseSlot(@RequestHeader("Authorization") String authHeader, @PathVariable UUID slotId) {
        authHelper.requireAuth(authHeader);
        return ResponseEntity.ok(slotService.releaseSlot(slotId));
    }
}
