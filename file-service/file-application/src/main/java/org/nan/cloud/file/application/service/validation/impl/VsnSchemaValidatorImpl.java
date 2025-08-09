package org.nan.cloud.file.application.service.validation.impl;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.service.validation.VsnSchemaValidator;
import org.nan.cloud.program.document.ProgramItem;
import org.nan.cloud.program.document.ProgramPage;
import org.nan.cloud.program.document.ProgramRegion;
import org.nan.cloud.program.document.VsnProgram;
import org.nan.cloud.program.vsn.DisplayRect;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class VsnSchemaValidatorImpl implements VsnSchemaValidator {
    @Override
    public void validate(List<VsnProgram> programs) {
        if (programs == null || programs.isEmpty()) {
            throw new IllegalArgumentException("Programs 不可为空");
        }
        for (VsnProgram program : programs) {
            if (program.getInformation() == null) {
                throw new IllegalArgumentException("Information 缺失");
            }
            if (isBlank(program.getInformation().getWidth()) || isBlank(program.getInformation().getHeight())) {
                throw new IllegalArgumentException("Information.Width/Height 必填");
            }
            if (program.getPages() == null || program.getPages().isEmpty()) {
                throw new IllegalArgumentException("Pages 不能为空");
            }
            for (ProgramPage page : program.getPages()) {
                // LoopType 必填；当为0时 AppointDuration 必填
                if (isBlank(page.getLoopType())) {
                    throw new IllegalArgumentException("Page.looptype 必填");
                }
                if ("0".equals(page.getLoopType()) && isBlank(page.getAppointDuration())) {
                    throw new IllegalArgumentException("Page.appointduration 在 looptype=0 时必填");
                }
                // 最低要求：背景色必填
                if (page.getBgColor() == null) {
                    throw new IllegalArgumentException("Page.bgColor 必填");
                }
                if (page.getRegions() == null || page.getRegions().isEmpty()) {
                    throw new IllegalArgumentException("Regions 不能为空");
                }
                for (ProgramRegion region : page.getRegions()) {
                    DisplayRect rect = region.getRect();
                    if (rect == null || isBlankInt(rect.getX()) || isBlankInt(rect.getY())
                            || isBlankInt(rect.getWidth()) || isBlankInt(rect.getHeight())
                            || isBlankInt(rect.getBorderWidth())) {
                        throw new IllegalArgumentException("Region.Rect X/Y/Width/Height/BorderWidth 必填");
                    }
                    if (isBlank(region.getName())) {
                        throw new IllegalArgumentException("Region.name 必填");
                    }
                    if (isBlank(region.getIsScheduleRegion())) {
                        throw new IllegalArgumentException("Region.isScheduleRegion 必填");
                    }
                    if (region.getItems() == null || region.getItems().isEmpty()) {
                        throw new IllegalArgumentException("Region.Items 不能为空");
                    }
                    // 同步节目限制
                    if ("sync_program".equalsIgnoreCase(region.getName())) {
                        for (ProgramItem item : region.getItems()) {
                            if (!List.of("2", "4", "6").contains(item.getType())) {
                                throw new IllegalArgumentException("同步区域仅支持 Item.type 2/4/6");
                            }
                        }
                    }
                    // 排程区域限制
                    if ("1".equals(region.getIsScheduleRegion())) {
                        for (ProgramItem item : region.getItems()) {
                            if (item.getSchedule() == null) {
                                throw new IllegalArgumentException("isScheduleRegion=1 时，Item.schedule 必填");
                            }
                        }
                    }
                    // 文本字体必填
                    for (ProgramItem item : region.getItems()) {
                        if (isBlank(item.getType())) {
                            throw new IllegalArgumentException("Item.type 必填");
                        }
                        if ("4".equals(item.getType()) || "5".equals(item.getType())) {
                            if (item.getLogFont() == null || isBlank(item.getLogFont().getLfHeight())) {
                                throw new IllegalArgumentException("文本类 Item 需要 LogFont.lfHeight");
                            }
                        }
                        if ("2".equals(item.getType())) {
                            if (item.getFileSource() == null) {
                                throw new IllegalArgumentException("图片 Item.filesource 必填");
                            }
                            if (isBlank(item.getAlpha())) {
                                throw new IllegalArgumentException("图片 Item.alpha 必填");
                            }
                        }
                        if ("3".equals(item.getType())) {
                            if (item.getFileSource() == null) {
                                throw new IllegalArgumentException("视频 Item.filesource 必填");
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private boolean isBlankInt(Integer v) { return v == null; }
}

