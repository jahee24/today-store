package today_store.content.content.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import today_store.common.runcomfy.dto.RunComfyResultResponse;
import today_store.content.content.service.ComfyWebhookService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class ComfyWebhookController {

    private final ComfyWebhookService comfyWebhookService;

    @PostMapping("/comfy")
    public ResponseEntity<Void> receiveWebhook(
            @RequestParam("apiLogId") UUID apiLogId,
            @RequestParam("inputImageId") UUID inputImageId,
            @RequestBody RunComfyResultResponse payload) {

        log.info("Received RunComfy webhook callback for apiLogId: {}, inputImageId: {}", apiLogId, inputImageId);

        comfyWebhookService.handleWebhook(apiLogId, inputImageId, payload);

        return ResponseEntity.ok().build();
    }
}
