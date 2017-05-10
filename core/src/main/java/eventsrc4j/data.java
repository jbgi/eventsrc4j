package eventsrc4j;

import fj.Equal;
import fj.Hash;
import fj.Ord;
import fj.Show;
import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.Instances;

import static org.derive4j.Flavour.FJ;

@Data(flavour = FJ)
@Derive(@Instances({Equal.class, Hash.class, Show.class, Ord.class, }))
public @interface data {
}
