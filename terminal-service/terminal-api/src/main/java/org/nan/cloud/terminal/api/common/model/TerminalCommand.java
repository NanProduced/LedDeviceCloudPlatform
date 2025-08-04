package org.nan.cloud.terminal.api.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "终端指令封装")
public class TerminalCommand {

    @Schema(description = "指令确认ID")
    private Integer id;

    @Schema(description = "终端Id，设备不处理")
    private Integer post;

    @Schema(description = "指令操作类型", example = "api/brightness")
    @JsonProperty("author_url")
    private String authorUrl;
    
    @Schema(description = "发送指令的用户ID")
    private Long uid;

    @Schema(description = "指令对象")
    private Content content;

    /**
     * 屏幕执行方式<p>
     * 0-get, 1-post, 2-put,3-delete
     */
    @Schema(description = "终端屏幕执行方式", example = "0|1|2|3")
    private Integer karma;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "指令对象封装")
    public static class Content {

        @Schema(description = "指令对应的json,{}为空")
        private String raw;
    }

    /* ========================== 构造方法 =============================== */

    /**
     * 亮度指令
     * @param tid
     * @param brightness
     * @return
     */
    public static TerminalCommand brightnessCommand(Long tid, Integer brightness) {
        return TerminalCommand.builder()
                .post(tid.intValue())
                .authorUrl("api/brightness")
                .karma(2)
                .content(new TerminalCommand.Content("{\"brightness\":" + brightness / 100 * 255 +"}"))
                .build();
    }

    /**
     * 色温指令
     * @param tid
     * @param colorTemp
     * @return
     */
    public static TerminalCommand colorTempCommand(Long tid, Integer colorTemp) {
        return TerminalCommand.builder()
                .post(tid.intValue())
                .authorUrl("api/colortemp")
                .karma(2)
                .content(new TerminalCommand.Content("{\"colorTemp\":" + colorTemp + "}"))
                .build();
    }

    /**
     * 音量指令
     * @param tid
     * @param volume
     * @return
     */
    public static TerminalCommand volumeCommand(Long tid, Integer volume) {
        return TerminalCommand.builder()
                .post(tid.intValue())
                .authorUrl("api/volume")
                .karma(2)
                .content(new TerminalCommand.Content("{\"musicvolume\":" + volume + "}"))
                .build();
    }

    /**
     * 休眠指令
     * @param tid
     * @return
     */
    public static TerminalCommand sleepCommand(Long tid) {
        return TerminalCommand.builder()
                .post(tid.intValue())
                .authorUrl("api/action")
                .karma(1)
                .content(new TerminalCommand.Content("{\"command\":\"sleep\"}"))
                .build();
    }

    /**
     * 唤醒指令
     * @param tid
     * @return
     */
    public static TerminalCommand wakeupCommand(Long tid) {
        return TerminalCommand.builder()
                .post(tid.intValue())
                .authorUrl("api/action")
                .karma(1)
                .content(new TerminalCommand.Content("{\"command\":\"wakeup\"}"))
                .build();
    }

}
