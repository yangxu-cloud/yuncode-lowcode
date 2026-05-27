package com.yuncode.system.dto;

import com.yuncode.system.entity.SysBoField;
import lombok.Data;

import java.util.List;

@Data
public class BoDesignSaveDTO {
    private List<SysBoField> fields;
    private String indexes;
}
