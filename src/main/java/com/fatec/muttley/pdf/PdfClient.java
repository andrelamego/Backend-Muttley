package com.fatec.muttley.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class PdfClient {

    private final RestClient restClient;

    public PdfClient(@Value("${pdf.ms-url}") String pdfMsUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(pdfMsUrl)
                .build();
    }

    public byte[] gerarPdf(String htmlContent) {
        return restClient.post()
                .uri("/api/pdf/gerar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("htmlContent", htmlContent))
                .retrieve()
                .body(byte[].class);
    }
}
