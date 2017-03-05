package com.edillower.heymavic;

import java.util.Map;

/**
 * Created by Edillower on 3/5/17.
 */

public class RecogRecord {
    private String rId;
    private String rS2TStr;
    private String rEncodedStr;

    public RecogRecord (String inId, String inS2TStr, String inEncodedStr){
        rId=inId;
        rS2TStr=inS2TStr;
        rEncodedStr=inEncodedStr;
    }

    public void setId(String inId){
        rId = inId;
    }
    public void setS2TStr(String inS2TStr){
        rS2TStr = inS2TStr;
    }
    public void setEncodedStr(String inEncodedStr){
        rEncodedStr = inEncodedStr;
    }
    public Map<String, Object> toMap(){
        return null;
    }
}
