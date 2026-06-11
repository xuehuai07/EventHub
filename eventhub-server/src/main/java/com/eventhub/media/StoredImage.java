package com.eventhub.media;

import java.nio.file.Path;

public record StoredImage(String fileName, Path path, String contentType, long size, int width, int height) {}
