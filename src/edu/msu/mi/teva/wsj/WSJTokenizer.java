package edu.msu.mi.teva.wsj;

import edu.mit.cci.text.preprocessing.AlphaNumericTokenizer;
import edu.mit.cci.text.preprocessing.CompositeMunger;
import edu.mit.cci.text.preprocessing.Munger;
/**
 * Created by charlesbean on 3/25/14.
 */
public class WSJTokenizer extends AlphaNumericTokenizer {
    public WSJTokenizer(Munger... mungers) {
        super(mungers);
    }

    public String replace(String input) {
        return super.replace(input.replace("_",""));
    }
}
