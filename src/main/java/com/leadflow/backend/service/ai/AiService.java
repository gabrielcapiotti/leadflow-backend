package com.leadflow.backend.service.ai;

public interface AiService {

    /**
     * Executa um prompt no modelo de IA configurado.
     *
     * @param prompt 
     * @return 
     */
    String generate(String prompt);

}