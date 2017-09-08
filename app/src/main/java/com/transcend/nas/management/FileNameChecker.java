package com.transcend.nas.management;

/**
 * Created by steve_su on 2017/1/18.
 */

public class FileNameChecker {
    private static final Character[] INVALID_CHARS = {'!', '*', '\'', ';', ':', '@', '&', '=', '+', '$', ',', '/', '?', '#',
            '<', '>', '%','|', '\\', '^', '`', '！', '＊', '；', '：', '＠', '＆', '＝', '＋', '＄', '，', '／', '？', '＃',
            '＜', '＞', '％','｜', '︿', '‘'};

    private String mInput;

    public FileNameChecker(String input) {
        mInput = input;
    }

    public boolean isContainInvalid() {
        for (int i = 0; i < INVALID_CHARS.length; i++) {
            if (mInput.contains(INVALID_CHARS[i].toString())) {
                return true;
            }
        }
        return false;
    }

    public boolean isStartWithSpace(){
        return mInput.startsWith(" ");
    }
}
