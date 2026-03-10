package com.aurora.iotonenet.api.dto;

import java.util.ArrayList;
import java.util.List;

public class PlatformLogSummaryDTO {

    private Long count;
    private String file;
    private List<String> supportedTypes = new ArrayList<>();

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public List<String> getSupportedTypes() {
        return supportedTypes;
    }

    public void setSupportedTypes(List<String> supportedTypes) {
        this.supportedTypes = supportedTypes;
    }
}
