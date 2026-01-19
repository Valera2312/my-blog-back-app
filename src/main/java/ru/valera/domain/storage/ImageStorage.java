package ru.valera.domain.storage;

import java.util.Optional;

/**
 * Интерфейс для хранения и загрузки изображений
 */
public interface ImageStorage {
    /**
     * Сохраняет изображение с указанным именем файла
     * 
     * @param fileName имя файла
     * @param data данные изображения
     * @throws StorageException если произошла ошибка при сохранении
     */
    void save(String fileName, byte[] data);
    
    /**
     * Загружает изображение по имени файла
     * 
     * @param fileName имя файла
     * @return данные изображения, если файл существует
     * @throws StorageException если произошла ошибка при загрузке
     */
    Optional<byte[]> load(String fileName);
    
    /**
     * Исключение для ошибок хранилища
     */
    class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public StorageException(String message) {
            super(message);
        }
    }
}
