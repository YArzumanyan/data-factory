package cz.cuni.mff.df_manager.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service for interacting with the artifact repository.
 */
public interface ArtifactRepositoryService {
    
    /**
     * Uploads an artifact file to the artifact repository.
     *
     * @param file The artifact file to upload
     * @return The artifact ID assigned by the repository
     */
    String uploadArtifact(MultipartFile file);
}