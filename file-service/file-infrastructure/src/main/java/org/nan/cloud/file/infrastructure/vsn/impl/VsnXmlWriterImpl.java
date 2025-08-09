package org.nan.cloud.file.infrastructure.vsn.impl;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.port.VsnXmlWriter;
import org.nan.cloud.program.document.ProgramItem;
import org.nan.cloud.program.document.ProgramPage;
import org.nan.cloud.program.document.ProgramRegion;
import org.nan.cloud.program.document.VsnProgram;
import org.nan.cloud.program.vsn.DisplayRect;
import org.springframework.stereotype.Component;
import static org.nan.cloud.file.infrastructure.vsn.VsnFormatUtils.*;

import java.util.List;

@Slf4j
@Component
public class VsnXmlWriterImpl implements VsnXmlWriter {
    @Override
    public String write(List<VsnProgram> programs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Programs>");
        for (VsnProgram program : programs) {
            sb.append("<Program>");
            // Information
            sb.append("<Information>");
            sb.append(tag("Height", program.getInformation().getHeight()));
            sb.append(tag("Width", program.getInformation().getWidth()));
            sb.append("</Information>");
            // Pages
            sb.append("<Pages>");
            for (ProgramPage page : program.getPages()) {
                sb.append("<Page>");
                if (page.getAppointDuration() != null) sb.append(tag("AppointDuration", page.getAppointDuration()));
                if (page.getLoopType() != null) sb.append(tag("LoopType", page.getLoopType()));
                if (page.getBgColor() != null) sb.append(tag("bgColor", colorFromInt(page.getBgColor())));
                // Regions
                sb.append("<Regions>");
                if (page.getRegions() != null) {
                    for (ProgramRegion region : page.getRegions()) {
                        sb.append("<Region>");
                        // Rect
                        DisplayRect rect = region.getRect();
                        if (rect != null) {
                            sb.append("<Rect>");
                            sb.append(tag("X", String.valueOf(rect.getX())));
                            sb.append(tag("Y", String.valueOf(rect.getY())));
                            sb.append(tag("Height", String.valueOf(rect.getHeight())));
                            sb.append(tag("Width", String.valueOf(rect.getWidth())));
                            sb.append(tag("BorderWidth", String.valueOf(rect.getBorderWidth())));
                            sb.append("</Rect>");
                        }
                        if (region.getName() != null) sb.append(tag("Name", region.getName()));
                        if (region.getIsScheduleRegion() != null) sb.append(tag("isScheduleRegion", ensure01(region.getIsScheduleRegion())));
                        if (region.getLayer() != null) sb.append(tag("layer", region.getLayer()));
                        // Items
                        sb.append("<Items>");
                        if (region.getItems() != null) {
                            for (ProgramItem item : region.getItems()) {
                                sb.append("<Item>");
                                if (item.getType() != null) sb.append(tag("Type", item.getType()));
                                if (item.getTextColor() != null) sb.append(tag("TextColor", item.getTextColor()));
                                if (item.getText() != null) sb.append(tag("Text", item.getText()));
                                if (item.getLogFont() != null && item.getLogFont().getLfHeight() != null) {
                                    sb.append("<LogFont>");
                                    sb.append(tag("lfHeight", item.getLogFont().getLfHeight()));
                                    if (item.getLogFont().getLfWeight() != null) sb.append(tag("lfWeight", item.getLogFont().getLfWeight()));
                                    if (item.getLogFont().getLfItalic() != null) sb.append(tag("lfItalic", item.getLogFont().getLfItalic()));
                                    if (item.getLogFont().getLfUnderline() != null) sb.append(tag("lfUnderline", item.getLogFont().getLfUnderline()));
                                    if (item.getLogFont().getLfFaceName() != null) sb.append(tag("lfFaceName", item.getLogFont().getLfFaceName()));
                                    sb.append("</LogFont>");
                                }
                                if (item.getDuration() != null) sb.append(tag("duration", item.getDuration()));
                                if (item.getAlpha() != null) sb.append(tag("alpha", item.getAlpha()));
                                if (item.getCenterAlign() != null) sb.append(tag("centeralalign", item.getCenterAlign()));
                                if (item.getIsScroll() != null) sb.append(tag("isscroll", ensure01(item.getIsScroll())));
                                // filesource
                                if (item.getFileSource() != null) {
                                    var fs = item.getFileSource();
                                    sb.append("<filesource>");
                                    if (fs.getIsRelative() != null) sb.append(tag("isrelative", ensure01(fs.getIsRelative())));
                                    if (fs.getFilePath() != null) sb.append(tag("filepath", fs.getFilePath()));
                                    if (fs.getMd5() != null) sb.append(tag("MD5", fs.getMd5()));
                                    if (fs.getOriginName() != null) sb.append(tag("originName", fs.getOriginName()));
                                    if (fs.getConvertPath() != null) sb.append(tag("convertPath", fs.getConvertPath()));
                                    sb.append("</filesource>");
                                }
                                // schedule
                                if (item.getSchedule() != null) {
                                    var sc = item.getSchedule();
                                    sb.append("<schedule>");
                                    if (sc.getIsLimitTime() != null) sb.append(tag("isLimitTime", sc.getIsLimitTime()));
                                    if (sc.getStartTime() != null) sb.append(tag("startTime", sc.getStartTime()));
                                    if (sc.getEndTime() != null) sb.append(tag("endTime", sc.getEndTime()));
                                    if (sc.getIsLimitDate() != null) sb.append(tag("isLimitDate", sc.getIsLimitDate()));
                                    if (sc.getStartDay() != null) sb.append(tag("startDay", sc.getStartDay()));
                                    if (sc.getStartDayTime() != null) sb.append(tag("startDayTime", sc.getStartDayTime()));
                                    if (sc.getEndDay() != null) sb.append(tag("endDay", sc.getEndDay()));
                                    if (sc.getEndDayTime() != null) sb.append(tag("endDayTime", sc.getEndDayTime()));
                                    if (sc.getIsLimitWeek() != null) sb.append(tag("isLimitWeek", sc.getIsLimitWeek()));
                                    if (sc.getLimitWeek() != null) sb.append(tag("limitWeek", sc.getLimitWeek()));
                                    sb.append("</schedule>");
                                }
                                // ineffect
                                if (item.getInEffect() != null) {
                                    var ef = item.getInEffect();
                                    sb.append("<ineffect>");
                                    if (ef.getType() != null) sb.append(tag("Type", ef.getType()));
                                    if (ef.getTime() != null) sb.append(tag("Time", ef.getTime()));
                                    sb.append("</ineffect>");
                                }
                                sb.append("</Item>");
                            }
                        }
                        sb.append("</Items>");
                        sb.append("</Region>");
                    }
                }
                sb.append("</Regions>");
                sb.append("</Page>");
            }
            sb.append("</Pages>");
            sb.append("</Program>");
        }
        sb.append("</Programs>");
        return sb.toString();
    }

    private String tag(String name, String value) {
        return "<" + name + ">" + escape(value) + "</" + name + ">";
    }

    private String escape(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

