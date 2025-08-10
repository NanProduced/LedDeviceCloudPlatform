package org.nan.cloud.file.infrastructure.vsn.impl;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.port.StreamingVsnXmlWriter;
import org.nan.cloud.program.document.ProgramItem;
import org.nan.cloud.program.document.ProgramPage;
import org.nan.cloud.program.document.ProgramRegion;
import org.nan.cloud.program.document.VsnProgram;
import org.nan.cloud.program.vsn.DisplayRect;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.nan.cloud.file.infrastructure.vsn.VsnFormatUtils.*;

/**
 * 流式 VSN XML 写入器实现
 * 直接将XML内容写入输出流，避免大XML字符串在内存中存在
 */
@Slf4j
@Component
public class StreamingVsnXmlWriterImpl implements StreamingVsnXmlWriter {
    
    private static final int BUFFER_SIZE = 16384; // 16KB 缓冲区
    
    @Override
    public long writeToStream(List<VsnProgram> programs, OutputStream outputStream) throws Exception {
        long totalBytes = 0;
        
        // 使用缓冲输出流和字符流写入器提升性能
        try (BufferedOutputStream bufferedOut = new BufferedOutputStream(outputStream, BUFFER_SIZE);
             OutputStreamWriter writer = new OutputStreamWriter(bufferedOut, StandardCharsets.UTF_8)) {
            
            totalBytes += writeString(writer, "<Programs>");
            
            for (VsnProgram program : programs) {
                totalBytes += writeProgramToStream(writer, program);
            }
            
            totalBytes += writeString(writer, "</Programs>");
            
            // 确保所有数据写入
            writer.flush();
            bufferedOut.flush();
            
            log.debug("流式写入VSN XML完成，总字节数: {}", totalBytes);
        }
        
        return totalBytes;
    }
    
    private long writeProgramToStream(Writer writer, VsnProgram program) throws Exception {
        long bytes = 0;
        
        bytes += writeString(writer, "<Program>");
        
        // Information
        bytes += writeString(writer, "<Information>");
        bytes += writeTag(writer, "Height", program.getInformation().getHeight());
        bytes += writeTag(writer, "Width", program.getInformation().getWidth());
        bytes += writeString(writer, "</Information>");
        
        // Pages
        bytes += writeString(writer, "<Pages>");
        for (ProgramPage page : program.getPages()) {
            bytes += writePageToStream(writer, page);
        }
        bytes += writeString(writer, "</Pages>");
        
        bytes += writeString(writer, "</Program>");
        
        return bytes;
    }
    
    private long writePageToStream(Writer writer, ProgramPage page) throws Exception {
        long bytes = 0;
        
        bytes += writeString(writer, "<Page>");
        
        if (page.getAppointDuration() != null) {
            bytes += writeTag(writer, "AppointDuration", page.getAppointDuration());
        }
        if (page.getLoopType() != null) {
            bytes += writeTag(writer, "LoopType", page.getLoopType());
        }
        if (page.getBgColor() != null) {
            bytes += writeTag(writer, "bgColor", colorFromInt(page.getBgColor()));
        }
        
        // Regions
        bytes += writeString(writer, "<Regions>");
        if (page.getRegions() != null) {
            for (ProgramRegion region : page.getRegions()) {
                bytes += writeRegionToStream(writer, region);
            }
        }
        bytes += writeString(writer, "</Regions>");
        
        bytes += writeString(writer, "</Page>");
        
        return bytes;
    }
    
    private long writeRegionToStream(Writer writer, ProgramRegion region) throws Exception {
        long bytes = 0;
        
        bytes += writeString(writer, "<Region>");
        
        // Rect
        DisplayRect rect = region.getRect();
        if (rect != null) {
            bytes += writeString(writer, "<Rect>");
            bytes += writeTag(writer, "X", String.valueOf(rect.getX()));
            bytes += writeTag(writer, "Y", String.valueOf(rect.getY()));
            bytes += writeTag(writer, "Height", String.valueOf(rect.getHeight()));
            bytes += writeTag(writer, "Width", String.valueOf(rect.getWidth()));
            bytes += writeTag(writer, "BorderWidth", String.valueOf(rect.getBorderWidth()));
            bytes += writeString(writer, "</Rect>");
        }
        
        if (region.getName() != null) {
            bytes += writeTag(writer, "Name", region.getName());
        }
        if (region.getIsScheduleRegion() != null) {
            bytes += writeTag(writer, "isScheduleRegion", ensure01(region.getIsScheduleRegion()));
        }
        if (region.getLayer() != null) {
            bytes += writeTag(writer, "layer", region.getLayer());
        }
        
        // Items
        bytes += writeString(writer, "<Items>");
        if (region.getItems() != null) {
            for (ProgramItem item : region.getItems()) {
                bytes += writeItemToStream(writer, item);
            }
        }
        bytes += writeString(writer, "</Items>");
        
        bytes += writeString(writer, "</Region>");
        
        return bytes;
    }
    
    private long writeItemToStream(Writer writer, ProgramItem item) throws Exception {
        long bytes = 0;
        
        bytes += writeString(writer, "<Item>");
        
        if (item.getType() != null) {
            bytes += writeTag(writer, "Type", item.getType());
        }
        if (item.getTextColor() != null) {
            bytes += writeTag(writer, "TextColor", item.getTextColor());
        }
        if (item.getText() != null) {
            bytes += writeTag(writer, "Text", item.getText());
        }
        
        // LogFont
        if (item.getLogFont() != null && item.getLogFont().getLfHeight() != null) {
            bytes += writeString(writer, "<LogFont>");
            bytes += writeTag(writer, "lfHeight", item.getLogFont().getLfHeight());
            if (item.getLogFont().getLfWeight() != null) {
                bytes += writeTag(writer, "lfWeight", item.getLogFont().getLfWeight());
            }
            if (item.getLogFont().getLfItalic() != null) {
                bytes += writeTag(writer, "lfItalic", item.getLogFont().getLfItalic());
            }
            if (item.getLogFont().getLfUnderline() != null) {
                bytes += writeTag(writer, "lfUnderline", item.getLogFont().getLfUnderline());
            }
            if (item.getLogFont().getLfFaceName() != null) {
                bytes += writeTag(writer, "lfFaceName", item.getLogFont().getLfFaceName());
            }
            bytes += writeString(writer, "</LogFont>");
        }
        
        if (item.getDuration() != null) {
            bytes += writeTag(writer, "duration", item.getDuration());
        }
        if (item.getAlpha() != null) {
            bytes += writeTag(writer, "alpha", item.getAlpha());
        }
        if (item.getCenterAlign() != null) {
            bytes += writeTag(writer, "centeralalign", item.getCenterAlign());
        }
        if (item.getIsScroll() != null) {
            bytes += writeTag(writer, "isscroll", ensure01(item.getIsScroll()));
        }
        
        // filesource
        if (item.getFileSource() != null) {
            var fs = item.getFileSource();
            bytes += writeString(writer, "<filesource>");
            if (fs.getIsRelative() != null) {
                bytes += writeTag(writer, "isrelative", ensure01(fs.getIsRelative()));
            }
            if (fs.getFilePath() != null) {
                bytes += writeTag(writer, "filepath", fs.getFilePath());
            }
            if (fs.getMd5() != null) {
                bytes += writeTag(writer, "MD5", fs.getMd5());
            }
            if (fs.getOriginName() != null) {
                bytes += writeTag(writer, "originName", fs.getOriginName());
            }
            if (fs.getConvertPath() != null) {
                bytes += writeTag(writer, "convertPath", fs.getConvertPath());
            }
            bytes += writeString(writer, "</filesource>");
        }
        
        // schedule
        if (item.getSchedule() != null) {
            var sc = item.getSchedule();
            bytes += writeString(writer, "<schedule>");
            if (sc.getIsLimitTime() != null) {
                bytes += writeTag(writer, "isLimitTime", sc.getIsLimitTime());
            }
            if (sc.getStartTime() != null) {
                bytes += writeTag(writer, "startTime", sc.getStartTime());
            }
            if (sc.getEndTime() != null) {
                bytes += writeTag(writer, "endTime", sc.getEndTime());
            }
            if (sc.getIsLimitDate() != null) {
                bytes += writeTag(writer, "isLimitDate", sc.getIsLimitDate());
            }
            if (sc.getStartDay() != null) {
                bytes += writeTag(writer, "startDay", sc.getStartDay());
            }
            if (sc.getStartDayTime() != null) {
                bytes += writeTag(writer, "startDayTime", sc.getStartDayTime());
            }
            if (sc.getEndDay() != null) {
                bytes += writeTag(writer, "endDay", sc.getEndDay());
            }
            if (sc.getEndDayTime() != null) {
                bytes += writeTag(writer, "endDayTime", sc.getEndDayTime());
            }
            if (sc.getIsLimitWeek() != null) {
                bytes += writeTag(writer, "isLimitWeek", sc.getIsLimitWeek());
            }
            if (sc.getLimitWeek() != null) {
                bytes += writeTag(writer, "limitWeek", sc.getLimitWeek());
            }
            bytes += writeString(writer, "</schedule>");
        }
        
        // ineffect
        if (item.getInEffect() != null) {
            var ef = item.getInEffect();
            bytes += writeString(writer, "<ineffect>");
            if (ef.getType() != null) {
                bytes += writeTag(writer, "Type", ef.getType());
            }
            if (ef.getTime() != null) {
                bytes += writeTag(writer, "Time", ef.getTime());
            }
            bytes += writeString(writer, "</ineffect>");
        }
        
        bytes += writeString(writer, "</Item>");
        
        return bytes;
    }
    
    private long writeTag(Writer writer, String name, String value) throws Exception {
        String tag = "<" + name + ">" + escape(value) + "</" + name + ">";
        return writeString(writer, tag);
    }
    
    private long writeString(Writer writer, String content) throws Exception {
        writer.write(content);
        // UTF-8编码下估算字节数（实际可能略有差异）
        return content.getBytes(StandardCharsets.UTF_8).length;
    }
    
    private String escape(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}