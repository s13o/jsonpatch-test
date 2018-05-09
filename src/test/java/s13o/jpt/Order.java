package s13o.jpt;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Order {
    private int id;
    private int version;
    private String name;
}
