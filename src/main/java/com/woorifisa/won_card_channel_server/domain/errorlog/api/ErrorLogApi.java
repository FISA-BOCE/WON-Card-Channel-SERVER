package com.woorifisa.won_card_channel_server.domain.errorlog.api;

import com.woorifisa.won_card_channel_server.domain.errorlog.dto.response.ErrorLogResponse;
import com.woorifisa.won_card_channel_server.global.logging.ErrorLogService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/error-logs")
@RequiredArgsConstructor
public class ErrorLogApi {

    private final ErrorLogService errorLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ErrorLogResponse>>> getErrorLogs(
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ErrorLogResponse> result = status != null
                ? errorLogService.findByStatus(status, pageable).map(ErrorLogResponse::from)
                : errorLogService.findAll(pageable).map(ErrorLogResponse::from);

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.OK, result));
    }
}
