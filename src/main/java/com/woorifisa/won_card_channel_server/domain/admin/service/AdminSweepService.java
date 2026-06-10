package com.woorifisa.won_card_channel_server.domain.admin.service;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.admin.external.CardCoreAdminSweepApi;
import com.woorifisa.won_card_channel_server.domain.admin.external.dto.CardCoreAdminSweepRequestItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.external.dto.CardCoreAdminSweepRequestListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.external.dto.CardCoreAdminSweepRequestSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepRepository;
import com.woorifisa.won_card_channel_server.global.exception.code.CommonErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSweepService {

    private final CardCoreAdminSweepApi cardCoreAdminSweepApi;
    private final SweepRepository sweepRepository;

    public AdminSweepRequestListResponse getSweepRequests(
            String status,
            String baseMonth,
            UUID cardUserUuid,
            Long sweepRequestId,
            int page,
            int size
    ) {
        CardCoreAdminSweepRequestListResponse coreResponse = callCardCore(() -> cardCoreAdminSweepApi.getSweepRequests(
                mapRequestStatusToCoreStatus(status),
                baseMonth,
                cardUserUuid,
                sweepRequestId,
                page,
                size
        ));

        Map<Long, Sweep> sweepsByPointLedgerId = sweepRepository.findByPointLedgerIdIn(
                        coreResponse.items()
                                .stream()
                                .map(CardCoreAdminSweepRequestItemResponse::pointLedgerId)
                                .filter(Objects::nonNull)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(Sweep::getPointLedgerId, Function.identity(), (left, right) -> left));

        return new AdminSweepRequestListResponse(
                mapSummary(coreResponse.summary()),
                coreResponse.items()
                        .stream()
                        .map(item -> AdminSweepRequestItemResponse.from(item, sweepsByPointLedgerId.get(item.pointLedgerId())))
                        .toList(),
                coreResponse.page(),
                coreResponse.size(),
                coreResponse.totalCount(),
                coreResponse.totalPages()
        );
    }

    public AdminSweepRequestItemResponse getSweepRequest(Long sweepRequestId) {
        Sweep sweep = sweepRepository.findById(sweepRequestId).orElse(null);
        Long coreSweepRequestId = sweep == null ? sweepRequestId : sweep.getPointLedgerId();

        CardCoreAdminSweepRequestItemResponse coreResponse = callCardCore(
                () -> cardCoreAdminSweepApi.getSweepRequest(coreSweepRequestId)
        );

        Sweep resolvedSweep = sweep == null
                ? sweepRepository.findByPointLedgerId(coreResponse.pointLedgerId()).orElse(null)
                : sweep;

        return AdminSweepRequestItemResponse.from(coreResponse, resolvedSweep);
    }

    private AdminSweepRequestSummaryResponse mapSummary(CardCoreAdminSweepRequestSummaryResponse summary) {
        return new AdminSweepRequestSummaryResponse(
                summary.totalCount(),
                summary.createdCount(),
                summary.processingCount(),
                summary.completedCount(),
                summary.failedCount()
        );
    }

    private String mapRequestStatusToCoreStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        return switch (status) {
            case "CREATED" -> "NONE";
            case "PROCESSING" -> "REQUESTED";
            default -> status;
        };
    }

    private <T> T callCardCore(Supplier<ApiResponse<T>> supplier) {
        try {
            ApiResponse<T> response = supplier.get();

            if (response == null || response.data() == null) {
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Card Core 관리자 조회 응답이 비어 있습니다.");
            }

            return response.data();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Card Core 관리자 조회에 실패했습니다.", e);
        }
    }
}
