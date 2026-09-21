package com.yuncode.system.app.dto;

import com.yuncode.system.app.entity.SysBoField;
import lombok.Data;

import java.util.List;

@Data
public class BoDesignSaveDTO {
    private List<SysBoField> fields;
    private String indexes;
}
