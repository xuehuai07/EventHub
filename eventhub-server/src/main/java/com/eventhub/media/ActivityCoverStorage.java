package com.eventhub.media;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@EnableConfigurationProperties(MediaStorageProperties.class)
public class ActivityCoverStorage {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 6000;
    private static final Pattern FILE_NAME = Pattern.compile("[a-f0-9-]{36}\\.(jpg|png)");

    private final Path coverRoot;

    public ActivityCoverStorage(MediaStorageProperties properties) {
        String uploadRoot =
                properties.uploadRoot() == null || properties.uploadRoot().isBlank()
                        ? ".data/uploads"
                        : properties.uploadRoot();
        this.coverRoot = Path.of(uploadRoot).toAbsolutePath().normalize().resolve("activity-covers");
    }

    public StoredImage store(MultipartFile file) {
        validateFile(file);
        ImageMetadata metadata = readMetadata(file);
        String fileName = UUID.randomUUID() + metadata.extension();
        Path target = resolve(fileName);
        Path temporary = null;
        try {
            Files.createDirectories(coverRoot);
            temporary = Files.createTempFile(coverRoot, ".upload-", ".tmp");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, target);
            return new StoredImage(
                    fileName, target, metadata.contentType(), file.getSize(), metadata.width(), metadata.height());
        } catch (IOException exception) {
            deleteQuietly(temporary);
            throw new BusinessException(ErrorCode.MEDIA_STORAGE_FAILED);
        }
    }

    public StoredImage load(String fileName) {
        if (fileName == null
                || !FILE_NAME.matcher(fileName.toLowerCase(Locale.ROOT)).matches()) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        Path file = resolve(fileName.toLowerCase(Locale.ROOT));
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        try {
            String contentType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
            return new StoredImage(fileName, file, contentType, Files.size(file), 0, 0);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.MEDIA_INVALID_IMAGE, "请选择 JPG 或 PNG 图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_TOO_LARGE);
        }
    }

    private ImageMetadata readMetadata(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new BusinessException(ErrorCode.MEDIA_INVALID_IMAGE);
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                throw new BusinessException(ErrorCode.MEDIA_INVALID_IMAGE, "图片尺寸不能超过 6000 × 6000");
            }
            String format = detectFormat(file);
            return switch (format) {
                case "jpg", "jpeg" -> new ImageMetadata(".jpg", "image/jpeg", width, height);
                case "png" -> new ImageMetadata(".png", "image/png", width, height);
                default -> throw new BusinessException(ErrorCode.MEDIA_INVALID_IMAGE);
            };
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.MEDIA_INVALID_IMAGE);
        }
    }

    private String detectFormat(MultipartFile file) throws IOException {
        try (var imageInput = ImageIO.createImageInputStream(file.getInputStream())) {
            if (imageInput == null) {
                return "";
            }
            var readers = ImageIO.getImageReaders(imageInput);
            return readers.hasNext() ? readers.next().getFormatName().toLowerCase(Locale.ROOT) : "";
        }
    }

    private Path resolve(String fileName) {
        Path resolved = coverRoot.resolve(fileName).normalize();
        if (!resolved.startsWith(coverRoot)) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        return resolved;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The failed temporary upload will be cleaned by routine maintenance.
        }
    }

    private record ImageMetadata(String extension, String contentType, int width, int height) {}
}
