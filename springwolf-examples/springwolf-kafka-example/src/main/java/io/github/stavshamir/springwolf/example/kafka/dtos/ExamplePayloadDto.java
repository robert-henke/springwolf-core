// SPDX-License-Identifier: Apache-2.0
package io.github.stavshamir.springwolf.example.kafka.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = """
        Example payload model with markdown description. This allows to use e.g.
        **bold**, *cursive* or <u>underlined</u> styled text.
        """
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExamplePayloadDto {
    @Schema(description = """
              
            Some string field with markdown description. This allows to use:
            		
            <h1>header</h1>
            		
            **bold**, *cursive*
            		
            ```
            code formatting
            ```
            
            also supports code highlighting for `json`, `http`, `java` languages
            
            ```json
            
            {
            	"key1":"value1",
            	"key2":"value2"
            }
            
            ```
            		
            <ul>
            <li>enumerations/lists</li>
            <li>other markdown styles</li>
            </ul>
            		
            """, example = "some string value", requiredMode = REQUIRED)
    private String someString;

    @Schema(description = "Some long field", example = "5")
    private long someLong;

    @Schema(description = "Some enum field", example = "FOO2", requiredMode = REQUIRED)
    private ExampleEnum someEnum;

    public enum ExampleEnum {
        FOO1,
        FOO2,
        FOO3
    }
}
