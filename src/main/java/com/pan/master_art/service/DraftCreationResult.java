package com.pan.master_art.service;

import com.pan.master_art.dto.DraftView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 草稿创建结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftCreationResult {
    /** 草稿视图（含幂等键） */
    private DraftView view;
    /** true = 幂等重放（复用已有草稿），false = 新创建 */
    private boolean replayed;
}
