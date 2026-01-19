package ru.valera.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import ru.valera.domain.storage.ImageStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Реализация хранилища изображений на основе файловой системы
 */
@Component
@Slf4j
public class FileSystemImageStorage implements ImageStorage {
    
    private final Resource imagesDir;
    
    public FileSystemImageStorage(@Value("classpath:images") Resource imagesDir) {
        this.imagesDir = imagesDir;
    }
    
    @Override
    public void save(String fileName, byte[] data) {
        try {
            Path targetPath = getProtectedPath(fileName);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, data);
            log.info("Image successfully saved: {}", fileName);
        } catch (IOException e) {
            throw new StorageException("Failed to save image: " + fileName, e);
        }
    }
    
    @Override
    public Optional<byte[]> load(String fileName) {
        try {
            Path targetPath = getProtectedPath(fileName);
            if (Files.exists(targetPath)) {
                return Optional.of(Files.readAllBytes(targetPath));
            }
            return Optional.empty();
        } catch (SecurityException e) {
            log.warn("Security exception for file: {}", fileName);
            return Optional.empty();
        } catch (IOException e) {
            throw new StorageException("Failed to load image: " + fileName, e);
        }
    }
    
    private Path getProtectedPath(String fileName) throws IOException {
        Path basePath = Paths.get(imagesDir.getURI());
        Path targetPath = basePath.resolve(fileName).normalize();
        
        // Проверка на path traversal
        if (!targetPath.startsWith(basePath)) {
            throw new SecurityException("Path traversal attempt: " + fileName);
        }
        
        return targetPath;
    }
}
