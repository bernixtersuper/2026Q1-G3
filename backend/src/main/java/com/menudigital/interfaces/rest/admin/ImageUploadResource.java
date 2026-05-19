package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.infrastructure.storage.MenuImageUrls;
import com.menudigital.infrastructure.storage.S3ImageStorageService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

@Path("/api/admin/upload")
@Authenticated
public class ImageUploadResource {
    
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    @Inject
    S3ImageStorageService imageStorageService;

    @Inject
    MenuImageUrls menuImageUrls;
    
    @Inject
    TenantContext tenantContext;
    
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@FormParam("file") FileUpload file) {
        if (file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("NO_FILE", "No file provided"))
                .build();
        }
        
        String contentType = file.contentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_TYPE", "Only JPEG, PNG, GIF, and WebP images are allowed"))
                .build();
        }
        
        long fileSize;
        try {
            fileSize = Files.size(file.uploadedFile());
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("READ_ERROR", "Could not read file size"))
                .build();
        }
        
        if (fileSize > MAX_FILE_SIZE) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("FILE_TOO_LARGE", "File must be smaller than 5MB"))
                .build();
        }
        
        try (InputStream inputStream = Files.newInputStream(file.uploadedFile())) {
            String key = imageStorageService.upload(
                inputStream,
                contentType,
                fileSize,
                tenantContext.getTenantId().toString()
            );
            
            return Response.ok(new UploadResponse(menuImageUrls.toApiPath(key))).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("UPLOAD_FAILED", "Failed to upload image"))
                .build();
        }
    }
    
    public record UploadResponse(String url) {}
    public record ErrorResponse(String code, String message) {}
}
