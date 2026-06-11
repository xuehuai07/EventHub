package com.eventhub.media;

import com.eventhub.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/merchant/uploads")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantMediaController {

    private final ActivityCoverStorage storage;

    public MerchantMediaController(ActivityCoverStorage storage) {
        this.storage = storage;
    }

    @Operation(summary = "上传活动封面")
    @PostMapping(value = "/activity-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<ActivityCoverUploadView> uploadActivityCover(@RequestPart("file") MultipartFile file) {
        StoredImage image = storage.store(file);
        return ApiResponse.success(new ActivityCoverUploadView(
                "/api/media/activity-covers/" + image.fileName(),
                image.fileName(),
                image.size(),
                image.width(),
                image.height()));
    }
}
