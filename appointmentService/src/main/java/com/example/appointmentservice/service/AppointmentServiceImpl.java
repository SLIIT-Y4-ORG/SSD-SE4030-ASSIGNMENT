package com.example.appointmentservice.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.appointmentservice.client.PaymentServiceClient;
import com.example.appointmentservice.client.PaymentServiceClient.PaymentSessionRequest;
import com.example.appointmentservice.client.PaymentServiceClient.PaymentSessionResponse;
import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.CreateAppointmentRequest;
import com.example.appointmentservice.exception.BadRequestException;
import com.example.appointmentservice.exception.ResourceNotFoundException;
import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.PaymentStatus;
import com.example.appointmentservice.repository.AppointmentRepository;
import com.example.appointmentservice.util.AppointmentMapper;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final com.example.appointmentservice.client.PatientServiceClient patientServiceClient;
    private final com.example.appointmentservice.client.DoctorServiceClient doctorServiceClient;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  PaymentServiceClient paymentServiceClient,
                                  com.example.appointmentservice.client.PatientServiceClient patientServiceClient,
                                  com.example.appointmentservice.client.DoctorServiceClient doctorServiceClient) {
        this.appointmentRepository = appointmentRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.patientServiceClient = patientServiceClient;
        this.doctorServiceClient = doctorServiceClient;
    }

    @Override
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        validateCreateRequest(request);

        if (appointmentRepository.existsBySlotIdAndStatus(request.getSlotId(), AppointmentStatus.CONFIRMED)) {
            throw new BadRequestException("This slot is already confirmed");
        }

        // Validate patient & doctor exist in their respective services
        try {
            var patient = patientServiceClient.getPatientById(request.getPatientId());
            if (patient == null) {
                throw new ResourceNotFoundException("Patient not found: " + request.getPatientId());
            }
        } catch (RuntimeException ex) {
            throw new ResourceNotFoundException("Patient validation failed: " + ex.getMessage());
        }

        try {
            var doctor = doctorServiceClient.getDoctorById(request.getDoctorId());
            if (doctor == null) {
                throw new ResourceNotFoundException("Doctor not found: " + request.getDoctorId());
            }
        } catch (RuntimeException ex) {
            throw new ResourceNotFoundException("Doctor validation failed: " + ex.getMessage());
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(request.getPatientId());
        appointment.setDoctorId(request.getDoctorId());
        appointment.setSlotId(request.getSlotId());

        // Temporary values until doctorService integration is added
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));

        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setPaymentStatus(PaymentStatus.NOT_INITIATED);
        appointment.setAmount(request.getAmount());
        appointment.setCurrency(request.getCurrency());

        // appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setReason(request.getReason());
        appointment.setNotes(request.getNotes());

        Appointment saved = appointmentRepository.save(appointment);
        return AppointmentMapper.toResponse(saved);
    }

    @Override
    public AppointmentResponse getAppointmentById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        return AppointmentMapper.toResponse(appointment);
    }

    @Override
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(AppointmentMapper::toResponse)
                .toList();
    }

    @Override
    public AppointmentResponse cancelAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Appointment is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentMapper.toResponse(updated);
    }

    private void validateCreateRequest(CreateAppointmentRequest request) {
        if (request.getPatientId() == null) {
            throw new BadRequestException("patientId is required");
        }

        if (request.getDoctorId() == null) {
            throw new BadRequestException("doctorId is required");
        }

        if (request.getSlotId() == null) {
            throw new BadRequestException("slotId is required");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("reason is required");
        }
    }

    @Override
    public java.util.Map<String, Object> initiatePaymentSession(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Payment can only be initiated for pending appointments");
        }

        // Call PaymentServiceClient to create actual Stripe session
        PaymentSessionRequest paymentRequest = new PaymentSessionRequest(
            appointmentId,
            appointment.getPatientId(),
            appointment.getAmount().toPlainString(),
            appointment.getCurrency(),
            "Appointment with doctor",
            appointment.getAppointmentDate().toString()
        );

        PaymentSessionResponse paymentResponse = paymentServiceClient.createPaymentSession(paymentRequest);

        // Store Stripe session ID in appointment
        if (paymentResponse != null && paymentResponse.getSessionId() != null) {
            appointment.setStripeSessionId(paymentResponse.getSessionId());
            appointment.setPaymentStatus(PaymentStatus.PENDING);
            appointmentRepository.save(appointment);
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("amount", appointment.getAmount());
        response.put("currency", appointment.getCurrency());
        if (paymentResponse != null) {
            response.put("sessionId", paymentResponse.getSessionId());
            response.put("checkoutUrl", paymentResponse.getCheckoutUrl());
            response.put("status", paymentResponse.getStatus());
        }
        return response;
    }

    @Override
    public void handlePaymentCallback(java.util.Map<String, Object> payload) {
        String appointmentIdStr = (String) payload.get("appointmentId");
        String paymentStatus = (String) payload.get("paymentStatus");
        String transactionId = (String) payload.get("transactionId");

        if (appointmentIdStr == null || paymentStatus == null) {
            throw new BadRequestException("Missing required fields in payment callback");
        }

        UUID appointmentId = UUID.fromString(appointmentIdStr);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if ("success".equalsIgnoreCase(paymentStatus)) {
            appointment.setPaymentStatus(PaymentStatus.COMPLETED);
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            if (transactionId != null) {
                appointment.setPaymentTransactionId(UUID.fromString(transactionId));
            }
        } else if ("failed".equalsIgnoreCase(paymentStatus)) {
            appointment.setPaymentStatus(PaymentStatus.FAILED);
            appointment.setStatus(AppointmentStatus.PAYMENT_FAILED);
        }

        appointmentRepository.save(appointment);
    }

    @Override
    public java.util.Map<String, Object> getAppointmentStatus(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("status", appointment.getStatus());
        response.put("paymentStatus", appointment.getPaymentStatus());
        response.put("amount", appointment.getAmount());
        response.put("currency", appointment.getCurrency());
        response.put("paymentTransactionId", appointment.getPaymentTransactionId());
        return response;
    }
}