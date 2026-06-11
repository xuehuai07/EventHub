package com.eventhub.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ActivityCoverStorageTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAndLoadsJpegWithGeneratedName() throws Exception {
        ActivityCoverStorage storage = storage();
        byte[] content = image("jpg");

        StoredImage stored = storage.store(new MockMultipartFile("file", "cover.jpg", "image/jpeg", content));
        StoredImage loaded = storage.load(stored.fileName());

        assertThat(stored.fileName()).matches("[a-f0-9-]{36}\\.jpg");
        assertThat(stored.width()).isEqualTo(40);
        assertThat(stored.height()).isEqualTo(24);
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        assertThat(loaded.path()).exists();
        assertThat(loaded.size()).isEqualTo(content.length);
    }

    @Test
    void detectsPngEvenWhenClientNameClaimsJpeg() throws Exception {
        ActivityCoverStorage storage = storage();

        StoredImage stored = storage.store(new MockMultipartFile("file", "cover.jpg", "image/jpeg", image("png")));

        assertThat(stored.fileName()).endsWith(".png");
        assertThat(stored.contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsTextDisguisedAsImage() {
        ActivityCoverStorage storage = storage();
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "not-an-image".getBytes());

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_INVALID_IMAGE));
    }

    @Test
    void rejectsOversizedFileBeforeDecoding() {
        ActivityCoverStorage storage = storage();
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_FILE_TOO_LARGE));
    }

    @Test
    void rejectsUnsafeFileName() {
        ActivityCoverStorage storage = storage();

        assertThatThrownBy(() -> storage.load("../secret.jpg"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_NOT_FOUND));
    }

    private ActivityCoverStorage storage() {
        return new ActivityCoverStorage(new MediaStorageProperties(temporaryDirectory.toString()));
    }

    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(40, 24, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(184, 75, 50));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
