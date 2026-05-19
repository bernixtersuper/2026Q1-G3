package com.menudigital.infrastructure.storage;

import java.io.InputStream;

public record StoredMenuImage(InputStream body, long contentLength, String contentType) {}
