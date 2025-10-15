package org.nan.cloud.core.api.DTO.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SingleCommandResult {

    private Boolean success;

    private String commandId;
}
