package com.eventhub.media;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media/activity-covers")
public class PublicMediaController {

    private final ActivityCoverStorage storage;

    public PublicMediaController(ActivityCoverStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/{fileName}")
    ResponseEntity<FileSystemResource> activityCover(@PathVariable String fileName) {
        StoredImage image = storage.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.size())
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(new FileSystemResource(image.path()));
    }
}
