package com.menudigital.interfaces.rest;

import com.menudigital.infrastructure.storage.S3ImageStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.InputStream;

@Path("/api/media")
@Tag(name = "Media", description = "Proxy de imágenes del menú (bucket S3 privado)")
public class MediaResource {

    @Inject
    S3ImageStorageService imageStorageService;

    @GET
    @Path("{objectKey:.*}")
    @Operation(summary = "Obtener imagen del menú", description = "Lee el objeto en S3 y lo sirve al navegador (sin URL pública al bucket).")
    public Response getImage(@PathParam("objectKey") String objectKey) {
        return imageStorageService.open(objectKey)
            .map(img -> {
                CacheControl cache = new CacheControl();
                cache.setMaxAge(86_400);
                var builder = Response.ok((InputStream) img.body())
                    .type(img.contentType())
                    .cacheControl(cache);
                if (img.contentLength() >= 0) {
                    builder.header("Content-Length", img.contentLength());
                }
                return builder.build();
            })
            .orElseThrow(NotFoundException::new);
    }
}
