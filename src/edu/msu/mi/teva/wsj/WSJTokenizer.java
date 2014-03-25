package edu.msu.mi.teva.wsj;

import
/**
 * Created by charlesbean on 3/25/14.
 */
public class WSJTokenizer extends AlphaNumericTokenizer {
    public ICMITokenizer(Munger... mungers) {
        super(mungers);
    }

    public ICMITokenizer(){
        super();
    }

    public String replace(String input) {
        return super.replace(input.replace("_",""));
    }


}
