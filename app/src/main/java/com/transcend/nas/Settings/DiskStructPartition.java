package com.transcend.nas.settings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ikelee on 16/07/20.
 */
public class DiskStructPartition {
    public final static List<String> FORMAT = Arrays.asList("available", "fs", "blocks", "usedpercent", "end", "label",
            "start", "length", "used", "flags", "path", "mountpoint", "type", "uuid", "raidmember");
    public Map<String, String> infos = new HashMap<String, String>();
}
