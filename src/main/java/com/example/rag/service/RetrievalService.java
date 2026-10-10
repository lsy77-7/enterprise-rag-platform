package com.example.rag.service;

import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RetrievalSearchResponse;

import java.util.List;

public interface RetrievalService {

    List<RetrievalSearchResponse> search(RetrievalSearchRequest request);
}
