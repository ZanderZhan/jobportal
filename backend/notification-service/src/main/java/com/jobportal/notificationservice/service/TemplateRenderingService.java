package com.jobportal.notificationservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateRenderingService {

    // A small token replacement is enough for the current assignment scope.
    public String render(String template, Map<String, String> templateData) {
        String rendered = template;
        for (Map.Entry<String, String> entry : templateData.entrySet()) {
            rendered = rendered.replace("#{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }
}
